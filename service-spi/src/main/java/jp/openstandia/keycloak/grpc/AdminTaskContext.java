package jp.openstandia.keycloak.grpc;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.*;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class AdminTaskContext extends TransactionalTaskContext {
    public final GrpcAdminRoot adminRoot;
    public final AdminAuth adminAuth;

    public AdminTaskContext(TransactionalTaskContext ctx, GrpcAdminRoot adminRoot, AdminAuth adminAuth) {
        super(ctx.baseUrl, ctx.session);
        this.adminRoot = adminRoot;
        this.adminAuth = adminAuth;
    }

    public HttpHeaders getHeaders() {
        String token = Constant.AuthorizationHeaderContextKey.get();

        MultivaluedMap<String, String> map = new Headers<>();
        map.putSingle("Authorization", token);
        HttpHeaders headers = new ResteasyHttpHeaders(map);
        return headers;
    }

    public AdminEventBuilder getAdminEventBuilder(RealmModel realm) {
        return new AdminEventBuilder(realm, adminAuth, session, clientConnection);
    }

    public AdminPermissionEvaluator getAdminPermission(RealmModel realm) {
        return AdminPermissions.evaluator(session, realm, adminAuth);
    }

    public RealmModel getRealm(String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm does not exist");
        }
        return realm;
    }

    public RealmsAdminResource getRealmsAdmin(String httpMethod, String realm, String pathTemplate, String ...params) {
        KeycloakTransactionManager tx = session.getTransactionManager();

        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        sb.append("/admin/realms/");
        sb.append(realm);
        sb.append("/");
        sb.append(String.format(pathTemplate, params));
        String url = sb.toString();

        if (!tx.isActive() || Resteasy.getContextData(UriInfo.class) == null) {
            throw new IllegalStateException("You must call this method within 'withTransaction()'");
        }

        URI uri = URI.create(url);
        URI baseUri = URI.create(baseUrl);

        ResteasyUriInfo resteasyUriInfo = new ResteasyUriInfo(url, uri.getRawQuery(), baseUri.getPath());
        Resteasy.pushContext(UriInfo.class, resteasyUriInfo);

        GrpcAdminRoot adminRoot = new GrpcAdminRoot(session, httpMethod, url);
        return (RealmsAdminResource) adminRoot.getRealmsAdmin(getHeaders());
    }

    public RealmAdminResource getRealmAdmin(String httpMethod, String realm, String pathTemplate, String ...params) {
        RealmsAdminResource resource = getRealmsAdmin(httpMethod, realm, pathTemplate, params);
        return  resource.getRealmAdmin(getHeaders(), realm);
    }

    public UsersResource getUsers(String httpMethod, String realm, String pathTemplate, String ...params) {
        RealmAdminResource resource = getRealmAdmin(httpMethod, realm, pathTemplate, params);
        return  resource.users();
    }
}
