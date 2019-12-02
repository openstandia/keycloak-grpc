package jp.openstandia.keycloak.grpc.admin;

import jp.openstandia.keycloak.grpc.AbstractGrpcServiceProviderFactory;
import jp.openstandia.keycloak.grpc.GrpcServiceProvider;

public class RealmAdminResourceServiceFactory extends AbstractGrpcServiceProviderFactory {

    @Override
    public GrpcServiceProvider create() {
        return new RealmAdminResourceService();
    }

    @Override
    public String getId() {
        return "grpc-realm-admin-resource-service";
    }
}
