### gRPC Examples with Spring and Quarkus

[![CI](https://github.com/AZapata27/grpc-examples/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/AZapata27/grpc-examples/actions/workflows/ci.yml) [![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/AZapata27/grpc-examples/badge)](https://scorecard.dev/viewer/?uri=github.com/AZapata27/grpc-examples)

This repository contains gRPC examples built with Spring and Quarkus. The examples are organized into three projects:

1. **Interface Project**: Contains the original protobuf files and generates the model and service classes in Java. This project is typically shared between the server and clients.

2. **Server Project**: Contains the actual implementation of the project and uses the interface project as a dependency. Here, gRPC services are implemented, and interceptors, events, and security are configured.

3. **Client Projects**: (Optional and potentially many) Any client projects that use pre-generated stubs to access the server.

### Project Structure

- **grpc-interfaces/**: Contains the protobuf files and generates model and service classes in Java.

- **server-grpc-spring-boot/**: Contains the implementation of the gRPC server, exception handler, security config.

- **client-grpc-spring-boot/**: Contains client projects that use pre-generated stubs to access the server.

## Architecture

```mermaid
flowchart LR
    subgraph contracts["grpc-interfaces"]
        proto["helloworld.proto<br/>qrservice.proto"] --> stubs["Generated stubs<br/>MyServiceGrpc / QRServiceGrpc"]
    end

    subgraph client["client-grpc-spring-boot"]
        rest["REST controller"] --> clientSvc["Client services<br/>@GrpcClient stubs"]
        clientSvc --> clientInt["Client interceptors<br/>logging + basic auth credentials"]
    end

    subgraph server["server-grpc-spring-boot"]
        serverInt["Server interceptors<br/>logging + Spring Security auth"] --> impl["Service impl<br/>HelloWordService / QRGeneretedService"]
        impl -. exception .-> advice["GrpcExceptionAdvice<br/>exception to gRPC Status"]
    end

    subgraph quarkus["server-client-grpc-quarkus"]
        qproto["hello.proto"] --> qclient["Mutiny client + REST resource"]
        qclient -- gRPC --> qserver["HelloGrpcService"]
    end

    stubs -. shared dependency .-> clientSvc
    stubs -. shared dependency .-> impl
    clientInt -- "gRPC :9090" --> serverInt
    server -. registers .-> eureka["cloud-eureka-server"]
```

### Additional Configurations

The server and client projects may contain additional configurations such as:

- **Exception Handler**: Handles exceptions by intercepting incoming and outgoing gRPC calls.
  
- **Interceptors**: To intercept incoming and outgoing gRPC calls.

- **Events**: To handle events related to the lifecycle of the server and clients.

- **Security**: To secure gRPC communications through authentication and authorization with spring security and basic auth.

- **Testing**: Validates gRPC functionality through unit and integration testing.

### Running Tests

The repository includes tests to ensure the correct operation of the gRPC services. These tests can be executed using appropriate testing tools for Spring and Quarkus projects.

Each module is an independent Maven project (Java 21). Install the contracts first, then build the rest:

```bash
(cd grpc-interfaces && mvn install)
(cd server-grpc-spring-boot && mvn verify)
(cd client-grpc-spring-boot && mvn verify)
(cd server-client-grpc-quarkus && mvn verify)
```

### License

This project is licensed under the [Apache License 2.0](LICENSE).
