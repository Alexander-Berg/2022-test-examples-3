package ru.yandex.mail.micronaut.tvm;

import io.micronaut.context.env.ActiveEnvironment;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.core.io.ResourceLoader;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public class TvmToolPropertySourceLoader implements PropertySourceLoader {
    private static final Path TVM_TOOL_PORT_FILE = Paths.get("tvmtool.port");
    private static final Path TVM_TOOL_TOKEN_FILE = Paths.get("tvmtool.authtoken");

    private Map<String, Object> readProperties() throws IOException {
        return Map.of(
            "tvmtool.port", Integer.parseInt(Files.readString(TVM_TOOL_PORT_FILE)),
            "tvmtool.token", Files.readString(TVM_TOOL_TOKEN_FILE)
        );
    }

    @Override
    @Deprecated
    @SneakyThrows
    public Optional<PropertySource> load(String resourceName, ResourceLoader resourceLoader,
                                         @Nullable String environmentName) {
        return Optional.ofNullable(environmentName)
            .map(name -> ActiveEnvironment.of(name, 0))
            .flatMap(env -> loadEnv(resourceName, resourceLoader, env));
    }

    @Override
    @SneakyThrows
    public Optional<PropertySource> loadEnv(String resourceName, ResourceLoader resourceLoader, ActiveEnvironment activeEnvironment) {
        if (Environment.TEST.equals(activeEnvironment.getName())) {
            return Optional.of(PropertySource.of(readProperties()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Object> read(String name, InputStream input) throws IOException {
        return readProperties();
    }
}
