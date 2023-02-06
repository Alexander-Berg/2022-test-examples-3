package ru.yandex.market.adv.shop.integration.metrika.yt.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.model.business.BusinessInfo;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.YtBusinessInfo;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

@DisplayName("Тесты на репозиторий YtBusinessInfoRepository")
class YtBusinessInfoRepositoryTest extends AbstractShopIntegrationTest {

    private static final long BUSINESS_ID = 1L;

    @Autowired
    private YtBusinessInfoRepository ytBusinessInfoRepository;

    @DisplayName("Успешно получили информацию о бизнесе из Yt-таблицы")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/ytBusinessInfoGet_existIds_ytBusinessInfo_business"
            ),
            before = "YtBusinessInfoRepositoryTest/json/ytBusinessInfoGet_existIds_ytBusinessInfo.before.json"
    )
    @Test
    void ytBusinessInfoGet_existIds_ytBusinessInfo() {
        run("ytBusinessInfoGet_existIds_ytBusinessInfo_",
                () -> Assertions.assertThat(
                                ytBusinessInfoRepository
                                        .get(BUSINESS_ID)
                        )
                        .isPresent()
                        .get()
                        .isEqualTo(getBusinessInfo())
        );
    }

    @DisplayName("Успешно обработали случай, когда информация о бизнесе не найдена")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/ytBusinessInfoGet_notExist_empty_business"
            ),
            before = "YtBusinessInfoRepositoryTest/json/ytBusinessInfoGet_notExist_empty.before.json"
    )
    @Test
    void ytBusinessInfoGet_notExist_exception() {
        run("ytBusinessInfoGet_notExist_empty_",
                () -> Assertions.assertThat(
                                ytBusinessInfoRepository
                                        .get(BUSINESS_ID)
                        )
                        .isEmpty()
        );
    }

    private BusinessInfo getBusinessInfo() {
        return new BusinessInfo(BUSINESS_ID, "Бизнес 1", 1, 1649421304L);
    }
}
