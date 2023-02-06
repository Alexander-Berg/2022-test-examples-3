package ru.yandex.market.logistics.nesu.client;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class GetShopWithSendersClientTest extends AbstractClientTest {

    @Test
    void getShopWithSenders() {
        prepareMockRequest(
            extractFileContent("response/shop_with_senders_response.json"),
            extractFileContent("request/shop_with_sendes_by_market_ids.json")
        );

        List<ShopWithSendersDto> shops = client.getActiveShopsByMarketId(Set.of(1L, 2L));
        checkShopWithSendersResponse(shops);
    }

    @Test
    void getEmptyShopWithSenders() {
        prepareMockRequest("[]", extractFileContent("request/shop_with_sendes_by_market_ids.json"));

        List<ShopWithSendersDto> shops = client.getActiveShopsByMarketId(Set.of(1L, 2L));
        softly.assertThat(shops).hasSize(0);
    }

    @Test
    void getEmptyRequestShopWithSenders() {
        List<ShopWithSendersDto> shops = client.getActiveShopsByMarketId(Set.of());

        softly.assertThat(shops).hasSize(0);
    }

    @Test
    void getShopWithSendersByIds() {
        prepareMockRequest(
            extractFileContent("response/shop_with_senders_response.json"),
            extractFileContent("request/shop_with_sendes_by_shop_ids.json")
        );

        List<ShopWithSendersDto> shops = client.searchShopWithSenders(
            ShopWithSendersFilter.builder().shopIds(Set.of(3L, 4L)).build()
        );
        checkShopWithSendersResponse(shops);
    }

    @Test
    void getEmptyShopWithSendersByIds() {
        ShopWithSendersFilter filter = ShopWithSendersFilter.builder().shopIds(Set.of(3L, 4L)).build();
        prepareMockRequest("[]", extractFileContent("request/shop_with_sendes_by_shop_ids.json"));
        List<ShopWithSendersDto> shops = client.searchShopWithSenders(filter);

        softly.assertThat(shops).hasSize(0);
    }

    private void checkShopWithSendersResponse(List<ShopWithSendersDto> shops) {
        assertThat(shops).hasSize(2);
        softly.assertThat(shops.get(0)).usingRecursiveComparison().isEqualTo(
            ShopWithSendersDto.builder()
                .id(1L)
                .marketId(1L)
                .balanceContractId(12L)
                .name("shop_one")
                .senders(List.of(createNamedEntity(101L, "sender_one")))
                .build()
        );
        softly.assertThat(shops.get(1)).usingRecursiveComparison().isEqualTo(
            ShopWithSendersDto.builder()
                .id(2L)
                .marketId(2L)
                .balanceContractId(42L)
                .name("shop_two")
                .senders(List.of(
                    createNamedEntity(102L, "sender_two"),
                    createNamedEntity(103L, "sender_three")
                ))
                .build()
        );
    }

    void prepareMockRequest(String content, String request) {
        mock.expect(requestTo(uri + "/internal/shops/search"))
            .andExpect(content().json(request))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK).body(content).contentType(APPLICATION_JSON));
    }

    @Nonnull
    private NamedEntity createNamedEntity(long id, String name) {
        return NamedEntity.builder().id(id).name(name).build();
    }
}
