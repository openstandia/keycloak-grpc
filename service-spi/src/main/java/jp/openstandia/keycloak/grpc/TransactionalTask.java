package jp.openstandia.keycloak.grpc;

import org.keycloak.models.KeycloakSession;

public interface TransactionalTask<T> {
    T run(KeycloakSession session);
}