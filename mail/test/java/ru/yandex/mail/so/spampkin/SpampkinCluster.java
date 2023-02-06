package ru.yandex.mail.so.spampkin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.util.filesystem.CloseableDeleter;
import ru.yandex.util.ip.CidrListConfigBuilder;

public class SpampkinCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    private final StaticServer jrbld;
    private final StaticServer shingler;
    private final Spampkin spampkin;

    public SpampkinCluster() throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            jrbld = new StaticServer(Configs.baseConfig("JRBLD"));
            chain.get().add(jrbld);

            shingler = new StaticServer(Configs.baseConfig("Shingler"));
            chain.get().add(shingler);

            SpampkinConfigBuilder builder = new SpampkinConfigBuilder();
            builder.port(0);
            builder.connections(2);
            builder.jrbldConfig(Configs.hostConfig(jrbld));
            builder.hamCheckName("fast-ham");
            builder.spamCheckName("fast-spam");
            builder.rejectCheckName("fast-reject");
            builder.shinglerConfig(Configs.hostConfig(shingler));

            Path intranetIps = Files.createTempFile("intranet", ".txt");
            chain.get().add(new CloseableDeleter(intranetIps));
            Files.write(
                intranetIps,
                Arrays.asList(
                    "77.88.46.0/23",
                    "77.88.60.0/23",
                    "123.44.33.22"),
                StandardCharsets.UTF_8);
            builder.intranetIps(
                new CidrListConfigBuilder().file(intranetIps.toFile()));

            spampkin = new Spampkin(builder.build());
            chain.get().add(spampkin);
            reset(chain.release());
        }
    }

    public void start() throws IOException {
        jrbld.start();
        shingler.start();
        spampkin.start();
    }

    public StaticServer jrbld() {
        return jrbld;
    }

    public StaticServer shingler() {
        return shingler;
    }

    public Spampkin spampkin() {
        return spampkin;
    }
}

