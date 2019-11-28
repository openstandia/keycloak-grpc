package jp.openstandia.keycloak.grpc;

import org.keycloak.provider.ProviderEvent;

public class GrpcAddServiceEvent implements ProviderEvent {
    private final String id;
    private final boolean hotDeploy;

    public GrpcAddServiceEvent(String id, boolean hotDeploy) {
        this.id = id;
        this.hotDeploy = hotDeploy;
    }

    public String getId() {
        return id;
    }

    public boolean isHotDeploy() {
        return hotDeploy;
    }
}
