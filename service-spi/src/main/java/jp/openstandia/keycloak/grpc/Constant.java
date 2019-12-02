package jp.openstandia.keycloak.grpc;

import io.grpc.Context;
import io.grpc.Metadata;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.KeycloakApplication;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class Constant {
    public static final Metadata.Key<String> AuthorizationMetadataKey =
            Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);

    public static final Context.Key<KeycloakApplication> KeycloakApplicationContextKey = Context.key("keycloakApplication");
    public static final Context.Key<KeycloakSession> KeycloakSessionContextKey = Context.key("keycloakSession");
    public static final Context.Key<String> BaseUrlContextKey = Context.key("baseUrl");
    public static final Context.Key<String> AuthorizationHeaderContextKey = Context.key("authorization");
}
