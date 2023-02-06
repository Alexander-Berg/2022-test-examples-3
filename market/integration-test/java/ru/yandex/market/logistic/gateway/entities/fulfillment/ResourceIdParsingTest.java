package ru.yandex.market.logistic.gateway.entities.fulfillment;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

public class ResourceIdParsingTest extends AbstractIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Сценарий, когда fulfillment_id не заполнен, а partner_id - заполнен.
     */
    @Test
    public void testParsingWithPartnerId() throws Exception {
        execute("fixtures/entities/fulfillment/resource_partner_id.json", "4");
    }

    /**
     * Сценарий, когда fulfillment_id отсутствует, а partner_id - заполнен.
     */
    @Test
    public void testParsingWithOnlyPartnerId() throws Exception {
        execute("fixtures/entities/fulfillment/resource_partner_id_only.json", "4");
    }

    /**
     * Сценарий, когда и fulfillment_id и partner_id - отсутствуют.
     */
    @Test
    public void testParsingWithoutPartnerId() throws Exception {
        execute("fixtures/entities/fulfillment/resource_partner_id_empty.json", null);
    }

    /**
     * Сценарий, когда fulfillment_id заполнен, а partner_id отсутствует.
     */
    @Test
    public void testParsingWithOnlyFulfillmentId() throws Exception {
        execute("fixtures/entities/fulfillment/resource_fulfillment_id_only.json", "3");
    }

    /**
     * Сценарий, когда fulfillment_id заполнен, а partner_id - нет.
     */
    @Test
    public void testParsingWithFulfillmentId() throws Exception {
        execute("fixtures/entities/fulfillment/resource_fulfillment_id.json", "3");
    }

    private void execute(String fileName, String partnerId) throws IOException {
        String fileContent = getFileContent(fileName);
        ResourceId resourceId = objectMapper.readValue(fileContent, ResourceId.class);

        softAssert.assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("1");

        softAssert.assertThat(resourceId.getFulfillmentId())
            .as("Asserting fulfillment id")
            .isEqualTo(partnerId);

        softAssert.assertThat(resourceId.getPartnerId())
            .as("Asserting partner id")
            .isEqualTo(partnerId);
    }
}
