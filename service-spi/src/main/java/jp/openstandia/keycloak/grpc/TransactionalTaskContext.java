package jp.openstandia.keycloak.grpc;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;

public class TransactionalTaskContext {

    public final String baseUrl;
    public final KeycloakSession session;
    public final ClientConnection clientConnection;

    public TransactionalTaskContext(String baseUrl, KeycloakSession session) {
        this.baseUrl = baseUrl;
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
    }
}
