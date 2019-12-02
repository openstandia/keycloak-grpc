package jp.openstandia.keycloak.grpc;

import io.grpc.BindableService;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.provider.Provider;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.GrpcAdminRoot;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public interface GrpcServiceProvider extends Provider, BindableService {

    default <T> T nullable(T s) {
        if (s != null) {
            if (s instanceof String) {
                if (((String) s).isEmpty()) {
                    return null;
                }
            } else if (s instanceof Integer) {
                if ((Integer) s == 0) {
                    return null;
                }
            }
        }
        return s;
    }

    default KeycloakApplication getKeycloakApplication() {
        return Constant.KeycloakApplicationContextKey.get();
    }

    default KeycloakSession getKeycloakSession() {
        return Constant.KeycloakSessionContextKey.get();
    }

    default String getBaseUrl() {
        return Constant.BaseUrlContextKey.get();
    }

    default <T> T withTransaction(TransactionalTask<T> task) {
        KeycloakSession session = getKeycloakSession();
        KeycloakTransactionManager tx = session.getTransactionManager();

        // Need for validating JWT access token
        URI uri = URI.create(getBaseUrl());
        ResteasyUriInfo resteasyUriInfo = new ResteasyUriInfo(getBaseUrl(), "", uri.getPath());
        Resteasy.pushContext(UriInfo.class, resteasyUriInfo);

        // See KeycloakSessionServletFilter
        Resteasy.pushContext(KeycloakApplication.class, getKeycloakApplication());
        Resteasy.pushContext(KeycloakSession.class, session);
        Resteasy.pushContext(ClientConnection.class, session.getContext().getConnection());
        Resteasy.pushContext(KeycloakTransaction.class, tx);

        try {
            tx.begin();
            T result = task.run(new TransactionalTaskContext(getBaseUrl(), session));
            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            Resteasy.clearContextData();
        }
    }

    default <T> T runAdminTask(AdminTask<T> task) {
        return withTransaction(ctx -> {
            String token = Constant.AuthorizationHeaderContextKey.get();

            GrpcAdminRoot adminRoot = new GrpcAdminRoot(ctx.session, HttpMethod.GET, getBaseUrl() + "/admin");
            AdminAuth adminAuth = adminRoot.authenticateRealmAdminRequest(token);

            return task.run(new AdminTaskContext(ctx, adminRoot, adminAuth));
        });
    }

    default AdminAuth authenticate() {
        KeycloakSession session = getKeycloakSession();
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (!tx.isActive() || Resteasy.getContextData(UriInfo.class) == null) {
            throw new IllegalStateException("You must call this method within 'withTransaction()'");
        }

        String token = Constant.AuthorizationHeaderContextKey.get();

        GrpcAdminRoot adminRoot = new GrpcAdminRoot(session);
        AdminAuth adminAuth = adminRoot.authenticateRealmAdminRequest(token);

        return adminAuth;
    }

    default String getToken() {
        return Constant.AuthorizationHeaderContextKey.get();
    }

    @Override
    default void close() {
    }
}
