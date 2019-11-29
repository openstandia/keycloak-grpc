package jp.openstandia.keycloak.grpc.admin;

import jp.openstandia.keycloak.grpc.AbstractGrpcServiceProviderFactory;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;

public class UserResourceServiceFactory extends AbstractGrpcServiceProviderFactory {

    @Override
    public GrpcServiceProvider create() {
        return new UserResourceService();
    }

    @Override
    public String getId() {
        return "grpc-user-resource-service";
    }
}
