package jp.openstandia.keycloak.grpc.admin;

import io.grpc.stub.StreamObserver;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;
import org.jboss.logging.Logger;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.services.resources.admin.RealmAdminResource;

import javax.ws.rs.HttpMethod;

public class RealmAdminResourceService extends RealmAdminResourceGrpc.RealmAdminResourceImplBase implements GrpcServiceProvider {

    private static final Logger logger = Logger.getLogger(RealmAdminResourceService.class);

    @Override
    public void logoutAll(LogoutAllRequest request, StreamObserver<LogoutAllResponse> responseObserver) {
        GlobalRequestResult response = runAdminTask(ctx -> {
            RealmAdminResource resource = ctx.getRealmAdmin(HttpMethod.POST, request.getRealm(), "logout-all");
            return resource.logoutAll();
        });

        // TODO return result

        LogoutAllResponse res = LogoutAllResponse.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }
}
