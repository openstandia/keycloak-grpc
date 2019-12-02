package jp.openstandia.keycloak.grpc;

import io.grpc.BindableService;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.*;
import org.keycloak.provider.Provider;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.admin.*;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
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

    default HttpHeaders getHeaders() {
        String token = Constant.AuthorizationHeaderContextKey.get();

        MultivaluedMap<String, String> map = new Headers<>();
        map.putSingle("Authorization", token);
        HttpHeaders headers = new ResteasyHttpHeaders(map);
        return headers;
    }

    default RealmModel getRealm(String realmName) {
        KeycloakSession session = getKeycloakSession();
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (!tx.isActive() || Resteasy.getContextData(UriInfo.class) == null) {
            throw new IllegalStateException("You must call getRealm() within 'withTransaction()'");
        }

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm does not exist");
        }
        return realm;
    }

    default RealmsAdminResource getRealmsAdmin(String httpMethod, String realm, String pathTemplate, String ...params) {
        KeycloakSession session = getKeycloakSession();
        KeycloakTransactionManager tx = session.getTransactionManager();

        String baseUrl = getBaseUrl();

        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        sb.append("/");
        sb.append(String.format(pathTemplate, params));
        String url = sb.toString();

        if (!tx.isActive() || Resteasy.getContextData(UriInfo.class) == null) {
            throw new IllegalStateException("You must call getRealmsAdmin() within 'withTransaction()'");
        }

        URI uri = URI.create(url);
        URI baseUri = URI.create(baseUrl);

        ResteasyUriInfo resteasyUriInfo = new ResteasyUriInfo(url, uri.getRawQuery(), baseUri.getPath());
        Resteasy.pushContext(UriInfo.class, resteasyUriInfo);

        GrpcAdminRoot adminRoot = new GrpcAdminRoot(session, httpMethod, url);
        return (RealmsAdminResource) adminRoot.getRealmsAdmin(getHeaders());
    }

    default RealmAdminResource getRealmAdmin(String httpMethod, String realm, String pathTemplate, String ...params) {
        RealmsAdminResource resource = getRealmsAdmin(httpMethod, realm, pathTemplate, params);
        return  resource.getRealmAdmin(getHeaders(), realm);
    }

    default UsersResource getUsers(String httpMethod, String realm, String pathTemplate, String ...params) {
        RealmAdminResource resource = getRealmAdmin(httpMethod, realm, pathTemplate, params);
        return  resource.users();
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
