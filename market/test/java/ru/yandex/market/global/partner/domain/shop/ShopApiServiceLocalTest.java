package ru.yandex.market.global.partner.domain.shop;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.partner.BaseLocalTest;
import ru.yandex.market.global.partner.api.ShopApiService;
import ru.yandex.market.global.partner.util.RandomDataGenerator;
import ru.yandex.market.global.partner.util.TestPartnerFactory;
import ru.yandex.mj.generated.server.model.ShopPatchDto;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopApiServiceLocalTest extends BaseLocalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ShopApiServiceLocalTest.class).build();
    private final ShopApiService shopApiService;
    private final TestPartnerFactory testPartnerFactory;

    @Test
    public void testPatch() {
        ShopPatchDto patch = new ShopPatchDto()
                .hidden(false);
        shopApiService.apiV1ShopPatchPost(11379949L, patch);
    }
}
