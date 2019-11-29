package jp.openstandia.keycloak.grpc;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class GrpcServiceSPI implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "grpc-service";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return GrpcServiceProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return GrpcServiceProviderFactory.class;
    }
}