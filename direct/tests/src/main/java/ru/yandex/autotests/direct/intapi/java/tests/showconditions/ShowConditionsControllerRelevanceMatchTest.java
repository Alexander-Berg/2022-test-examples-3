package ru.yandex.autotests.direct.intapi.java.tests.showconditions;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsBaseRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.java.steps.ShowConditionsControllerSteps;
import ru.yandex.autotests.direct.intapi.models.RelevanceMatchAddItem;
import ru.yandex.autotests.direct.intapi.models.RelevanceMatchItem;
import ru.yandex.autotests.direct.intapi.models.RelevanceMatchModificationContainer;
import ru.yandex.autotests.direct.intapi.models.ShowConditionsRequest;
import ru.yandex.autotests.direct.intapi.models.ShowConditionsResponse;
import ru.yandex.autotests.direct.utils.features.FeatureNameEnum;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.apiclient.config.Semaphore;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка работы ShowConditionsController update: relevance match")
@Stories(TestFeatures.ShowConditions.RELEVANCE_MATCH)
@Features(TestFeatures.SHOW_CONDITIONS)
@Tag(Tags.SHOW_CONDITIONS)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-77018")
public class ShowConditionsControllerRelevanceMatchTest {

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

    private Long adGroupId;
    private Long campaignId;
    private Long relevanceMatchId;

    @Before
    public void before() {
        campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        adGroupId = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        int shard = api.userSteps.getDBSteps().shardingSteps().getShardByCid(campaignId);

        relevanceMatchId = api.userSteps.getDirectJooqDbSteps().useShard(shard).bidsBaseSteps()
                .saveDefaultRelevanceMatch(campaignId, adGroupId, Currency.RUB);
        assumeThat("ожидается валидный идентификатор", relevanceMatchId, greaterThan(0L));

        CampaignsRecord campaignsRecord =
                directClassRule.dbSteps().useShardForLogin(LOGIN).campaignsSteps().getCampaignById(campaignId);
        clientId = campaignsRecord.getClientid();
        operatorUid = campaignsRecord.getUid();

        //До открытия расширенного автотаргетинга на всех делаем проверки с выключенной фичей,
        //при открытии нужно поправить тесты с учетом необходимости задания priceContext
        disableFeatureForClient(FeatureNameEnum.CONTEXT_RELEVANCE_MATCH, clientId, shard);
        disableFeatureForClient(FeatureNameEnum.CONTEXT_RELEVANCE_MATCH_INTERFACE_ONLY, clientId, shard);

        showConditionsControllerSteps = directClassRule.intapiSteps().showConditionsControllerSteps();
    }

    private void disableFeatureForClient(FeatureNameEnum feature, long clientId, int shard) {

        long featureId = api.userSteps.getDirectJooqDbSteps().useShard(shard).featuresSteps()
                .getFeatureId(feature.getValue());
        api.userSteps.getDirectJooqDbSteps().useShard(shard).clientsSteps()
                .setClientFeature(clientId, featureId, false);
    }

    @Test
    public void update_AddNewRelevanceMatch_SuccessAdd() {
        Long newAdGroupId = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        assumeThat("группа должна быть создана", newAdGroupId, notNullValue());

        ShowConditionsRequest request = createAddRequest(newAdGroupId, BigDecimal.TEN);
        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(true));

        BidsBaseRecord record = api.userSteps.getDirectJooqDbSteps().bidsBaseSteps().getBidsBaseByPid(adGroupId);
        assertThat(record, notNullValue());
    }

    @Test
    public void update_AddAfterDeleteRelevanceMatch_SuccessAdd() {
        ShowConditionsRequest request = createDeleteRequest(adGroupId, relevanceMatchId);
        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assumeThat("бесфразный таргетинг удалён", response.getSuccess(), is(true));

        BigDecimal newPrice = BigDecimal.valueOf(44.4);
        request = createAddRequest(adGroupId, newPrice);
        response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(true));

        BidsBaseRecord record = api.userSteps.getDirectJooqDbSteps().bidsBaseSteps().getBidsBaseByPid(adGroupId);
        assertThat(record.getPrice(), comparesEqualTo(newPrice));
    }

    @Test
    public void update_AddExistsRelevanceMatch_ItemNotUpdated() {
        BidsBaseRecord record = api.userSteps.getDirectJooqDbSteps().bidsBaseSteps().getBidsBaseByPid(adGroupId);
        BigDecimal oldPrice = record.getPrice();

        ShowConditionsRequest request = createAddRequest(adGroupId, BigDecimal.valueOf(77.7));
        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(true));

        record = api.userSteps.getDirectJooqDbSteps().bidsBaseSteps().getBidsBaseByPid(adGroupId);
        assertThat(record.getPrice(), comparesEqualTo(oldPrice));
    }

    @Test
    public void update_ChangePriceAndSuspend_SuccessEdit() {
        BigDecimal newPrice = BigDecimal.valueOf(33.3);
        ShowConditionsRequest request = new ShowConditionsRequest()
                .withRelevanceMatches(singletonMap(
                        adGroupId.toString(),
                        new RelevanceMatchModificationContainer().withEdited(
                                singletonMap(relevanceMatchId.toString(),
                                        new RelevanceMatchItem()
                                                .withIsSuspended(1)
                                                .withPrice(newPrice)))));

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(true));

        BidsBaseRecord record = api.userSteps.getDirectJooqDbSteps().bidsBaseSteps().getBidsBaseByPid(adGroupId);
        assertThat(record.getPrice(), comparesEqualTo(newPrice));
        assertThat(record.getOpts(), containsString("suspended"));
    }

    @Test
    public void update_EditAfterDelete_FailedRequest() {
        ShowConditionsRequest request = createDeleteRequest(adGroupId, relevanceMatchId);
        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assumeThat("бесфразный таргетинг удалён", response.getSuccess(), is(true));

        request = new ShowConditionsRequest()
                .withRelevanceMatches(singletonMap(
                        adGroupId.toString(),
                        new RelevanceMatchModificationContainer().withEdited(
                                singletonMap(relevanceMatchId.toString(),
                                        new RelevanceMatchItem().withPrice(BigDecimal.TEN)))));

        response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(false));
    }

    @Test
    public void update_DeleteRelevanceMatch_ExistRelevanceMatchId_SuccessDelete() {
        ShowConditionsRequest request = createDeleteRequest(adGroupId, relevanceMatchId);

        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(true));

        BidsBaseRecord record = api.userSteps.getDirectJooqDbSteps().bidsBaseSteps().getBidsBaseByPid(adGroupId);
        assertThat(record.getOpts(), containsString("deleted"));
    }


    @Test
    public void update_DeleteRelevanceMatch_NotExistRelevanceMatchId_SuccessDelete() {
        ShowConditionsRequest request = createDeleteRequest(adGroupId, relevanceMatchId);
        ShowConditionsResponse response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assumeThat("ожидается успешное удаление бесфразного таргета", response.getSuccess(), is(true));

        response = showConditionsControllerSteps.update(operatorUid, clientId, request);
        assertThat(response.getSuccess(), is(false));
    }

    private ShowConditionsRequest createAddRequest(long adGroupId, BigDecimal price) {
        return new ShowConditionsRequest()
                .withRelevanceMatches(singletonMap(
                        String.valueOf(adGroupId),
                        new RelevanceMatchModificationContainer().withAdded(singletonList(
                                new RelevanceMatchAddItem().withPrice(price)))));
    }

    private ShowConditionsRequest createDeleteRequest(long adGroupId, long relevanceMatchId) {
        return new ShowConditionsRequest()
                .withRelevanceMatches(singletonMap(
                        String.valueOf(adGroupId),
                        new RelevanceMatchModificationContainer().withDeleted(singletonList(relevanceMatchId))));
    }
}
