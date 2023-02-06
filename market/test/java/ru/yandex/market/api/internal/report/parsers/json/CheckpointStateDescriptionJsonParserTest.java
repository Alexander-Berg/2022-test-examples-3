package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import ru.yandex.market.api.internal.report.data.CheckpointStateDescription;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;

/**
 * @author apershukov
 */
public class CheckpointStateDescriptionJsonParserTest {

    @Test
    public void testParse() {
        CheckpointStateDescriptionJsonParser parser = new CheckpointStateDescriptionJsonParser();
        CheckpointStateDescription description = parser.parse(
                ResourceHelpers.getResource("checkpoint-state.json")
        );

        assertEquals(45, description.getId());
        assertEquals("В пункте выдачи", description.getText());
    }
}
