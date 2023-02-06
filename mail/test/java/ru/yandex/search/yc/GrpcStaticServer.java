package ru.yandex.search.yc;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.http.HttpHost;

import ru.yandex.function.GenericAutoCloseable;

public class GrpcStaticServer implements GenericAutoCloseable<IOException> {
    private final Server server;

    public GrpcStaticServer(final BindableService service) {
        this(0, service);
    }

    public GrpcStaticServer(
        final int port,
        final BindableService service)
    {
        server = ServerBuilder.forPort(port).addService(service).build();
    }

    public void start() throws IOException {
        server.start();
    }

    public int port() {
        return server.getPort();
    }

    public HttpHost host() {
        return new HttpHost("localhost", port());
    }

    @Override
    public void close() throws IOException {
        server.shutdown();
        server.shutdownNow();
    }
}
