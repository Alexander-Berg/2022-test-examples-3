package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.TaxSystem;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.logistics.nesu.client.model.warehouse.WarehouseContactDto;
import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class RegisterShopClientTest extends AbstractClientTest {
    private static final String TEST_SERVICE_TICKET = "test-service-ticket";
    private static final String TEST_USER_TICKET = "test-user-ticket";

    @Test
    void registerShop() {
        prepareMockRequest("/internal/shops/register", "request/register_shop.json");
        client.registerShop(
            RegisterShopDto.builder()
                .id(10L)
                .marketId(200L)
                .businessId(300L)
                .balanceClientId(255L)
                .balanceContractId(100L)
                .balancePersonId(200L)
                .name("my little shop")
                .role(ShopRole.DROPSHIP)
                .taxSystem(TaxSystem.ESN)
                .siteUrl("mirigrushek.ru")
                .regionId(100)
                .warehouseContact(
                    WarehouseContactDto.builder()
                        .firstName("Игорь")
                        .lastName("Гончаров")
                        .phoneNumber("8-800-555-35-35")
                        .build()
                )
                .createPartnerOnRegistration(true)
                .build()
        );
    }

    void prepareMockRequest(final String path, final String requestFile) {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON);

        mock.expect(requestTo(uri + path))
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(header(HttpTemplate.SERVICE_TICKET_HEADER, TEST_SERVICE_TICKET))
            .andExpect(header(HttpTemplate.USER_TICKET_HEADER, TEST_USER_TICKET))
            .andExpect(jsonRequestContent(requestFile))
            .andRespond(taskResponseCreator);
    }

}
