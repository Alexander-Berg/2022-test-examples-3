package ru.yandex.market.adv.shop.integration.metrika.yt.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.Shopsdat;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

@DisplayName("Тесты на репозиторий YtShopsdatRepository")
class YtShopsdatRepositoryTest extends AbstractShopIntegrationTest {

    private static final long BUSINESS_ID = 1L;

    @Autowired
    private YtShopsdatRepository ytShopsdatRepository;

    @DisplayName("Успешно получили информацию о партнерах бизнеса из Yt-таблицы")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/getPartners_existIds_listParners_shopsdat"
            ),
            before = "YtShopsdatRepositoryTest/json/getPartners_existIds_listParners.before.json"
    )
    @Test
    void getPartners_existIds_listParners() {
        run("getPartners_existIds_listParners_",
                () -> Assertions.assertThat(
                                ytShopsdatRepository.getPartners(BUSINESS_ID)
                        )
                        .containsExactlyInAnyOrder(1L, 3L)
        );
    }

    @DisplayName("Успешно обработали случай, когда партнеры бизнеса не найдены")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/getPartners_notExist_empty_shopsdat"
            ),
            before = "YtShopsdatRepositoryTest/json/getPartners_notExist_empty.before.json"
    )
    @Test
    void getPartners_notExist_empty() {
        run("getPartners_notExist_empty_",
                () -> Assertions.assertThat(
                                ytShopsdatRepository.getPartners(BUSINESS_ID)
                        )
                        .isEmpty()
        );
    }
}
