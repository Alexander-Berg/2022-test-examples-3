package ru.yandex.mail.diffusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.diffusion.IncrementalObject.Enumeration;
import ru.yandex.mail.diffusion.IncrementalObject.NonIncremental;
import ru.yandex.mail.diffusion.json.JsonPatchCollector;
import ru.yandex.mail.diffusion.json.JsonPatchProvider;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPatchTest extends BaseTest {
    private static final IncrementalObject FIRST = new IncrementalObject(
        true,
        "str",
        22,
        18L,
        Set.of("A", "B"),
        Set.of((byte) -1, (byte) 1),
        new NonIncremental("non", 11),
        OptionalLong.of(100500L),
        Optional.of((short)0),
        Enumeration.ONE
    );

    private static final IncrementalObject SECOND = new IncrementalObject(
        false,
        "trs",
        33,
        54L,
        Set.of("B", "C"),
        Set.of((byte) 1, (byte) 5),
        new NonIncremental("non", 99),
        OptionalLong.empty(),
        Optional.of((short) -1),
        Enumeration.TWO
    );

    private final static ObjectMapper objectMapper = new ObjectMapper();

    private static String takePatch(IncrementalObject from, IncrementalObject to) {
        try (val collector = new JsonPatchCollector(objectMapper)) {
            return collector.collect(from, to, MATCHER);
        }
    }

    @BeforeAll
    static void init() {
        objectMapper
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("Verify json patch collection")
    void testGetPatch() throws Exception {
        val expectedPatch =
            "{"
          + "    \"bool\":      {\"old\": true, \"new\": false},"
          + "    \"string\":    {\"old\": \"str\", \"new\": \"trs\"},"
          + "    \"integer\":   {\"old\": 22, \"new\": 33},"
          + "    \"boxedLong\": {\"old\": 18, \"new\": 54},"
          + "    \"set\": {"
          + "        \"add\": [\"C\"],"
          + "        \"del\": [\"A\"]"
          + "    },"
          + "    \"byteSet\": {"
          + "        \"add\": [5],"
          + "        \"del\": [-1]"
          + "    },"
          + "    \"pojo\": {"
          + "        \"old\": {\"string\": \"non\", \"integer\": 11},"
          + "        \"new\": {\"string\": \"non\", \"integer\": 99}"
          + "    },"
          + "    \"optionalLong\":  {\"old\": 100500, \"new\": null},"
          + "    \"optionalShort\": {\"old\": 0, \"new\": -1},"
          + "    \"enumeration\":   {\"old\": \"ONE\", \"new\": \"TWO\"}"
          + "}";

        val patch = takePatch(FIRST, SECOND);
        val patchTree = objectMapper.readTree(patch);
        val expectedPatchTree = objectMapper.readTree(expectedPatch);

        assertThat(patchTree).isEqualTo(expectedPatchTree);
    }

    @Test
    @DisplayName("Verify patch apply")
    void applyPatchTest() {
        val patch = takePatch(FIRST, SECOND);
        val provider = new JsonPatchProvider(objectMapper, patch);
        val result = APPLIER.apply(FIRST, provider);
        assertThat(result).isEqualTo(SECOND);
    }
}
