package ru.yandex.market.logistic.gateway.entities.delivery;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;

import static ru.yandex.market.logistic.gateway.utils.delivery.ApiDtoFactory.createItemInstances;

public class ItemInstancesParsingTest extends AbstractIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Сценарий, когда поле instance заполнено.
     */
    @Test
    public void testParsingInstances() throws IOException {
        Item item = preAssertItem("fixtures/entities/delivery/item_instances.json");

        softAssert.assertThat(item.getInstances())
            .as("Asserting instances")
            .isEqualTo(createItemInstances());
    }

    /**
     * Сценарий, когда поле instance – пустой список
     */
    @Test
    public void testParsingInstancesEmpty() throws IOException {
        Item item = preAssertItem("fixtures/entities/delivery/item_instances_empty.json");

        softAssert.assertThat(item.getInstances())
            .as("Asserting instances empty")
            .isEqualTo(new ArrayList<>());
    }

    /**
     * Сценарий, когда поле instance не пришло совсем.
     */
    @Test
    public void testParsingInstancesNull() throws IOException {
        Item item = preAssertItem("fixtures/entities/delivery/item_instances_null.json");

        softAssert.assertThat(item.getInstances())
            .as("Asserting instances is null")
            .isEqualTo(null);
    }

    /**
     * Сценарий, когда поле instance заполнено дополнительно странной шляпой.
     */
    @Test
    public void testParsingInstancesNonStandard() throws IOException {
        Item item = preAssertItem("fixtures/entities/delivery/item_instances_non_standard.json");

        List<Map<String, String>> instances = List.of(
            Map.of("cis", "123abc"),
            Map.of("cis", "cba321", "test", "test")
        );
        softAssert.assertThat(item.getInstances())
            .as("Asserting instances")
            .isEqualTo(instances);
    }

    private Item preAssertItem(String filename) throws IOException {
        String fileContent = getFileContent(filename);
        Item item = objectMapper.readValue(
            fileContent, Item.class
        );

        softAssert.assertThat(item.getName())
            .as("Asserting name")
            .isEqualTo("name");

        softAssert.assertThat(item.getCount())
            .as("Asserting count")
            .isEqualTo(1);

        softAssert.assertThat(item.getPrice())
            .as("Asserting price")
            .isEqualTo(BigDecimal.valueOf(100));
        return item;
    }
}
