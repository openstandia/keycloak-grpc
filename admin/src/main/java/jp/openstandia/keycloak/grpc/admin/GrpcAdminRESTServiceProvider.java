package jp.openstandia.keycloak.grpc.admin;

import jp.openstandia.keycloak.grpc.GrpcServiceProvider;

public interface GrpcAdminRESTServiceProvider extends GrpcServiceProvider {

    default <T> T runAdminRestTask(AdminRestTask<T> task) {
        return runAdminTask(ctx -> {
            return task.run(new AdminRestTaskContext(ctx, ctx.adminRoot, ctx.adminAuth));
        });
    }
}
