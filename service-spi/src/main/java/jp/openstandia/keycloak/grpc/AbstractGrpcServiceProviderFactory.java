package jp.openstandia.keycloak.grpc;

import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.KeycloakTransactionManager;

import javax.servlet.ServletContext;

public abstract class AbstractGrpcServiceProviderFactory implements GrpcServiceProviderFactory {
    protected KeycloakSessionFactory sessionFactory;

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        sessionFactory = factory;
        factory.publish(new GrpcAddServiceEvent(getId(), isHotDeploy()));
    }

    @Override
    public void close() {
        sessionFactory.publish(new GrpcRemoveServiceEvent(getId()));
    }

    public boolean isHotDeploy() {
        ServletContext context = Resteasy.getContextData(ServletContext.class);
        return context == null;
    }
}
