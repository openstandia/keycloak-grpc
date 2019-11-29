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
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UserResourceService extends UserResourceGrpc.UserResourceImplBase implements GrpcServiceProvider {

    private static final Logger logger = Logger.getLogger(UserResourceService.class);

    private static <T> T nullable(T s) {
        if (s != null) {
            if (s instanceof String) {
                if (((String)s).isEmpty()) {
                    return null;
                }
            } else if (s instanceof Integer) {
                if ((Integer)s == 0) {
                    return null;
                }
            }
        }
        return s;
    }

    @Override
    public void executeActionsEmail(ExecuteActionsEmailRequest request, StreamObserver<ExecuteActionsEmailResponse> responseObserver) {
        withTransaction(session -> {
            AdminAuth adminAuth = authenticate();

            return withRealm(request.getRealm(), realm -> {
                AdminPermissionEvaluator auth = AdminPermissions.evaluator(session, realm, adminAuth);

                return withUser(realm, request.getUserId(), user -> {
                    String redirectUri = nullable(request.getRedirectUri());
                    String clientId = nullable(request.getClientId());
                    Integer lifespan = nullable(request.getLifespan());
                    List<String> requiredActions = request.getRequiredActionsList();

                    auth.users().requireManage(user);

                    if (user.getEmail() == null) {
                        return ErrorResponse.error("User email missing", Response.Status.BAD_REQUEST);
                    }

                    if (!user.isEnabled()) {
                        throw new WebApplicationException(
                                ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST));
                    }

                    if (redirectUri != null && clientId == null) {
                        throw new WebApplicationException(
                                ErrorResponse.error("Client id missing", Response.Status.BAD_REQUEST));
                    }

                    if (clientId == null) {
                        clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
                    }

                    ClientModel client = realm.getClientByClientId(clientId);
                    if (client == null) {
                        logger.debugf("Client %s doesn't exist", clientId);
                        throw new WebApplicationException(
                                ErrorResponse.error("Client doesn't exist", Response.Status.BAD_REQUEST));
                    }
                    if (!client.isEnabled()) {
                        logger.debugf("Client %s is not enabled", clientId);
                        throw new WebApplicationException(
                                ErrorResponse.error("Client is not enabled", Response.Status.BAD_REQUEST));
                    }

                    String redirect;
                    if (redirectUri != null) {
                        redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
                        if (redirect == null) {
                            throw new WebApplicationException(
                                    ErrorResponse.error("Invalid redirect uri.", Response.Status.BAD_REQUEST));
                        }
                    }

                    if (lifespan == null) {
                        lifespan = realm.getActionTokenGeneratedByAdminLifespan();
                    }
                    int expiration = Time.currentTime() + lifespan;
                    ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), expiration, requiredActions, redirectUri, clientId);

                    try {
                        UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
                        builder.queryParam("key", token.serialize(session, realm, session.getContext().getUri()));

                        String link = builder.build(realm.getName()).toString();

                        session.getProvider(EmailTemplateProvider.class)
                                .setAttribute(Constants.TEMPLATE_ATTR_REQUIRED_ACTIONS, token.getRequiredActions())
                                .setRealm(realm)
                                .setUser(user)
                                .sendExecuteActions(link, TimeUnit.SECONDS.toMinutes(lifespan));

                        //audit.user(user).detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, accessCode.getCodeId()).success();

//                        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).success();

                        return Response.ok().build();
                    } catch (EmailException e) {
                        ServicesLogger.LOGGER.failedToSendActionsEmail(e);
                        return ErrorResponse.error("Failed to send execute actions email", Response.Status.INTERNAL_SERVER_ERROR);
                    }
                });
            });
        });

        ExecuteActionsEmailResponse res = ExecuteActionsEmailResponse.newBuilder().build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }
}
