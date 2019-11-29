package jp.openstandia.keycloak.grpc.admin;

import jp.openstandia.keycloak.grpc.AbstractGrpcServiceProviderFactory;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;

public class UsersResourceServiceFactory extends AbstractGrpcServiceProviderFactory {

    @Override
    public GrpcServiceProvider create() {
        return new UsersResourceService();
    }

    @Override
    public String getId() {
        return "grpc-users-resource-service";
    }
}
