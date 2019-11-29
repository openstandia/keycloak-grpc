package jp.openstandia.keycloak.grpc.admin;

import io.grpc.stub.StreamObserver;
import jp.openstandia.keycloak.grpc.BuilderWrapper;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import java.util.List;
import java.util.stream.Collectors;

public class UsersResourceService extends UsersResourceGrpc.UsersResourceImplBase implements GrpcServiceProvider {

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
        List<User> results = withTransaction(session -> {

            AdminAuth adminAuth = authenticate();

            String realmName = request.getRealm();

            RealmManager realmManager = new RealmManager(session);
            RealmModel realm = realmManager.getRealmByName(realmName);

            AdminPermissionEvaluator auth = AdminPermissions.evaluator(session, realm, adminAuth);
            UserPermissionEvaluator usersEvaluator = auth.users();

            List<UserModel> users = session.users().getUsers(realm, false);
            List<User> resUsers = users.stream().map(x -> {
                return BuilderWrapper.wrap(User.newBuilder())
                        .nullSafe(x.getId(), (b, v) -> b.setId(v))
                        .nullSafe(x.getCreatedTimestamp(), (b, v) -> b.setCreatedTimestamp(v))
                        .nullSafe(x.getUsername(), (b, v) -> b.setUsername(v))
                        .nullSafe(x.getFirstName(), (b, v) -> b.setFirstName(v))
                        .nullSafe(x.getLastName(), (b, v) -> b.setLastName(v))
                        .nullSafe(x.isEnabled(), (b, v) -> b.setEnabled(v))
                        .nullSafe(session.userCredentialManager().isConfiguredFor(realm, x, CredentialModel.OTP), (b, v) -> b.setTotp(v))
                        .nullSafe(x.isEmailVerified(), (b, v) -> b.setEmailVerified(v))
                        .nullSafe(session.userCredentialManager().getDisableableCredentialTypes(realm, x), (b, v) -> b.addAllDisableableCredentialTypes(v))
                        .nullSafe(x.getRequiredActions(), (b, v) -> b.addAllRequiredActions(v))
                        .nullSafe(session.users().getNotBeforeOfUser(realm, x), (b, v) -> b.setNotBefore(v))
                        .nullSafe(
                                x.getAttributes().entrySet().stream().map(y -> {
                                    return Attribute.newBuilder()
                                            .setKey(y.getKey())
                                            .addAllValue(y.getValue())
                                            .build();
                                }).collect(Collectors.toList()),
                                (b, v) -> b.addAllAttributes(v)
                        )
                        .nullSafe(usersEvaluator.getAccess(x), (b, v) -> b.putAllAccess(v))
                        .unwrap()
                        .build();
            }).collect(Collectors.toList());

            return resUsers;
        });

        GetUsersResponse res = GetUsersResponse.newBuilder().addAllUsers(results).build();
        responseObserver.onNext(res);
        responseObserver.onCompleted();
    }
}
