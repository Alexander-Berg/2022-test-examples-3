package ru.yandex.market.billing.marketing;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.marketing.MarketingCampaignType;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class DailyMarketingCampaignCompensationServiceTest extends FunctionalTest {
    @Autowired
    PromoOrderItemDao promoOrderItemDao;

    @Autowired
    @Qualifier("pgDailyMarketingCampaignCompensationService")
    DailyMarketingCampaignCompensationService campaignCompensationService;

    @Autowired
    MarketingCampaignDao pgMarketingCampaignDao;

    @Test
    @DisplayName("Получение остатков по кампаниям без обилленых заказов")
    @DbUnitDataSet(
            before = "DailyMarketingCampaignCompensationServiceTest.common.csv"
    )
    void calculateRemainAmountsWithoutOrdersTest() {
        LocalDate date = LocalDate.parse("2021-06-15");
        List<MarketingCampaign> campaings = getCampaignsForBilling(date);

        Map<Long, Long> remainAmountsTillDate = campaignCompensationService
                .calculateRemainAmountsTillDate(date, campaings);

        assertThat(remainAmountsTillDate).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        201L, 100500L,
                        202L, 100500L,
                        203L, 100500L,
                        204L, 100500L,
                        205L, 100500L,
                        206L, 100500L
                )
        );
    }

    @Test
    @DisplayName("Сохранение дневных начислений по кампаниям")
    @DbUnitDataSet(
            before = "DailyMarketingCampaignCompensationServiceTest.common.csv",
            after = "DailyMarketingCampaignCompensationServiceTest.persistoneday.after.csv"
    )
    void persistDailyCampaignCompensation() {
        LocalDate date = LocalDate.parse("2021-06-15");
        campaignCompensationService.persistDailyCampaignCompensation(List.of(
                DailyMarketingCampaignCompensation.builder()
                        .setCampaignId(201L)
                        .setAmount(11000L)
                        .setBillinDate(date)
                        .build(),
                DailyMarketingCampaignCompensation.builder()
                        .setCampaignId(202L)
                        .setAmount(12000L)
                        .setBillinDate(date)
                        .build(),
                DailyMarketingCampaignCompensation.builder()
                        .setCampaignId(203L)
                        .setAmount(13000L)
                        .setBillinDate(date)
                        .build(),
                DailyMarketingCampaignCompensation.builder()
                        .setCampaignId(204L)
                        .setAmount(14000L)
                        .setBillinDate(date)
                        .build(),
                DailyMarketingCampaignCompensation.builder()
                        .setCampaignId(205L)
                        .setAmount(15000L)
                        .setBillinDate(date)
                        .build(),
                DailyMarketingCampaignCompensation.builder()
                        .setCampaignId(206L)
                        .setAmount(16000L)
                        .setBillinDate(date)
                        .build()
        ));
    }

    @Test
    @DisplayName("Получение остатков по кампаниям с обиллеными заказами")
    @DbUnitDataSet(
            before = {
                    "DailyMarketingCampaignCompensationServiceTest.common.csv",
                    "DailyMarketingCampaignCompensationServiceTest.before.csv"
            }
    )
    void calculateRemainAmountsWithOrdersTest() {
        LocalDate date = LocalDate.parse("2021-06-16");
        List<MarketingCampaign> campaings = getCampaignsForBilling(date);

        Map<Long, Long> remainAmountsTillDate = campaignCompensationService
                .calculateRemainAmountsTillDate(date, campaings);

        assertThat(remainAmountsTillDate).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        204L, 100500L - 14000,
                        205L, 100500L - 15000,
                        206L, 100500L - 16000
                )
        );

        date = LocalDate.parse("2021-06-17");
        campaings = getCampaignsForBilling(date);

        remainAmountsTillDate = campaignCompensationService
                .calculateRemainAmountsTillDate(date, campaings);

        assertThat(remainAmountsTillDate).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        203L, 100500L - 13000
                )
        );
    }

    @Test
    @DisplayName("Получение остатков по кампаниям с обиллеными заказами и корректировками")
    @DbUnitDataSet(
            before = {
                    "DailyMarketingCampaignCompensationServiceTest.common.csv",
                    "DailyMarketingCampaignCompensationServiceTest.before.csv",
                    "DailyMarketingCampaignCompensationServiceTest.corrections.before.csv"
            }
    )
    void calculateRemainAmountsWithOrdersAndCorrectionsTest() {
        LocalDate date = LocalDate.parse("2021-06-16");
        List<MarketingCampaign> campaings = List.of(
                defaultCampaign().setId(204).setSum(100500).build(),
                defaultCampaign().setId(205).setSum(100500).build()
        );

        Map<Long, Long> remainAmountsTillDate = campaignCompensationService
                .calculateRemainAmountsTillDate(date, campaings);

        assertThat(remainAmountsTillDate).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        204L, 100500L - 14000 - 10000,
                        205L, 100500L - 15000 - 1000 - 1000 - (-900) - 80 - 7
                )
        );
    }

    private static MarketingCampaign.Builder defaultCampaign() {
        return MarketingCampaign.builder()
                .setId(-1)
                .setPartnerId(-1)
                .setStartDate(LocalDate.parse("1970-01-01"))
                .setEndDate(LocalDate.parse("2099-12-31"))
                .setImportDate(LocalDate.parse("2021-06-15"))
                .setType(MarketingCampaignType.CASHBACK)
                .setSum(100500)
                .setAnaplanId(0)
                .setCurrency("")
                .setNds(true);
    }

    private List<MarketingCampaign> getCampaignsForBilling(LocalDate date) {
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date);
        return pgMarketingCampaignDao.getCampaignsByAnaplanIds(
                promoOrderItemDao.getOrderCampaignAnaplanIdsByItemIds(date, itemIds)
        );
    }

    @Test
    @DisplayName("Очистка дневных начислений по кампаниям")
    @DbUnitDataSet(
            before = {
                    "DailyMarketingCampaignCompensationServiceTest.common.csv",
                    "DailyMarketingCampaignCompensationServiceTest.before.csv"
            },
            after = "DailyMarketingCampaignCompensationServiceTest.reset.after.csv"
    )
    void resetDailyCampaignCompensationTest() {
        LocalDate date = LocalDate.parse("2021-06-16");
        campaignCompensationService.resetDailyCampaignAmounts(date);
    }

    @Test
    void calculateRemainAmountsTillDate() {
    }
}
