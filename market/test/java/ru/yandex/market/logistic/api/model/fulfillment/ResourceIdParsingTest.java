package ru.yandex.market.logistic.api.model.fulfillment;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

import static ru.yandex.market.logistic.api.client.LogisticApiClientFactory.createXmlMapper;

public class ResourceIdParsingTest extends ParsingTest<ResourceId> {

    public ResourceIdParsingTest() {
        super(createXmlMapper(), ResourceId.class, "fixture/entities/fulfillment/resource_id.xml");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
            "yandexId", "1",
            "fulfillmentId", "2",
            "partnerId", "2"
        );
    }

    /**
     * Сценарий, когда fulfillment_id не заполнен, а partner_id - заполнен.
     */
    @Test
    public void testParsingWithPartnerId() throws Exception {
        execute("fixture/entities/fulfillment/resource_partner_id.xml", "4");
    }

    /**
     * Сценарий, когда fulfillment_id отсутствует, а partner_id - заполнен.
     */
    @Test
    public void testParsingWithOnlyPartnerId() throws Exception {
        execute("fixture/entities/fulfillment/resource_partner_id_only.xml", "4");
    }

    /**
     * Сценарий, когда и fulfillment_id и partner_id - отсутствуют.
     */
    @Test
    public void testParsingWithoutPartnerId() throws Exception {
        execute("fixture/entities/fulfillment/resource_partner_id_empty.xml", null);
    }

    /**
     * Сценарий, когда fulfillment_id заполнен, а partner_id отсутствует.
     */
    @Test
    public void testParsingWithOnlyFulfillmentId() throws Exception {
        execute("fixture/entities/fulfillment/resource_fulfillment_id_only.xml", "3");
    }

    /**
     * Сценарий, когда fulfillment_id заполнен, а partner_id - нет.
     */
    @Test
    public void testParsingWithFulfillmentId() throws Exception {
        execute("fixture/entities/fulfillment/resource_fulfillment_id.xml", "3");
    }

    private void execute(String fileName, String partnerId) throws java.io.IOException {
        String fileContent = getFileContent(fileName);
        ResourceId resourceId = getMapper().readValue(fileContent, ResourceId.class);

        assertions().assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("1");

        assertions().assertThat(resourceId.getFulfillmentId())
            .as("Asserting fulfillment id")
            .isEqualTo(partnerId);

        assertions().assertThat(resourceId.getPartnerId())
            .as("Asserting partner id")
            .isEqualTo(partnerId);
    }
}
