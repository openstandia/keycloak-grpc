package jp.openstandia.keycloak.grpc;

import io.grpc.BindableService;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.*;
import org.keycloak.provider.Provider;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.GrpcAdminRoot;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public interface GrpcServiceProvider extends Provider, BindableService {

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
        Resteasy.pushContext(KeycloakSession.class, session);
        Resteasy.pushContext(ClientConnection.class, session.getContext().getConnection());
        Resteasy.pushContext(KeycloakTransaction.class, tx);

        try {
            tx.begin();
            T result = task.run(session);
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

    default <T> T withRealm(String realmName, RealmTask<T> task) {
        RealmManager realmManager = new RealmManager(getKeycloakSession());
        RealmModel realm = realmManager.getRealmByName(realmName);
        return task.run(realm);
    }

    default <T> T withUser(RealmModel realm, String userId, UserTask<T> task) {
        UserModel user = getKeycloakSession().users().getUserById(userId, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return task.run(user);
    }

    default AdminAuth authenticate() {
        KeycloakSession session = getKeycloakSession();
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (!tx.isActive() || Resteasy.getContextData(UriInfo.class) == null) {
            throw new IllegalStateException("You must call authenticate() within 'withTransaction()'");
        }

        String token = Constant.AuthorizationHeaderContextKey.get();

        GrpcAdminRoot adminRoot = new GrpcAdminRoot(session);
        AdminAuth adminAuth = adminRoot.authenticateRealmAdminRequest(token);

        return adminAuth;
    }

    @Override
    default void close() {
    }

    public interface RealmTask<T> {
        T run(RealmModel realm);
    }

    public interface UserTask<T> {
        T run(UserModel user);
    }
}
