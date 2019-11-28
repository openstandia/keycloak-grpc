package jp.openstandia.keycloak.grpc;

import io.grpc.BindableService;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.provider.Provider;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.GrpcAdminRoot;

import javax.ws.rs.core.UriInfo;

public interface GrpcServiceProvider extends Provider, BindableService {

    default KeycloakSession getKeycloakSession() {
        KeycloakSession session = Constant.KeycloakSessionContextKey.get();
//        Resteasy.pushContext(KeycloakSession.class, session);
        return session;
    }

    default <T> T withTransaction(TransactionalTask<T> task) {
        KeycloakSession session = getKeycloakSession();
        KeycloakTransactionManager tx = session.getTransactionManager();

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
        }
    }

    default AdminAuth authenticate() {
        KeycloakTransactionManager tx = getKeycloakSession().getTransactionManager();
        if (!tx.isActive()) {
            tx.begin();
        }

//        String realmName = headers.get(Constant.RealmMetadataKey);
//        String authority = call.getAuthority();
        String url = "http://localhost:8080/auth/realms/master";// + realmName;

        ResteasyUriInfo resteasyUriInfo = new ResteasyUriInfo(url, "", "auth");
        Resteasy.pushContext(UriInfo.class, resteasyUriInfo);

        String authz = Constant.AuthorizationHeaderContextKey.get();
        GrpcAdminRoot adminRoot = new GrpcAdminRoot(getKeycloakSession());
        AdminAuth adminAuth = adminRoot.authenticateRealmAdminRequest(authz);

        Resteasy.clearContextData();

        return adminAuth;
    }

    default AdminAuth getAdminAuth() {
        return Constant.AdminAuthContextKey.get();
    }

    @Override
    default void close() {
    }
}
