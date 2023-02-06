package ru.yandex.market.stats.test.data;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Slf4j
public class TestDataResolver {

    private static final Map<String, String> RESOURCE_MAPPING = ImmutableMap.<String, String>builder()
        .put("shops-utf8-prod.dat.gz", "591667636")
        .put("shops-utf8-testing.dat.gz", "591670839")
        .put("geobase.tsv.gz", "607392130")
        .put("models_1336.pb", "591679679")
        .put("tovar-tree.pb.gz", "591681017")
        .put("global.vendors.xml.gz", "591685357")
        .put("pof-states.gz", "616965885")
        .put("sessions.tsv.gz", "591664349")
        .build();

    private static final String SANDBOX_PREFIX = "sandbox:";

    public static InputStream getResource(String name) throws IOException {
        if (name.startsWith(SANDBOX_PREFIX)) {
            name = name.substring(SANDBOX_PREFIX.length());
            String dataRoot = ru.yandex.devtools.test.Paths.getSandboxResourcesRoot();

            try {
                return dataRoot == null ? new FileInputStream(name) : new FileInputStream(dataRoot + "/" + name);
            } catch (FileNotFoundException e) {
                return new URL("https://proxy.sandbox.yandex-team.ru/" + RESOURCE_MAPPING.get(name)).openStream();
            }
        }
        return TestDataResolver.class.getResourceAsStream(name);
    }
}
