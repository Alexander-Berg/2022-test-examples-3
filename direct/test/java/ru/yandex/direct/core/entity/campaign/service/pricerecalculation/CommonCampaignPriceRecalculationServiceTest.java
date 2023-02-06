package ru.yandex.direct.core.entity.campaign.service.pricerecalculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignStrategyService;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.contextAverageClickStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageClickStrategy;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка сохранения стратегии")
public class CommonCampaignPriceRecalculationServiceTest {
    @Autowired
    public Steps steps;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CampaignStrategyService campaignStrategyService;
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignRepository campaignRepository;

    private CampaignInfo textCampaignInfo;

    @Before
    public void before() {
        textCampaignInfo = steps.campaignSteps().createActiveTextCampaign();
        TextCampaign newTextCampaign = new TextCampaign()
                .withId(textCampaignInfo.getCampaignId())
                .withName("newName");
        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(newTextCampaign, TextCampaign.NAME,
                newTextCampaign.getName());
        textCampaignModelChanges.process(LocalDate.now().plusDays(1), TextCampaign.START_DATE);
        textCampaignModelChanges.process(LocalDate.now().plusDays(7), TextCampaign.END_DATE);
        textCampaignModelChanges.process(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru", TextCampaign.EMAIL);
        AppliedChanges<TextCampaign> textCampaignAppliedChanges = textCampaignModelChanges.applyTo(newTextCampaign);
        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                textCampaignInfo.getShard(),
                textCampaignInfo.getUid(),
                textCampaignInfo.getClientId(),
                textCampaignInfo.getUid(),
                textCampaignInfo.getUid());
        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(textCampaignAppliedChanges));
    }

    @Test
    public void campSetStrategy() {
        DbStrategy strategy = defaultAverageClickStrategy();
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getCampaignId(),
                strategy, textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        System.out.println(result.getValidationResult().flattenErrors());
        Assertions.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        Collections.singletonList(textCampaignInfo.getCampaignId()));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        TextCampaign actualCampaign = textCampaigns.get(0);
        assertThat(actualCampaign).is(matchedBy(beanDiffer(getExpectedCampaign())
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    public TextCampaign getExpectedCampaign() {
        return new TextCampaign()
                .withId(textCampaignInfo.getCampaignId())
                .withClientId(textCampaignInfo.getClientId().asLong())
                .withType(CampaignType.TEXT)
                .withName(textCampaignInfo.getCampaign().getName())
                .withHasTitleSubstitution(true)
                .withStartDate(LocalDate.now().plusDays(1))
                .withEndDate(LocalDate.now().plusDays(7))
                .withStrategy(defaultAverageClickStrategy());
    }

    @Test
    public void calcPercentile() {
        List<BigDecimal> values = List.of(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
        BigDecimal result = CommonCampaignPriceRecalculationService.calcPercentile(values, new BigDecimal("0.5"));
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void calcPercentile_OkWithNull() {
        List<BigDecimal> values = Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, null);
        BigDecimal result = CommonCampaignPriceRecalculationService.calcPercentile(values, new BigDecimal("0.5"));
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testChangeBroadMatchFlag() {
        //При смене статегии на стратегию в сетях при флаге ДРФ мы выключаем флаг
        Campaign camp = campaignRepository.getCampaigns(textCampaignInfo.getShard(),
                List.of(textCampaignInfo.getCampaignId())).get(0);
        ModelChanges<Campaign> modelChanges = new ModelChanges<>(textCampaignInfo.getCampaignId(), Campaign.class)
                .process(true, Campaign.BROAD_MATCH_FLAG);
        campaignRepository.updateCampaigns(textCampaignInfo.getShard(), List.of(modelChanges.applyTo(camp)));
        assertThat(campaignRepository.getCampaigns(textCampaignInfo.getShard(),
                List.of(textCampaignInfo.getCampaignId())).get(0).getBroadMatchFlag()).isTrue();

        campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getCampaignId(),
                contextAverageClickStrategy(), textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        Campaign actualCampaign = campaignRepository.getCampaigns(textCampaignInfo.getShard(),
                List.of(textCampaignInfo.getCampaignId())).get(0);
        assertThat(actualCampaign.getBroadMatchFlag()).isFalse();
    }
}
