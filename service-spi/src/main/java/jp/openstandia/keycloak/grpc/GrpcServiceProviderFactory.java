package jp.openstandia.keycloak.grpc;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

public interface GrpcServiceProviderFactory extends ProviderFactory<GrpcServiceProvider> {

    @Override
    default GrpcServiceProvider create(KeycloakSession nullSession) {
        throw new UnsupportedOperationException("You should implement create() method");
    }

    GrpcServiceProvider create();

    @Override
    default void init(Config.Scope config) {

    }
}
