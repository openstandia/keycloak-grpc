package jp.openstandia.keycloak.grpc.admin;

import jp.openstandia.keycloak.grpc.AdminTaskContext;
import jp.openstandia.keycloak.grpc.TransactionalTaskContext;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.keycloak.common.util.Resteasy;
import org.keycloak.services.resources.admin.*;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class AdminRestTaskContext extends AdminTaskContext {

    public AdminRestTaskContext(TransactionalTaskContext ctx, GrpcAdminRoot adminRoot, AdminAuth adminAuth) {
        super(ctx, adminRoot, adminAuth);
    }

    public String attachAdminRestUri(String realm, String pathTemplate, String ...params) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        sb.append("/admin/realms/");
        sb.append(realm);
        sb.append("/");
        sb.append(String.format(pathTemplate, params));
        String url = sb.toString();

        attachUri(realm, url);

        return url;
    }

    public RealmsAdminResource getRealmsAdminResource(String httpMethod, String realm, String pathTemplate, String ...params) {
        String url = attachAdminRestUri(realm, pathTemplate, params);

        GrpcAdminRoot adminRoot = new GrpcAdminRoot(session, httpMethod, url);
        RealmsAdminResource resource = (RealmsAdminResource) adminRoot.getRealmsAdmin(getHeaders());

        return resource;
    }

    public RealmAdminResource getRealmAdminResource(String httpMethod, String realm, String pathTemplate, String ...params) {
        RealmsAdminResource resource = getRealmsAdminResource(httpMethod, realm, pathTemplate, params);
        RealmAdminResource realmAdminResource = resource.getRealmAdmin(getHeaders(), realm);

        return realmAdminResource;
    }

    public UsersResource getUsersResource(String httpMethod, String realm, String pathTemplate, String ...params) {
        RealmAdminResource resource = getRealmAdminResource(httpMethod, realm, pathTemplate, params);
        UsersResource usersResource = resource.users();

        return usersResource;
    }
}
