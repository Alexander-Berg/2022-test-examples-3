package ru.yandex.market.global.index.domain.offer;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopCategoryResolverTest extends BaseLocalTest {

    private final ShopToMarketCategoryDictionary shopToMarketCategoryDictionary;

    @Test
    public void test() {
        String marketCategoryId = shopToMarketCategoryDictionary
                .resolve(6965998L, "קומבינציות");

        Assertions.assertThat(marketCategoryId).isEqualTo("46");
    }

}
