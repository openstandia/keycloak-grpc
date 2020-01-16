package jp.openstandia.keycloak.grpc;

import io.grpc.Metadata;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class Constant {
    public static final Metadata.Key<String> AuthorizationMetadataKey =
            Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
}
