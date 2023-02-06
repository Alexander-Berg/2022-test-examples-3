package ru.yandex.direct.core.entity.campaign.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.WhenMoneyOnCampaignWas;
import ru.yandex.direct.core.entity.campaign.repository.WhenMoneyOnCampWasRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


/**
 * Тест-план:
 * - MONEY_IN (одна, несколько кампаний)
 * - открытый интервал
 * - закрытый интервал
 * - нет записей по кампании
 * - открытый интервал, с кривым переходом на летнее время
 * - MONEY_OUT (одна|несколько кампаний)
 * - открытый интервал
 * - закрытый интервал
 * - открытый интервал, с кривым переходом на летнее время
 */

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignServiceWhenMoneyOnCampWasTest {

    private static final Duration DELTA = Duration.ofSeconds(10);
    private static final LocalDateTime END_OF_TIME = LocalDateTime.parse("2038-01-19T00:00:00");

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Autowired
    private Steps steps;

    @Autowired
    private WhenMoneyOnCampWasRepository whenMoneyOnCampWasRepository;

    @Autowired
    private CampaignService campaignService;

    private CampaignInfo campaignInfo;
    private long cid;

    @Before
    public void init() {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        cid = campaignInfo.getCampaignId();
    }

    public WhenMoneyOnCampaignWas getExpectedRecord(WhenMoneyOnCampWasEvents event) {
        return new WhenMoneyOnCampaignWas()
                .withIntervalStart(LocalDateTime.now())
                .withIntervalEnd(event == WhenMoneyOnCampWasEvents.MONEY_IN ? END_OF_TIME : LocalDateTime.now());
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyInOnEmptyTable() {
        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_IN);

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(0);

        WhenMoneyOnCampaignWas expectedRecord = getExpectedRecord(WhenMoneyOnCampWasEvents.MONEY_IN);

        softly.assertThat(Duration.between(expectedRecord.getIntervalStart(), actualRecord.getIntervalStart()).abs())
                .isLessThanOrEqualTo(DELTA);
        softly.assertThat(actualRecord.getIntervalEnd()).isEqualTo(END_OF_TIME);
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyInOnOpenInterval() {
        LocalDateTime theDateOfFirstMoneyIn = LocalDateTime.now().minusDays(1);
        whenMoneyOnCampWasRepository.addInterval(campaignInfo.getShard(), cid, theDateOfFirstMoneyIn, END_OF_TIME);

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas expectedRecord = recordList.get(0);

        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_IN);

        recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(0);

        assertThat("проверяем, что запись соответсвует ожиданиям (не изменилась)",
                actualRecord, beanDiffer(expectedRecord));
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyInOnOpenIntervalWithSummertime() {
        LocalDateTime theDateOfFirstMoneyIn = LocalDateTime.now().minusDays(1);
        whenMoneyOnCampWasRepository
                .addInterval(campaignInfo.getShard(), cid, theDateOfFirstMoneyIn, END_OF_TIME.minusHours(1));

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas expectedRecord = recordList.get(0);

        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_IN);

        recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(0);

        assertThat("проверяем, что запись соответсвует ожиданиям (не изменилась)",
                actualRecord, beanDiffer(expectedRecord));
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyInOnClosedInterval() {
        LocalDateTime theDateOfFirstMoneyIn = LocalDateTime.now().minusDays(2);
        whenMoneyOnCampWasRepository
                .addInterval(campaignInfo.getShard(), cid, theDateOfFirstMoneyIn, LocalDateTime.now().minusDays(1));

        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_IN);

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 2", recordList, iterableWithSize(2));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(1);

        WhenMoneyOnCampaignWas expectedRecord = getExpectedRecord(WhenMoneyOnCampWasEvents.MONEY_IN);

        softly.assertThat(Duration.between(expectedRecord.getIntervalStart(), actualRecord.getIntervalStart()).abs())
                .isLessThanOrEqualTo(DELTA);
        softly.assertThat(actualRecord.getIntervalEnd()).isEqualTo(END_OF_TIME);
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyOutOnOpenInterval() {
        LocalDateTime theDateOfFirstMoneyIn = LocalDateTime.now().minusDays(1);
        whenMoneyOnCampWasRepository.addInterval(campaignInfo.getShard(), cid, theDateOfFirstMoneyIn, END_OF_TIME);

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas expectedRecord = recordList.get(0);
        expectedRecord.setIntervalEnd(LocalDateTime.now());

        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_OUT);

        recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(0);

        softly.assertThat(actualRecord.getIntervalStart()).isEqualTo(expectedRecord.getIntervalStart());
        softly.assertThat(Duration.between(expectedRecord.getIntervalEnd(), actualRecord.getIntervalEnd()).abs())
                .isLessThanOrEqualTo(DELTA);
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyOutOnClosedInterval() {
        LocalDateTime theDateOfFirstMoneyIn = LocalDateTime.now().minusDays(2);
        whenMoneyOnCampWasRepository
                .addInterval(campaignInfo.getShard(), cid, theDateOfFirstMoneyIn, LocalDateTime.now().minusDays(1));

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas expectedRecord = recordList.get(0);

        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_OUT);

        recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(0);

        assertThat("проверяем, что после события MONEY_OUT запись не изменилась",
                actualRecord, beanDiffer(expectedRecord));
    }

    @Test
    public void testWhenMoneyOnCampWas_MoneyOutOnOpenIntervalWithSummertime() {
        LocalDateTime theDateOfFirstMoneyIn = LocalDateTime.now().minusDays(1);
        whenMoneyOnCampWasRepository
                .addInterval(campaignInfo.getShard(), cid, theDateOfFirstMoneyIn, END_OF_TIME.minusHours(1));

        List<WhenMoneyOnCampaignWas> recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas expectedRecord = recordList.get(0);
        expectedRecord.setIntervalEnd(LocalDateTime.now());

        campaignService.whenMoneyOnCampWas(cid, WhenMoneyOnCampWasEvents.MONEY_OUT);

        recordList =
                whenMoneyOnCampWasRepository.getWhenMoneyOnCampaignsWas(campaignInfo.getShard(), singletonList(cid));
        assumeThat("предполагаем, что кол-во записей равно 1", recordList, iterableWithSize(1));
        WhenMoneyOnCampaignWas actualRecord = recordList.get(0);

        softly.assertThat(actualRecord.getIntervalStart()).isEqualTo(expectedRecord.getIntervalStart());
        softly.assertThat(Duration.between(expectedRecord.getIntervalEnd(), actualRecord.getIntervalEnd()).abs())
                .isLessThanOrEqualTo(DELTA);
    }

}
