package ru.yandex.market.oms.utils;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFindModulePath extends TestBase {

    @Test
    public void testNotFound() throws URISyntaxException {
        Configuration configuration = new Configuration();
        configuration.setPipelineId("test");
        ProcaasYamlPatcher patcher = new ProcaasYamlPatcher(configuration,
                mapper,
                Path.of(ClassLoader.getSystemResource("example1").toURI()));

        Optional<Path> path = patcher.findModulePath();

        assertTrue(path.isEmpty());
    }

    @Test
    public void testFound() throws URISyntaxException {
        Configuration configuration = new Configuration();
        configuration.setPipelineId("oevent-pipeline/oevent-pipeline/update-order-status-pipeline");
        ProcaasYamlPatcher patcher = new ProcaasYamlPatcher(configuration,
                mapper,
                Path.of(ClassLoader.getSystemResource("example1").toURI()));

        Optional<Path> path = patcher.findModulePath();

        assertTrue(path.isPresent());
    }
}
