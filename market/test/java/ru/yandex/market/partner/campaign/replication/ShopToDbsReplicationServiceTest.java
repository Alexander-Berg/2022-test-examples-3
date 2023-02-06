package ru.yandex.market.partner.campaign.replication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.model.FeedInfo;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Прверяем {@link ShopToDbsReplicationServiceTest}.
 */
@DbUnitDataSet(before = "ShopToDbsReplicationServiceTest.before.csv")
class ShopToDbsReplicationServiceTest extends FunctionalTest {

    @Autowired
    private ShopToDbsReplicationService shopToDbsReplicationService;

    @Autowired
    private FeedService feedService;
    @Autowired
    private ParamService paramService;

    @Test
    @DbUnitDataSet(before = "ShopToDbsReplicationServiceTest.vat.before.csv",
            after = "ShopToDbsReplicationServiceTest.vat.after.csv")
    void checkVat() {
        shopToDbsReplicationService.copyVat(101L, 103L);
        shopToDbsReplicationService.copyVat(102L, 104L);
    }

    @ParameterizedTest
    @CsvSource({"Официальный производитель Биомаг-магнитотерапия,domain,domain DBS",
            "краткость,доменчик,краткость DBS",
            "46 символов внутреннее имя что бы тут еще напи,domain.ru,46 символов внутреннее имя что бы тут еще напи " +
                    "DBS"})
    void generateInternalShopNameTest(String internalName, String donorDomain, String expectedName) {
        assertThat(shopToDbsReplicationService.generateInternalShopName(internalName, donorDomain))
                .isEqualTo(expectedName);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "ShopToDbsReplicationServiceTest.feed.before.csv")
    @CsvSource({"101,0", "102,1", "200,0", "300,1", "301,0", "400,0"})
    void copyFeedsTest(long donorPartnerId, int expectedFeedCount) {
        BigDecimal homeRegionId = paramService.getParamNumberValue(ParamType.HOME_REGION, donorPartnerId);

        shopToDbsReplicationService.copyFeeds(donorPartnerId, 103L, homeRegionId.longValue(), 1);
        Map<Long, List<FeedInfo>> datasourcesFeeds = feedService.getDatasourcesFeeds(List.of(103L), true);
        assertThat(datasourcesFeeds.getOrDefault(103L, List.of())).hasSize(expectedFeedCount);
    }

    @ParameterizedTest
    @DisplayName("У дбс, в которых хотим скопировать фид, уже есть дефолтный. Фид должен остаться один")
    @DbUnitDataSet(before = {
            "ShopToDbsReplicationServiceTest.feed.before.csv",
            "ShopToDbsReplicationServiceTest.defaultFeed.before.csv"
    })
    @CsvSource({"101", "102", "200", "300", "301"})
    void copyFeeds_childWithDefaultFeed_defaultReplacedByReal(long donorPartnerId) {
        BigDecimal homeRegionId = paramService.getParamNumberValue(ParamType.HOME_REGION, donorPartnerId);

        shopToDbsReplicationService.copyFeeds(donorPartnerId, 103L, homeRegionId.longValue(), 1);
        Map<Long, List<FeedInfo>> datasourcesFeeds = feedService.getDatasourcesFeeds(List.of(103L), true);
        assertThat(datasourcesFeeds.getOrDefault(103L, List.of())).hasSize(1);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "ShopToDbsReplicationServiceTest.shouldCopyDeliveryRules.before.csv")
    @CsvSource({
            "101,true", //партнер без проблем
            "102,true", //партнер с весовыми СиСами
            "103,false", //партнер с категорийными СиСами
            "104,false", //партнер с автоСиСами(авторасчет)
            "105,false", //партнер с ЯДо
            "106,false",  //партнер с Почтой РФ
            "136967,false", //партнер без проблем
    })
    void shouldCopyDeliveryRulesTest(long partnerId, boolean expectedShouldCopy) {
        assertThat(shopToDbsReplicationService.shouldCopyDeliveryRules(partnerId)).isEqualTo(expectedShouldCopy);
    }

    @DisplayName("Проверяем что скопируется из настроек доставки у партнера")
    @Test
    @DbUnitDataSet(before = "ShopToDbsReplicationServiceTest.shouldCopyDeliveryRules.before.csv",
            after = "ShopToDbsReplicationServiceTest.shouldCopyDeliveryRules.after.csv")
    void checkCopyDeliveryRulesTest() {
        shopToDbsReplicationService.copyDeliveryRules(136967L, 1010L, 1L);
    }

    @Test
    @DbUnitDataSet(before = "ShopToDbsReplicationServiceTest.checkCopyOutletsTest.before.csv",
            after = "ShopToDbsReplicationServiceTest.checkCopyOutletsTest.after.csv")
    void checkCopyOutletsTest() {
        shopToDbsReplicationService.copyOutlets(545773, 1545773, 1L);
    }

}
