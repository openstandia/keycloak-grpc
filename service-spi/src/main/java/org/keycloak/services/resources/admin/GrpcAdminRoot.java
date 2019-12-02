package org.keycloak.services.resources.admin;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URISyntaxException;

public class GrpcAdminRoot extends AdminRoot {

    public GrpcAdminRoot(KeycloakSession session, String httpMethod, String url) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        try {
            this.request = MockHttpRequest.create(httpMethod, url);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public GrpcAdminRoot(KeycloakSession session) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
    }

    public AdminAuth authenticateRealmAdminRequest(String authorizationHeaderValue) {
        MultivaluedMap<String, String> map = new Headers<>();
        map.putSingle("Authorization", authorizationHeaderValue);
        HttpHeaders headers = new ResteasyHttpHeaders(map);
        return super.authenticateRealmAdminRequest(headers);
    }
}
