package ru.yandex.direct.core.entity.campaign.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandSafety;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultBrandSafetyGoal;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultBrandSafetyRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultBrandSafetyRules;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithBrandSafetyServiceTest {
    @org.junit.Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @org.junit.Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignWithBrandSafetyService campaignWithBrandSafetyService;
    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    private ClientId clientId;

    private Long campaignId;

    private int shard;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void saveCategories_categoryListIsNotExisting_added() {
        List<Long> categories = asList(4294967296L, 4294967297L, 4294967298L);
        CampaignWithBrandSafety campaign =
                createBrandSafetyCategoriesListByCampaignType(campaignId, categories, campaignType);
        Long retCondId = campaignWithBrandSafetyService.saveCategoriesAndGetRetCondIds(shard, clientId,
                singletonList(campaign)).get(campaignId);

        List<RetargetingCondition> actualRetConditions = retargetingConditionRepository
                .getBrandSafetyRetConditionsByClient(shard, clientId);
        assertThat(actualRetConditions, hasSize(1));

        Long actualRetCondId = actualRetConditions.get(0).getId();
        assertEquals(retCondId, actualRetCondId);

        List<Long> actualCategories = mapList(actualRetConditions.get(0).collectGoals(), Goal::getId);
        assertEquals(categories, actualCategories);
    }

    @Test
    public void saveCategories_categoryListIsExisting_rebinded() {
        List<Long> categories = asList(4294967299L, 4294967300L, 4294967301L);
        CampaignWithBrandSafety campaign =
                createBrandSafetyCategoriesListByCampaignType(campaignId, categories, campaignType);
        Long expectedRetCondId = campaignWithBrandSafetyService
                .saveCategoriesAndGetRetCondIds(shard, clientId, singletonList(campaign)).get(campaignId);

        Long secondCampaignId = steps.campaignSteps().createActiveCampaign(clientInfo).getCampaignId();
        CampaignWithBrandSafety secondCampaign =
                createBrandSafetyCategoriesListByCampaignType(secondCampaignId, categories, campaignType);
        Long actualRetCondId = campaignWithBrandSafetyService
                .saveCategoriesAndGetRetCondIds(shard, clientId, singletonList(secondCampaign)).get(secondCampaignId);
        assertEquals(expectedRetCondId, actualRetCondId);

        List<RetargetingCondition> actualRetConditions =
                retargetingConditionRepository.getBrandSafetyRetConditionsByClient(shard, clientId);
        assertThat(actualRetConditions, hasSize(1));
    }

    @Test
    public void saveCategories_categoryListIsExistingForAnotherClient_notRebinded() {
        List<Long> categories = asList(4294967302L, 4294967303L, 4294967304L);
        CampaignWithBrandSafety campaign =
                createBrandSafetyCategoriesListByCampaignType(campaignId, categories, campaignType);
        Long firstRetCondId = campaignWithBrandSafetyService
                .saveCategoriesAndGetRetCondIds(shard, clientId, singletonList(campaign)).get(campaignId);

        ClientInfo secondClientInfo = steps.clientSteps().createDefaultClient();
        int secondShard = secondClientInfo.getShard();
        ClientId secondClientId = secondClientInfo.getClientId();
        Long secondCampaignId = steps.campaignSteps().createActiveCampaign(secondClientInfo).getCampaignId();

        CampaignWithBrandSafety secondCampaign =
                createBrandSafetyCategoriesListByCampaignType(secondCampaignId, categories, campaignType);
        Long secondRetCondId = campaignWithBrandSafetyService
                .saveCategoriesAndGetRetCondIds(secondShard, secondClientId, singletonList(secondCampaign)).get(secondCampaignId);
        assertNotEquals(firstRetCondId, secondRetCondId);

        List<Long> secondCategories = mapList(retargetingConditionRepository
                .getBrandSafetyRetConditionsByClient(secondShard, secondClientId).get(0).collectGoals(), Goal::getId);
        assertEquals(categories, secondCategories);
    }

    @Test
    public void saveCategories_categoryListIsExistingButDeleted_notRebinded() {
        List<Long> categories = asList(4294967305L, 4294967306L, 4294967307L);
        CampaignWithBrandSafety campaign =
                createBrandSafetyCategoriesListByCampaignType(campaignId, categories, campaignType);
        RetargetingCondition deletedRetargetingCondition = createDeletedRetargetingCondition(clientId, campaign);
        Long deletedRetCondId =
                retargetingConditionRepository.add(shard, singletonList(deletedRetargetingCondition)).get(0);

        Long secondCampaignId = steps.campaignSteps().createActiveCampaign(clientInfo).getCampaignId();
        CampaignWithBrandSafety secondCampaign =
                createBrandSafetyCategoriesListByCampaignType(secondCampaignId, categories, campaignType);
        Long actualRetCondId = campaignWithBrandSafetyService
                .saveCategoriesAndGetRetCondIds(shard, clientId, singletonList(secondCampaign)).get(secondCampaignId);
        assertNotEquals(deletedRetCondId, actualRetCondId);

        List<Long> actualCategories = mapList(retargetingConditionRepository
                .getBrandSafetyRetConditionsByClient(shard, clientId).get(0).collectGoals(), Goal::getId);
        assertEquals(categories, actualCategories);
    }

    @Test
    public void saveCategories_categoryListIsEmpty_unbinded() {
        CampaignWithBrandSafety expectedWithoutCategories =
                createBrandSafetyCategoriesListByCampaignType(campaignId, emptyList(), campaignType);
        Long actualRetCondId = campaignWithBrandSafetyService
                .saveCategoriesAndGetRetCondIds(shard, clientId, singletonList(expectedWithoutCategories)).get(campaignId);
        assertNull(actualRetCondId);

        List<RetargetingCondition> actualRetConditions =
                retargetingConditionRepository.getBrandSafetyRetConditionsByClient(shard, clientId);
        assertThat(actualRetConditions, hasSize(0));
    }

    private static CampaignWithBrandSafety createBrandSafetyCategoriesListByCampaignType(Long campaignId,
                                                                                         List<Long> categories,
                                                                                         CampaignType campaignType) {
        return ((CampaignWithBrandSafety) TestCampaigns.newCampaignByCampaignType(campaignType))
                .withId(campaignId)
                .withBrandSafetyCategories(categories);
    }

    private static RetargetingCondition createDeletedRetargetingCondition(ClientId clientId,
                                                                          CampaignWithBrandSafety campaignWithBrandSafety) {
        List<Goal> goals = mapList(campaignWithBrandSafety.getBrandSafetyCategories(),
                categoryId -> (Goal) defaultBrandSafetyGoal().withId(categoryId));
        Rule rule = defaultBrandSafetyRules().get(0).withGoals(goals);
        return (RetargetingCondition) defaultBrandSafetyRetCondition(clientId)
                .withRules(singletonList(rule))
                .withDeleted(true);
    }
}
