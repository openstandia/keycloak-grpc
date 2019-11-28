package jp.openstandia.keycloak.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

import java.io.IOException;
import java.util.List;

public class DefaultGrpcServerProviderFactory implements GrpcServerProviderFactory {

    private static final Logger logger = Logger.getLogger(DefaultGrpcServerProviderFactory.class);

    static {
        System.out.println("################ DefaultGrpcServerProviderFactory loaded");
    }

    protected KeycloakSessionFactory sessionFactory;
    protected Config.Scope scope;
    protected Server server;

    @Override
    public GrpcServerProvider create(KeycloakSession nullSession) {
        return null;
    }

    @Override
    public void init(Config.Scope scope) {
        this.scope = scope;
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        sessionFactory = keycloakSessionFactory;

        int port = scope.getInt("port", 6565);

        ServerBuilder<?> builder = ServerBuilder.forPort(port)
                .intercept(TransmitStatusRuntimeExceptionInterceptor.instance())
                .intercept(KeycloakSessionInterceptor.instance(sessionFactory));

        List<ProviderFactory> factories = sessionFactory.getProviderFactories(GrpcServiceProvider.class);
        for (ProviderFactory factory : factories) {
            logger.infov("Adding gRPC service: {0}", factory.getId());
            GrpcServiceProvider grpcResourceProvider = ((GrpcServiceProviderFactory) factory).create();
            builder.addService(grpcResourceProvider);
        }

        server = builder.build();

        try {
            logger.infov("Starting gRPC server with port={0}", port);
            server.start();
        } catch (IOException e) {
            logger.error("Failed to start gRPC server", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        logger.infov("Stopping gRPC server");
        server.shutdown();
        server = null;
        sessionFactory = null;
        scope = null;
    }

    @Override
    public String getId() {
        return "default";
    }
}
