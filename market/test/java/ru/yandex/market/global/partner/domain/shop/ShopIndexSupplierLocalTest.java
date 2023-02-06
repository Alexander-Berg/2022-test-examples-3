package ru.yandex.market.global.partner.domain.shop;

import java.util.stream.IntStream;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.elastic.IndexingService;
import ru.yandex.market.global.partner.BaseLocalTest;
import ru.yandex.market.global.partner.util.RandomDataGenerator;
import ru.yandex.market.global.partner.util.TestPartnerFactory;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopIndexSupplierLocalTest extends BaseLocalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ShopIndexSupplierLocalTest.class).build();

    private final IndexingService indexingService;
    private final ShopIndexSupplier shopIndexSupplier;
    private final TestPartnerFactory testPartnerFactory;

    @SneakyThrows
    @Test
    public void testReindex() {
        IntStream.range(0, 10)
                .forEach(i -> testPartnerFactory.createShopAndAllRequired());

        indexingService.reindex(shopIndexSupplier);
    }
}
