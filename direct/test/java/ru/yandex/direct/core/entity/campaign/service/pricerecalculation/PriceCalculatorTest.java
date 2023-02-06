package ru.yandex.direct.core.entity.campaign.service.pricerecalculation;

import java.time.LocalDate;
import java.util.List;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PriceMarkup;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@Description("Проверка пересчёта ставок при смене даты cpm-price кампаний")
public class PriceCalculatorTest {
    private static LocalDate month9 = LocalDate.of(2021,9,1);
    private static LocalDate month10 = LocalDate.of(2021,10,1);
    private static LocalDate month11 = LocalDate.of(2021,11,1);
    private static LocalDate month12 = LocalDate.of(2021,12,1);

    @Test
    public void chooseMinPercentTest() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(List.of(
                new PriceMarkup().withDateStart(month10)
                        .withDateEnd(month11)
                        .withPercent(10)));
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,9,15))
                .withEndDate(LocalDate.of(2021,10,3));
        checkResultPercent(campaign, pricePackage, 0);
    }

    @Test
    public void needRejectCampaignApproveTest1() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(List.of(
                new PriceMarkup().withDateStart(month10)
                        .withDateEnd(month11)
                        .withPercent(10)));
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,9,15))
                .withEndDate(LocalDate.of(2021,10,20));
        checkNull(campaign, pricePackage);
    }

    @Test
    public void needRejectCampaignApproveTest2() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(List.of(
                new PriceMarkup().withDateStart(month9)
                        .withDateEnd(month10.minusDays(1))
                        .withPercent(40),
                new PriceMarkup().withDateStart(month10)
                        .withDateEnd(month12)
                        .withPercent(10)));
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,9,15))
                .withEndDate(LocalDate.of(2021,10,20));
        checkNull(campaign, pricePackage);
    }

    @Test
    public void notNeedRejectCampaignApproveTest() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(List.of(
                new PriceMarkup().withDateStart(month9)
                        .withDateEnd(month10.minusDays(1))
                        .withPercent(40),
                new PriceMarkup().withDateStart(month10)
                        .withDateEnd(month11)
                        .withPercent(15)));
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,9,28))
                .withEndDate(LocalDate.of(2021,10,20));
        checkResultPercent(campaign, pricePackage, 15);
    }

    @Test
    public void checkManyShortSeasonPeriodsWithPercent() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(getManyShortSeasonsForPackage());
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,9,28))
                .withEndDate(LocalDate.of(2021,10,1));
        checkResultPercent(campaign, pricePackage, 0);
    }

    @Test
    public void checkManyShortSeasonPeriodsWithReject() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(getManyShortSeasonsForPackage());
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,9,28))
                .withEndDate(LocalDate.of(2021,10,15));
        checkNull(campaign, pricePackage);
    }

    @Test
    public void checkManyShortSeasonPeriodsWithExactDates() {
        PricePackage pricePackage = new PricePackage().withPriceMarkups(getManyShortSeasonsForPackage());
        CpmPriceCampaign campaign = new CpmPriceCampaign()
                .withStartDate(LocalDate.of(2021,10,3))
                .withEndDate(LocalDate.of(2021,10,8));
        checkResultPercent(campaign, pricePackage, 20);
    }

    private void checkNull(CpmPriceCampaign campaign, PricePackage pricePackage) {
        PriceMarkup priceMarkup = PriceCalculator.getSeasonalPriceRatio(campaign, pricePackage);
        assertThat(priceMarkup).isNull();
    }

    private void checkResultPercent(CpmPriceCampaign campaign, PricePackage pricePackage, Integer expectedPercent) {
        PriceMarkup priceMarkup = PriceCalculator.getSeasonalPriceRatio(campaign, pricePackage);
        assertThat(priceMarkup).isNotNull();
        assertThat(priceMarkup.getPercent()).isEqualTo(expectedPercent);
    }

    private List<PriceMarkup> getManyShortSeasonsForPackage() {
        return  List.of(
                new PriceMarkup().withDateStart(LocalDate.of(2021,9,10))
                        .withDateEnd(LocalDate.of(2021,9,15))
                        .withPercent(10),
                new PriceMarkup().withDateStart(LocalDate.of(2021,9,20))
                        .withDateEnd(LocalDate.of(2021,9,30))
                        .withPercent(15),
                new PriceMarkup().withDateStart(LocalDate.of(2021,10,3))
                        .withDateEnd(LocalDate.of(2021,10,8))
                        .withPercent(20),
                new PriceMarkup().withDateStart(LocalDate.of(2021,10,10))
                        .withDateEnd(LocalDate.of(2021,10,15))
                        .withPercent(25)
        );
    }
}
