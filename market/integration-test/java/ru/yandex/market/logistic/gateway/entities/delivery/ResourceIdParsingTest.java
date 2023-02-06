package ru.yandex.market.logistic.gateway.entities.delivery;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;

public class ResourceIdParsingTest extends AbstractIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Сценарий, когда delivery_id не заполнен, а partner_id - заполнен.
     */
    @Test
    public void testParsingWithPartnerId() throws Exception {
        execute("fixtures/entities/delivery/resource_partner_id.json", "4");
    }

    /**
     * Сценарий, когда delivery_id отсутствует, а partner_id - заполнен.
     */
    @Test
    public void testParsingWithOnlyPartnerId() throws Exception {
        execute("fixtures/entities/delivery/resource_partner_id_only.json", "4");
    }

    /**
     * Сценарий, когда и delivery_id и partner_id - отсутствуют.
     */
    @Test
    public void testParsingWithoutPartnerId() throws Exception {
        execute("fixtures/entities/delivery/resource_partner_id_empty.json", null);
    }

    /**
     * Сценарий, когда delivery_id заполнен, а partner_id отсутствует.
     */
    @Test
    public void testParsingWithOnlyDeliveryId() throws Exception {
        execute("fixtures/entities/delivery/resource_delivery_id_only.json", "3");
    }

    /**
     * Сценарий, когда delivery_id заполнен, а partner_id - нет.
     */
    @Test
    public void testParsingWithDeliveryId() throws Exception {
        execute("fixtures/entities/delivery/resource_delivery_id.json", "3");
    }

    private void execute(String fileName, String partnerId) throws IOException {
        String fileContent = getFileContent(fileName);
        ResourceId resourceId = objectMapper.readValue(fileContent, ResourceId.class);

        softAssert.assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("1");

        softAssert.assertThat(resourceId.getDeliveryId())
            .as("Asserting delivery id")
            .isEqualTo(partnerId);

        softAssert.assertThat(resourceId.getPartnerId())
            .as("Asserting partner id")
            .isEqualTo(partnerId);
    }
}
