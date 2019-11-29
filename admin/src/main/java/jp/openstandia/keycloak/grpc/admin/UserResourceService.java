package jp.openstandia.keycloak.grpc.admin;

import com.google.protobuf.Descriptors;
import com.google.protobuf.ProtocolStringList;
import io.grpc.stub.StreamObserver;
import jp.openstandia.keycloak.grpc.BuilderWrapper;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;
import org.jboss.logging.Logger;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.*;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UserResourceService extends UserResourceGrpc.UserResourceImplBase implements GrpcServiceProvider {

    private static final Logger logger = Logger.getLogger(UserResourceService.class);

    @Override
    public void executeActionsEmail(ExecuteActionsEmailRequest request, StreamObserver<ExecuteActionsEmailResponse> responseObserver) {
        Response response = withTransaction(session -> {
            RealmsAdminResource resource = getRealmsAdmin(HttpMethod.PUT, getBaseUrl() + "/" + request.getRealm() + "/users/" + request.getUserId() + "/execute-actions-email");
            RealmAdminResource realmResource = resource.getRealmAdmin(getHeaders(), request.getRealm());
            UsersResource usersResource = realmResource.users();
            UserResource user = usersResource.user(request.getUserId());
            return user.executeActionsEmail(nullable(request.getRedirectUri()),
                    nullable(request.getClientId()),
                    nullable(request.getLifespan()),
                    request.getRequiredActionsList());
        });

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            ExecuteActionsEmailResponse res = ExecuteActionsEmailResponse.newBuilder().build();
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } else {
            // TODO return error
        }
    }
}
