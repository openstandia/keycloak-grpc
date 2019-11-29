package jp.openstandia.keycloak.grpc;

import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSessionFactory;

import javax.servlet.ServletContext;

public abstract class AbstractGrpcServiceProviderFactory implements GrpcServiceProviderFactory {
    protected KeycloakSessionFactory sessionFactory;
    protected String baseUrl;

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        sessionFactory = factory;
        factory.publish(new GrpcAddServiceEvent(getId(), isHotDeploy()));
    }

    @Override
    public void close() {
        sessionFactory.publish(new GrpcRemoveServiceEvent(getId()));
        baseUrl = null;
    }

    public boolean isHotDeploy() {
        ServletContext context = Resteasy.getContextData(ServletContext.class);
        return context == null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
