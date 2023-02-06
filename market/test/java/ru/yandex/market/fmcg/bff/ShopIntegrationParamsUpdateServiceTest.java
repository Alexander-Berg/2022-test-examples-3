package ru.yandex.market.fmcg.bff;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fmcg.bff.shops.basket.ShopIntegrationParamsUpdateService;
import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.fmcg.client.backend.FmcgBackClient;
import ru.yandex.market.fmcg.main.model.CartMethod;
import ru.yandex.market.fmcg.main.model.dto.BasketShopIntegrationParamsDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author semin-serg
 */
public class ShopIntegrationParamsUpdateServiceTest extends FmcgBffTest {

    @Autowired
    FmcgBackClient fmcgBackClient;

    @Autowired
    ShopIntegrationParamsUpdateService shopIntegrationParamsUpdateService;

    @Autowired
    AtomicReference<Map<Long, BasketShopIntegrationParamsDto>> basketShopIntegrationParamsRef;

    private Map<Long, BasketShopIntegrationParamsDto> originalMap;

    @BeforeEach
    void backupOriginalMap() {
        this.originalMap = basketShopIntegrationParamsRef.get();
    }

    @AfterEach
    void restoreOriginalMap() {
        basketShopIntegrationParamsRef.set(originalMap);
    }

    @Test
    public void generalTest() {
        List<BasketShopIntegrationParamsDto> shopIntegrationParams = generateShopIntegrationParams();
        when(fmcgBackClient.getShopsIntegrationParams()).thenAnswer(inv -> new ArrayList<>(shopIntegrationParams));

        shopIntegrationParamsUpdateService.update();

        Map<Long, BasketShopIntegrationParamsDto> actualMap = basketShopIntegrationParamsRef.get();
        assertEquals(
            shopIntegrationParams.stream().map(BasketShopIntegrationParamsDto::getId).collect(Collectors.toSet()),
            actualMap.keySet());
        actualMap.forEach((id, params) -> assertEquals((long) id, params.getId()));
        actualMap.forEach((id, actualParams) -> {
            BasketShopIntegrationParamsDto expectedParams = shopIntegrationParams.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(RuntimeException::new);
            assertEquals(expectedParams.getCartMethod(), actualParams.getCartMethod());
            assertEquals(expectedParams.getCartUrl(), actualParams.getCartUrl());
        });
    }

    List<BasketShopIntegrationParamsDto> generateShopIntegrationParams() {
        return Arrays.asList(
            new BasketShopIntegrationParamsDto(1, CartMethod.GET, "https://example.com/basket"),
            new BasketShopIntegrationParamsDto(2, CartMethod.POST, "https://supershop.ru/api/basket"),
            new BasketShopIntegrationParamsDto(3, CartMethod.GET, "https://somelocalshop.ru/order")
        );
    }

}
