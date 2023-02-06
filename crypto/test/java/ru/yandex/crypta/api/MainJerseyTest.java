package ru.yandex.crypta.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.crypta.api.proto.TApiServerConfig;
import ru.yandex.crypta.common.exception.Exceptions;
import ru.yandex.crypta.config.Initialization;
import ru.yandex.crypta.lib.proto.TTvmConfig;
import ru.yandex.misc.test.Assert;

public class MainJerseyTest extends JerseyTest {
    private static final Logger LOG = LoggerFactory.getLogger(MainJerseyTest.class);

    private int getTvmRecipePort() {
        try {
            String port = Files.readString(Paths.get(".","tvmapi.port"));

            return Integer.parseInt(port);
        } catch (IOException e) {
            LOG.error("Unable to read local TVM port from file");
            throw Exceptions.unavailable();
        }
    }

    @Override
    protected Application configure() {
        Initialization.run(Main.class);
        var main = new Main(new String[]{}) {
            @Override
            protected TApiServerConfig updateConfig(TApiServerConfig config) {
                var mutableConfig = config.toBuilder();
                mutableConfig.getDevelopmentBuilder().setDisableScheduling(true);
                mutableConfig.getDevelopmentBuilder().setDisableAuth(true);
                mutableConfig.getDevelopmentBuilder().setDisableIdm(true);
                mutableConfig.setTvm(
                        mutableConfig.getTvmBuilder().setSource(
                                TTvmConfig.newBuilder()
                                        .setSourceTvmId(1000600)
                                        .setSecret("WiLXMmbqhyOqxicxAb76ow")
                                        .setLocalhostPort(getTvmRecipePort())
                        )
                );
                return mutableConfig.build();
            }

        };
        return main.getResourceConfig();
    }

    @Test
    public void testPing() {
        var response = target("/ping").request().get();
        Assert.equals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testSolomon() {
        var response = target("/solomon").request().get();
        Assert.equals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

}
