package jp.openstandia.keycloak.grpc;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class GrpcServerSPI implements Spi {

    static {
        System.out.println("################ GrpcServerSPI loaded");
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "grpc-server";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return GrpcServerProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return GrpcServerProviderFactory.class;
    }
}