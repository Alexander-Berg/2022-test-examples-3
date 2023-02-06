package ru.yandex.direct.intapi.entity.bs.export;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import static com.google.common.io.Resources.getResource;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.intapi.entity.bs.export.utils.FakeBsExportLogsUtils.fillResponse;

public class FakeBsResponseTest {

    @Test
    public void test() throws IOException {

        URL resource = getResource("bs/export/logs/log_sample.json");

        JsonNode sample = new ObjectMapper().readTree(Resources.toString(resource, Charsets.UTF_8));
        JsonNode sampleCopy = sample.deepCopy();

        sample.elements().forEachRemaining(node -> {
            ((ObjectNode) node.get("response")).removeAll();

            fillResponse("root", node.get("request"), (ObjectNode) node.get("response"));
        });

        assertEquals(sample, sampleCopy);

    }
}
