package jp.openstandia.keycloak.grpc.admin;

import io.grpc.stub.StreamObserver;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.*;

import java.util.List;
import java.util.stream.Collectors;

public class UserResourceService extends UserResourceGrpc.UserResourceImplBase implements GrpcServiceProvider {

    @Override
    public void users(RealmRequest request, StreamObserver<UsersResponse> responseObserver) {
        KeycloakSession session = getKeycloakSession();
        session.getTransactionManager().begin();

        AdminAuth adminAuth = authenticate();

        String realmName = request.getRealm();

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        AdminPermissionEvaluator auth = AdminPermissions.evaluator(session, realm, adminAuth);
        UserPermissionEvaluator usersEvaluator = auth.users();

        List<UserModel> users = session.users().getUsers(realm, false);
        List<User> resUsers = users.stream().map(x -> {
            return User.newBuilder()
                    .setId(x.getId())
                    .setCreatedTimestamp(x.getCreatedTimestamp())
                    .setUsername(x.getUsername())
                    .setFirstName(get(x.getFirstName()))
                    .setLastName(get(x.getLastName()))
                    .setEnabled(x.isEnabled())
                    .setTotp(session.userCredentialManager().isConfiguredFor(realm, x, CredentialModel.OTP))
                    .setEmailVerified(x.isEmailVerified())
                    .addAllDisableableCredentialTypes(session.userCredentialManager().getDisableableCredentialTypes(realm, x))
                    .addAllRequiredActions(x.getRequiredActions())
                    .setNotBefore(session.users().getNotBeforeOfUser(realm, x))
                    .addAllAttributes(x.getAttributes().entrySet().stream().map(y -> {
                        return Attribute.newBuilder()
                                .setKey(y.getKey())
                                .addAllValue(y.getValue())
                                .build();
                    }).collect(Collectors.toList()))
                    .putAllAccess(usersEvaluator.getAccess(x))
                    .build();
        }).collect(Collectors.toList());

        if (session.getTransactionManager().isActive()) {
            session.getTransactionManager().commit();
        }

        UsersResponse res = UsersResponse.newBuilder().addAllUsers(resUsers).build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();

    }

    private String get(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }
}
