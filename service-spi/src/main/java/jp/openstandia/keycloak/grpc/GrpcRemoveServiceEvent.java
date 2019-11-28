package jp.openstandia.keycloak.grpc;

import org.keycloak.provider.ProviderEvent;

public class GrpcRemoveServiceEvent implements ProviderEvent {
    private final String id;

    public GrpcRemoveServiceEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
