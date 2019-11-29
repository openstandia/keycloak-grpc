# keycloak-grpc
**This repository is heavily under development.**

[Keycloak](https://github.com/keycloak/keycloak) extension that enables serving gRPC services on Keycloak server.

## Features

* gRPC server on Keycloak
* Provide SPI for gRPC services
* gRPC services deployer (with hot deployment for easy development)
* Sample implementation of Admin gRPC Service for keycloak

## Install

### Build

Install JDK 8+ and [maven3](https://maven.apache.org/download.cgi) then build:

```
mvn install
```

After successful the build, you can find `keycloak-grpc-server.war` in `./server/target` directory.
Also, you can see `keycloak-grpc-admin-services.jar` in `./admin/target` directory which is sample implementation of admin gRPC service.

### Setting

#### SPI loader setting

Since **keycloak-grpc** defines own custom SPIs for gRPC server and services,
you need to add a bit of configuration into your `$KEYCLOAK_HOME/standalone/configuration/standalone.xml` or `standalone-ha.xml` to load the SPIs.

```
        <subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
            <web-context>auth</web-context>
            <providers>
                <provider>
                    classpath:${jboss.home.dir}/providers/*
                </provider>
                <!-- Add the following config -->
                <provider>
                    module:deployment.keycloak-grpc-server.war
                </provider>
            </providers>
```

#### gRPC server setting

The gRPC server has some options that you can configure in your `$KEYCLOAK_HOME/standalone/configuration/standalone.xml` or `standalone-ha.xml`.

* `port`: Port number of the gRPC server. (Default: 6565)
* `baseUrl`: Base URL of keycloak server which is used as issuer. You need to configure this option when using authorization by access token in the gRPC services.

```
            <spi name="hostname">
                <default-provider>default</default-provider>
                <provider name="default" enabled="true">
                    <properties>
                        <property name="frontendUrl" value="${keycloak.frontendUrl:}"/>
                        <property name="forceBackendUrlToFrontendUrl" value="false"/>
                    </properties>
                </provider>
            </spi>
            <!-- Add the following config -->
            <spi name="grpc-server">
                <provider name="default" enabled="true">
                    <properties>
                        <property name="port" value="9999"/>
                        <property name="baseUrl" value="https://keycloak.example.com/auth"/>
                    </properties>
                </provider>
            </spi>
        </subsystem>
```

### Deploy gRPC server and sample gRPC admin service

Put `keycloak-grpc-server.war` into `$KEYCLOAK_HOME/standalone/deployments` directory.
Also, put `keycloak-grpc-admin-services.jar` into `$KEYCLOAK_HOME/standalone/deployments` directory simply
if you want to deploy the sample gRPC admin service.

### Start gRPC server and services

Start your keycloak server. You can see some logging about starting gRPC server and services:

```
13:51:44,495 INFO  [jp.openstandia.keycloak.grpc.DefaultGrpcServerProviderFactory] (ServerService Thread Pool -- 63) Adding gRPC service: grpc-user-resource-service
13:51:44,584 INFO  [jp.openstandia.keycloak.grpc.DefaultGrpcServerProviderFactory] (ServerService Thread Pool -- 63) Starting gRPC server with port=6,565
```

## How to write own custom gRPC service

You need to extend gRPC service SPI which is defined in [this sub-project](https://github.com/openstandia/keycloak-grpc/tree/master/service-spi).
Please see [the sample implementation of admin gRPC service](https://github.com/openstandia/keycloak-grpc/tree/master/admin).

After building your services, you can deploy it by putting it into `$KEYCLOAK_HOME/standalone/deployments` directory simply.
Also, it supports hot deployment thanks to keycloak.

## License

Licensed under the [Apache License 2.0](/LICENSE).
