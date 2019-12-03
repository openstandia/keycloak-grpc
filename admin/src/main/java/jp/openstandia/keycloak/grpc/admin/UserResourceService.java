package jp.openstandia.keycloak.grpc.admin;

import io.grpc.stub.StreamObserver;
import jp.openstandia.keycloak.grpc.ErrorHandler;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

public class UserResourceService extends UserResourceGrpc.UserResourceImplBase implements GrpcAdminRESTServiceProvider {

    private static final Logger logger = Logger.getLogger(UserResourceService.class);

    @Override
    public void executeActionsEmail(ExecuteActionsEmailRequest request, StreamObserver<ExecuteActionsEmailResponse> responseObserver) {
        Response response = runAdminRestTask(ctx -> {
            UsersResource resource = ctx.getUsersResource(HttpMethod.PUT, request.getRealm(), "users/%s/execute-actions-email", request.getUserId());
            UserResource user = resource.user(request.getUserId());
            return user.executeActionsEmail(nullable(request.getRedirectUri()),
                    nullable(request.getClientId()),
                    nullable(request.getLifespan()),
                    request.getRequiredActionsList());
        });

        if (ErrorHandler.hasError(response)) {
            throw ErrorHandler.convert(response);
        }

        ExecuteActionsEmailResponse res = ExecuteActionsEmailResponse.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void executeActionsEmailByUsername(ExecuteActionsEmailByUsernameRequest request, StreamObserver<ExecuteActionsEmailResponse> responseObserver) {
        Response response = runAdminRestTask(ctx -> {
            RealmModel realm = ctx.getRealm(request.getRealm());
            UserModel userModel = getKeycloakSession().users().getUserByUsername(request.getUsername(), realm);
            if (userModel == null) {
                throw new NotFoundException("User does not exist");
            }

            UsersResource resource = ctx.getUsersResource(HttpMethod.PUT, request.getRealm(), "users/%s/execute-actions-email", userModel.getId());
            UserResource user = resource.user(userModel.getId());
            return user.executeActionsEmail(nullable(request.getRedirectUri()),
                    nullable(request.getClientId()),
                    nullable(request.getLifespan()),
                    request.getRequiredActionsList());
        });

        if (ErrorHandler.hasError(response)) {
            throw ErrorHandler.convert(response);
        }

        ExecuteActionsEmailResponse res = ExecuteActionsEmailResponse.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        runAdminRestTask(ctx -> {
            UsersResource resource = ctx.getUsersResource(HttpMethod.PUT, request.getRealm(), "users/%s/logout", request.getUserId());
            UserResource user = resource.user(request.getUserId());
            user.logout();
            return null;
        });

        LogoutResponse res = LogoutResponse.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    @Override
    public void logoutByUsername(LogoutByUsernameRequest request, StreamObserver<LogoutResponse> responseObserver) {
        runAdminRestTask(ctx -> {
            KeycloakSession session = ctx.session;

            RealmModel realm = ctx.getRealm(request.getRealm());
            UserModel userModel = getKeycloakSession().users().getUserByUsername(request.getUsername(), realm);
            if (userModel == null) {
                throw new NotFoundException("User does not exist");
            }

            ctx.attachAdminRestUri(request.getRealm(), "users/%s/logout", userModel.getId());

            AdminPermissionEvaluator auth = ctx.getAdminPermission(realm);
            AdminEventBuilder adminEvent = ctx.getAdminEventBuilder(realm)
                    .realm(realm)
                    .resource(ResourceType.USER);

            auth.users().requireManage(userModel);

            session.users().setNotBeforeForUser(realm, userModel, Time.currentTime());

            List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, userModel);
            for (UserSessionModel userSession : userSessions) {
                if (request.getRemoveCurrent() || !isCurrentSession(userSession, request.getCurrentSessionId())) {
                    AuthenticationManager.backchannelLogout(session, userSession, true);
                }
            }
            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();

            return null;
        });

        LogoutResponse res = LogoutResponse.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }

    private boolean isCurrentSession(UserSessionModel session, String current) {
        return session.getId().equals(current);
    }
}
