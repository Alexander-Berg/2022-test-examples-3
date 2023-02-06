package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.adgroup.controller.CpmAdGroupController;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingGoal;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingRule;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmYndxFrontpageAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmYndxFrontpageAutoStrategyPriceTest {
    private static final BigDecimal MOSCOW_REGION_MOBILE_MIN_PRICE = BigDecimal.valueOf(1.5);

    @Autowired
    private Steps steps;
    @Autowired
    private CpmAdGroupController controller;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private RetargetingRepository retargetingRepository;
    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;
    @Autowired
    private DirectWebAuthenticationSource authenticationSource;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.CHF));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, true);
        setAuthData(clientInfo);
    }

    private void setAuthData(ClientInfo clientInfo) {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid()));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        UserInfo userInfo = clientInfo.getChiefUserInfo();
        User user = userInfo.getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }

    @Test
    public void addAdGroupsWithoutPriceInAutobudgetCampaign() {

        //Создаём группу с геотаргетингом на Россию и ретаргетинг на ней
        AdGroupInfo adGroupForCopy = createAdGroupForCopy(singletonList(RUSSIA_REGION_ID),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));
        WebCpmAdGroupRetargeting retargetingForCopy = createRetargeting(adGroupForCopy);

        //Скопируем группу, заменив в ней гео на Москву и проверим, что успешно сохранилась
        WebCpmAdGroup complexCpmAdGroup =
                randomNameWebCpmYndxFrontpageAdGroup(adGroupForCopy.getAdGroupId(), adGroupForCopy.getCampaignId())
                        .withGeo(String.valueOf(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID))
                        .withGeneralPrice(null)
                        .withRetargetings(singletonList(retargetingForCopy));
        WebResponse webResponse = controller.saveCpmAdGroup(
                singletonList(complexCpmAdGroup),
                adGroupForCopy.getCampaignId(), true, false, true, null);
        assertThat("запрос должен завершиться успешно", webResponse.isSuccessful(), is(true));

        //Проверяем, что на кампании в базе две группы объявлений, вычисляем идентификатор скопированной
        List<Long> adGroupIds = adGroupRepository.getAdGroupIdsBySelectionCriteria(clientInfo.getShard(),
                new AdGroupsSelectionCriteria().withCampaignIds(adGroupForCopy.getCampaignId()),
                LimitOffset.maxLimited());
        assertThat("количество групп не соответствует ожидаемому", adGroupIds, IsCollectionWithSize.hasSize(2));
        Long copiedAdGroupId = adGroupIds.get(0).equals(adGroupForCopy.getAdGroupId()) ?
                adGroupIds.get(1) : adGroupIds.get(0);

        //Проверяем, что в базе проставилась ставка для показа в москве
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(MOSCOW_REGION_MOBILE_MIN_PRICE);
        checkRetargetings(expectedRetargeting, copiedAdGroupId, clientInfo.getShard());
    }

    private AdGroupInfo createAdGroupForCopy(List<Long> geo, List<FrontpageCampaignShowType> campaignShowTypes) {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                clientInfo.getShard(), campaignInfo.getCampaignId(), campaignShowTypes);
        return steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(campaignInfo.getCampaignId())
                        .withGeo(geo))
                .withCampaignInfo(campaignInfo));
    }

    private WebCpmAdGroupRetargeting createRetargeting(AdGroupInfo adGroupInfo) {
        RetConditionInfo retargetingCondition =
                steps.retConditionSteps().createCpmRetCondition(adGroupInfo.getClientInfo());
        RetargetingInfo retargeting = steps.retargetingSteps().createRetargeting(defaultRetargeting()
                .withRetargetingConditionId(retargetingCondition.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(0.01)), adGroupInfo);

        return new WebCpmAdGroupRetargeting()
                .withId(retargeting.getRetargetingId())
                .withRetargetingConditionId(retargetingCondition.getRetConditionId())
                .withPriceContext(retargeting.getRetargeting().getPriceContext().doubleValue())
                .withName(retargetingCondition.getRetCondition().getName())
                .withDescription(retargetingCondition.getRetCondition().getDescription())
                .withConditionType(retargetingCondition.getRetCondition().getType())
                .withGroups(mapList(retargetingCondition.getRetCondition().getRules(), this::ruleFromCore));
    }

    private WebRetargetingRule ruleFromCore(Rule rule) {
        return new WebRetargetingRule()
                .withInterestType(CryptaInterestTypeWeb.fromCoreType(rule.getInterestType()))
                .withRuleType(rule.getType())
                .withGoals(mapList(rule.getGoals(), this::goalFromCore));
    }

    private WebRetargetingGoal goalFromCore(Goal goal) {
        return new WebRetargetingGoal()
                .withId(goal.getId())
                .withGoalType(goal.getType())
                .withTime(goal.getTime());
    }

    private void checkRetargetings(Retargeting expected, Long adGroupId, int shard) {
        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroupId));

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyFields(newPath("priceContext"))
                .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());
        assertThat("количество ретаргетингов должно быть равно единице", retargetings,
                IsCollectionWithSize.hasSize(1));
        assertThat("цена у ретаргетинга должна быть равна ожидаемой", retargetings.get(0),
                beanDiffer(expected).useCompareStrategy(strategy));
    }
}
