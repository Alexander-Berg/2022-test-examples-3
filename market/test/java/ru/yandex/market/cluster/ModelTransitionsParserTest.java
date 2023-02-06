package ru.yandex.market.cluster;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class ModelTransitionsParserTest {
    // /var/lib/yandex/market-data-getter/mbo_clusters/recent/model-transitions.json
    private static final String JSON =
            "[{\"from_id\": 4502,\"strong_to_id\": 4503,\"model_delete_timestamp\": 1457653141514}\n" +
                    ",{\"from_id\": 4503,\"strong_to_id\": 4505," +
                    "\"weak_to_id\": [],\"model_delete_timestamp\": 1457653141514}\n" +
                    ",{\"from_id\": 4504,\"strong_to_id\": 4506," +
                    "\"weak_to_id\": [4526],\"model_delete_timestamp\": 1457653141514}\n" +
                    ",{\"from_id\": 4508,\"strong_to_id\": 4526," +
                    "\"weak_to_id\": [4527, 4528],\"model_delete_timestamp\": 1457653141524}]";

    //http://cs-clusterizer01g.yandex.ru:33714/transitions/
    //                          getModelsTransitionsJson?start=1466141149000&end=1467141149000
    private static final String GET_MODELS_TRANSITIONS_JSON =
            "[{\"from_id\": 1,\"strong\": {\"to_id\": 111,\"offer_id\"" +
                    ": [\"304b7dfc2813c3a623c01774cf4ad72c\"],\"offers_count\": 1}," +
                    "\"weak\": [{\"to_id\": 1693119718,\"offers_count\": 1}]," +
                    "\"model_delete_timestamp\": 1466144890611}," +
                    "{\"from_id\": 2, \"weak\": [{\"to_id\": 111,\"offers_count\": 1}, " +
                    "{\"to_id\": 222,\"offers_count\": 10}],\"model_delete_timestamp\": 1466144890611}," +
                    "{\"from_id\": 3, \"missing\": {\"to_id\": 0," +
                    "\"offer_id\": [\"0d3f0f5857c8c6e8ad10d4172df3211f\"]},\"model_delete_timestamp\": 1466144890611}]";
    private static final Instant MODEL_DELETE_TIMESTAMP = Instant.parse("2016-03-10T23:39:01.514Z");
    private ModelTransitionsParser parser = new ModelTransitionsParser();
    private Path jsonFilePath;
    private Path jsonModelTransitionsFilePath;

    @Before
    public void prepareJsonFile() throws Exception {
        jsonFilePath = Files.createTempFile("model-transitions_" + UUID.randomUUID(), ".json");
        Files.write(jsonFilePath, JSON.getBytes(StandardCharsets.UTF_8));

        jsonModelTransitionsFilePath = Files.createTempFile("get-model-transitions_" + UUID.randomUUID(), ".json");
        Files.write(jsonModelTransitionsFilePath, GET_MODELS_TRANSITIONS_JSON.getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void deleteJsonFile() throws Exception {
        Files.delete(jsonFilePath);
        Files.delete(jsonModelTransitionsFilePath);
    }

    @Test
    public void parseTransitions() throws Exception {
        TransitionCaptor<ModelTransitionsParser.ModelTransition> transitionCaptor = new TransitionCaptor<>();
        parser.parse(jsonFilePath.toString(), transitionCaptor);
        final List<ModelTransitionsParser.ModelTransition> transitions = transitionCaptor.getTransitions();

        assertEquals(4, transitions.size());

        assertEquals(4502, transitions.get(0).getFromId());
        assertEquals(4503, transitions.get(0).getStrongToId());
        assertEquals(MODEL_DELETE_TIMESTAMP, transitions.get(0).getModelDeleteTimestamp());

        assertEquals(4508, transitions.get(3).getFromId());
        assertEquals(4526, transitions.get(3).getStrongToId());

        System.out.println(transitions.get(0).getModelDeleteTimestamp());
    }

    @Test
    public void parseModelTransitions() throws Exception {
        TransitionCaptor<ModelTransitionsParser.ModelTransitionFull> transitionCaptor = new TransitionCaptor<>();
        parser.parseFromStream(new FileInputStream(jsonModelTransitionsFilePath.toString()), transitionCaptor);
        final List<ModelTransitionsParser.ModelTransitionFull> transitions = transitionCaptor.getTransitions();

        assertEquals(3, transitions.size());
        assertNotNull(transitions.get(0).getStrongToId());
        assertEquals(111, transitions.get(0).getStrongToId().longValue());

        assertNotNull(transitions.get(1).getStrongToId());
        assertEquals(222, transitions.get(1).getStrongToId().longValue());

        assertNull(transitions.get(2).getStrongToId());
    }

    private static class TransitionCaptor<T> implements Consumer<T> {
        private final List<T> transitions = new ArrayList<>();

        @Override
        public void accept(T transition) {
            transitions.add(transition);
        }

        public List<T> getTransitions() {
            return transitions;
        }
    }
}
