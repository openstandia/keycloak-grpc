package jp.openstandia.keycloak.grpc;

import io.grpc.Context;
import io.grpc.Metadata;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminAuth;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class Constant {
    public static final Metadata.Key<String> AuthorizationMetadataKey =
            Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> RealmMetadataKey =
            Metadata.Key.of("Realm", ASCII_STRING_MARSHALLER);

    public static final Context.Key<KeycloakSession> KeycloakSessionContextKey = Context.key("keycloakSession");
    public static final Context.Key<String> AuthorizationHeaderContextKey = Context.key("authorization");
    public static final Context.Key<AdminAuth> AdminAuthContextKey = Context.key("adminAuth");
}
