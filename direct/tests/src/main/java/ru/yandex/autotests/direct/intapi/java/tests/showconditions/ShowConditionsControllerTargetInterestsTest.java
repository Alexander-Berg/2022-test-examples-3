package ru.yandex.autotests.direct.intapi.java.tests.showconditions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.java.steps.ShowConditionsControllerSteps;
import ru.yandex.autotests.direct.intapi.models.RetargetingItem;
import ru.yandex.autotests.direct.intapi.models.RetargetingModificationContainer;
import ru.yandex.autotests.direct.intapi.models.ShowConditionsRequest;
import ru.yandex.autotests.direct.intapi.models.ShowConditionsResponse;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.apiclient.config.Semaphore;
import ru.yandex.autotests.directapi.model.api5.audiencetargets.AddRequestMap;
import ru.yandex.autotests.directapi.model.api5.audiencetargets.AudienceTargetAddItemMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;

@Aqua.Test
@Description("Проверка работы ShowConditionsController update: target interests")
@Stories(TestFeatures.ShowConditions.TARGET_INTERESTS)
@Features(TestFeatures.SHOW_CONDITIONS)
@Tag(Tags.SHOW_CONDITIONS)
@Tag(TagDictionary.TRUNK)
public class ShowConditionsControllerTargetInterestsTest {

    private String LOGIN = LOGIN_MAIN;

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static ShowConditionsControllerSteps showConditionsControllerSteps;

    private Long clientId;
    private Long operatorUid;

    private String adGroupIdVal;
    private Long targetInterestsId;

    @Before
    public void before() {
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long groupId = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        adGroupIdVal = String.valueOf(groupId);
        Long userListsId = api.userSteps.retargetingListsSteps().addDefaultRetargetingLists(LOGIN);

        targetInterestsId = api.userSteps.audienceTargetsSteps().add(new AddRequestMap()
                .withAudienceTargets(new AudienceTargetAddItemMap()
                        .withAdGroupId(groupId)
                        .withRetargetingListId(userListsId)
                        .withContextBid(MoneyCurrency.get(Currency.RUB).getLongMinPrice().longValue())))
                .get(0);

        CampaignsRecord campaignsRecord =
                directClassRule.dbSteps().useShardForLogin(LOGIN).campaignsSteps().getCampaignById(campaignId);
        clientId = campaignsRecord.getClientid();
        operatorUid = campaignsRecord.getUid();

        showConditionsControllerSteps = directClassRule.intapiSteps().showConditionsControllerSteps();
    }

    @Test
    public void update_DeleteTargetInterests_DeleteNonExistsRetargeting_MessageErrors() {
        RetargetingModificationContainer container = new RetargetingModificationContainer()
                .withDeleted(asList(666L));

        ShowConditionsRequest request =
                new ShowConditionsRequest().withTargetInterests(singletonMap(adGroupIdVal, container));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);

        List<String> errors = response.getItems().get(adGroupIdVal).getErrors();
        assertThat(errors, hasSize(1));
        assertThat(response.getItems().get(adGroupIdVal).getRetargetings().getSuccessDeleted(), empty());
    }

    @Test
    public void update_DeleteTargetInterests_ExistRetargetingId_SuccessDelete() {
        RetargetingModificationContainer container = new RetargetingModificationContainer()
                .withDeleted(asList(targetInterestsId));

        ShowConditionsRequest request =
                new ShowConditionsRequest().withTargetInterests(singletonMap(adGroupIdVal, container));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);

        assertThat(response.getItems().get(adGroupIdVal).getErrors(), empty());
        assertThat(response.getItems().get(adGroupIdVal).getTargetInterests().getSuccessDeleted(),
                contains(targetInterestsId));
    }

    @Test
    public void update_DeleteTargetInterests_OneExistRetargetingIdAndOneNotExists_ErrorPartlyDelete() {
        RetargetingModificationContainer container = new RetargetingModificationContainer()
                .withDeleted(asList(targetInterestsId, 1L));

        ShowConditionsRequest request =
                new ShowConditionsRequest().withTargetInterests(singletonMap(adGroupIdVal, container));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);

        List<String> errors = response.getItems().get(adGroupIdVal).getErrors();
        assertThat(errors, hasSize(1));
        assertThat(response.getItems().get(adGroupIdVal).getTargetInterests().getSuccessDeleted(), empty());
    }

    @Test
    public void update_EditRetargetings_UpdateAllParams_SuccessEdit() {
        RetargetingModificationContainer container = new RetargetingModificationContainer()
                .withEdited(singletonMap(String.valueOf(targetInterestsId),
                        new RetargetingItem().withIsSuspended(1)
                                .withAutobudgetPriority(3)
                                .withPriceContext(BigDecimal.TEN)));

        ShowConditionsRequest request =
                new ShowConditionsRequest().withTargetInterests(singletonMap(adGroupIdVal, container));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);

        assertThat(response.getItems().get(adGroupIdVal).getErrors(), empty());
        assertThat(response.getItems().get(adGroupIdVal).getTargetInterests().getSuccessEdited(),
                contains(targetInterestsId));
    }

    @Test
    public void update_EditRetargetings_UpdateOnlySuspend_SuccessEdit() {
        RetargetingModificationContainer container = new RetargetingModificationContainer()
                .withEdited(singletonMap(String.valueOf(targetInterestsId),
                        new RetargetingItem().withIsSuspended(1)));

        ShowConditionsRequest request =
                new ShowConditionsRequest().withTargetInterests(singletonMap(adGroupIdVal, container));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);

        assertThat(response.getItems().get(adGroupIdVal).getErrors(), empty());
        assertThat(response.getItems().get(adGroupIdVal).getTargetInterests().getSuccessEdited(),
                contains(targetInterestsId));
    }

    @Test
    public void update_EmptyRequest_SuccessResponseWithEmptyItems() {
        RetargetingModificationContainer container = new RetargetingModificationContainer();

        ShowConditionsRequest request =
                new ShowConditionsRequest().withTargetInterests(singletonMap(adGroupIdVal, container));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);

        assertTrue(response.getSuccess());
        assertThat(response.getItems().entrySet(), empty());
    }
}
