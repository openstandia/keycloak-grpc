package jp.openstandia.keycloak.grpc;

public interface AdminTask<T> {
    T run(AdminTaskContext ctx);
}