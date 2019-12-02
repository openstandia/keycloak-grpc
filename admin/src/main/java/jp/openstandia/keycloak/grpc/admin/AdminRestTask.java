package jp.openstandia.keycloak.grpc.admin;

public interface AdminRestTask<T> {
    T run(AdminRestTaskContext ctx);
}
