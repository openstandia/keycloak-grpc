package jp.openstandia.keycloak.grpc;

public interface TransactionalTask<T> {
    T run(TransactionalTaskContext task);
}