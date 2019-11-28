package jp.openstandia.keycloak.grpc.admin;

import jp.openstandia.keycloak.grpc.GrpcServiceProvider;
import jp.openstandia.keycloak.grpc.GrpcServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

public class UserResourceServiceFactory implements GrpcServiceProviderFactory {

    static {
        System.out.println("################ UserResourceServiceFactory loaded");
    }

    @Override
    public GrpcServiceProvider create() {
        return new UserResourceService();
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "grpc-user-resource-service";
    }
}
