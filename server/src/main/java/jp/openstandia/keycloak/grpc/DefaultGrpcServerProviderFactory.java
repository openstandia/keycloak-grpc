package jp.openstandia.keycloak.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ProviderFactory;

import javax.servlet.ServletContext;
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
    protected GrpcServerEventListener listener;

    private final Object lock = new Object();

    @Override
    public GrpcServerProvider create(KeycloakSession nullSession) {
        return null;
    }

    @Override
    public void init(Config.Scope scope) {
        this.scope = scope;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        sessionFactory = factory;

        listener = new GrpcServerEventListener();
        sessionFactory.register(listener);

        // Need to start server now because PostMigrationEvent is finished when hot deploy mode
        if (isHotDeploy()) {
            synchronized (lock) {
                startServer();
            }
        }
    }

    protected void stopServer() {
        if (server != null && !server.isShutdown()) {
            server.shutdownNow();
            logger.infov("Stopped gRPC server");
        }
    }

    protected void startServer() {
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
        sessionFactory.unregister(listener);

        synchronized (lock) {
            server.shutdownNow();
            server = null;
            sessionFactory = null;
            scope = null;
        }

        logger.infov("Stopped gRPC server");
    }

    @Override
    public String getId() {
        return "default";
    }

    public boolean isHotDeploy() {
        ServletContext context = Resteasy.getContextData(ServletContext.class);
        return context == null;
    }

    private class GrpcServerEventListener implements ProviderEventListener {
        @Override
        public void onEvent(ProviderEvent event) {
            // Called when keycloak-grpc-server.war is deployed when booting keycloak.
            // If the war is deployed after booting (Hot deployed), we cant't trap this event.
            if (event instanceof PostMigrationEvent) {
                synchronized (lock) {
                    startServer();
                }
                return;
            }

            if (event instanceof GrpcAddServiceEvent) {
                GrpcAddServiceEvent addEvent = (GrpcAddServiceEvent) event;
                if (addEvent.isHotDeploy()) {
                    synchronized (lock) {
                        logger.infov("Restarting gRPC server");
                        stopServer();
                        startServer();
                    }
                }
                return;
            }

            if (event instanceof GrpcRemoveServiceEvent) {
                synchronized (lock) {
                    logger.infov("Restarting gRPC server");
                    stopServer();
                    startServer();
                }
                return;
            }
        }
    }
}
