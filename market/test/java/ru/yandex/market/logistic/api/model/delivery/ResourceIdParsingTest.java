package ru.yandex.market.logistic.api.model.delivery;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

import static ru.yandex.market.logistic.api.client.LogisticApiClientFactory.createXmlMapper;

public class ResourceIdParsingTest extends ParsingTest<ResourceId> {

    ResourceIdParsingTest() {
        super(createXmlMapper(), ResourceId.class, "fixture/entities/delivery/resource_id.xml");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
            "yandexId", "1",
            "deliveryId", "2",
            "partnerId", "2"
        );
    }

    /**
     * Сценарий, когда delivery_id не заполнен, а partner_id - заполнен.
     */
    @Test
    void testParsingWithPartnerId() throws Exception {
        execute("fixture/entities/delivery/resource_partner_id.xml", "4");
    }

    /**
     * Сценарий, когда delivery_id отсутствует, а partner_id - заполнен.
     */
    @Test
    void testParsingWithOnlyPartnerId() throws Exception {
        execute("fixture/entities/delivery/resource_partner_id_only.xml", "4");
    }

    /**
     * Сценарий, когда и delivery_id и partner_id - отсутствуют.
     */
    @Test
    void testParsingWithoutPartnerId() throws Exception {
        execute("fixture/entities/delivery/resource_partner_id_empty.xml", null);
    }

    /**
     * Сценарий, когда delivery_id заполнен, а partner_id отсутствует.
     */
    @Test
    void testParsingWithOnlyDeliveryId() throws Exception {
        execute("fixture/entities/delivery/resource_delivery_id_only.xml", "3");
    }

    /**
     * Сценарий, когда delivery заполнен, а partner_id - нет.
     */
    @Test
    void testParsingWithDeliveryId() throws Exception {
        execute("fixture/entities/delivery/resource_delivery_id.xml", "3");
    }

    private void execute(String fileName, String partnerId) throws java.io.IOException {
        String fileContent = getFileContent(fileName);
        ResourceId resourceId = getMapper().readValue(fileContent, ResourceId.class);

        assertions().assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("1");

        assertions().assertThat(resourceId.getDeliveryId())
            .as("Asserting delivery id")
            .isEqualTo(partnerId);

        assertions().assertThat(resourceId.getPartnerId())
            .as("Asserting partner id")
            .isEqualTo(partnerId);
    }
}
