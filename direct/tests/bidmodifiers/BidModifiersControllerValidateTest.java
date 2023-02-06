package ru.yandex.autotests.direct.intapi.java.tests.bidmodifiers;

import java.util.ArrayList;
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
import ru.yandex.autotests.direct.intapi.java.steps.BidModifierControllerSteps;
import ru.yandex.autotests.direct.intapi.models.BidModifierABSegmentConditionWeb;
import ru.yandex.autotests.direct.intapi.models.BidModifierABSegmentWeb;
import ru.yandex.autotests.direct.intapi.models.BidModifierInventoryConditionWeb;
import ru.yandex.autotests.direct.intapi.models.BidModifierInventoryWeb;
import ru.yandex.autotests.direct.intapi.models.BidModifierSingleWeb;
import ru.yandex.autotests.direct.intapi.models.ComplexBidModifierWeb;
import ru.yandex.autotests.direct.intapi.models.IntapiValidationResponse;
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;

@Aqua.Test
@Description("Проверка работы BidModifiersController validate")
@Stories(TestFeatures.BidModifiers.VALIDATE)
@Features(TestFeatures.BIDMODIFIERS)
@Tag(Tags.BIDMODIFIERS)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-80784")
public class BidModifiersControllerValidateTest {

    private static final String LOGIN = LOGIN_MAIN;
    private static final String CPMBANNER = "cpm_banner";

    @ClassRule
    public static final DirectRule directClassRule = DirectRule.defaultClassRule();

    @ClassRule
    public static final ApiSteps api = new ApiSteps().as(LOGIN_MAIN);

    @ClassRule
    public static final SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private BidModifierControllerSteps bidModifierControllerSteps;

    private Long clientId;

    @Before
    public void before() {
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();

        CampaignsRecord campaignsRecord =
                directClassRule.dbSteps().useShardForLogin(LOGIN).campaignsSteps().getCampaignById(campaignId);
        clientId = campaignsRecord.getClientid();
        bidModifierControllerSteps = directClassRule.intapiSteps().bidModifierControllerSteps();
    }

    @Test
    public void validate_InvalidRequest_FailedValidate() {
        ComplexBidModifierWeb request = createInvalidRequest();
        IntapiValidationResponse response = bidModifierControllerSteps.validate(clientId, CPMBANNER, request);
        assertThat(response.getSuccess(), is(false));
    }

    @Test
    public void validate_GodRequest_SuccessValidate() {
        ComplexBidModifierWeb request = createGoodRequest();
        IntapiValidationResponse response = bidModifierControllerSteps.validate(clientId, CPMBANNER, request);
        assertThat(response.getSuccess(), is(true));
    }

    @Test
    public void validate_InvalidRequestInventory_FailedValidate() {
        ComplexBidModifierWeb request = createRequestInventory(1350);
        IntapiValidationResponse response = bidModifierControllerSteps.validate(clientId, CPMBANNER, request);
        assertThat(response.getSuccess(), is(false));
    }

    @Test
    public void validate_InvalidRequestAbSegment_FailedValidate() {
        ComplexBidModifierWeb request = createRequestAbSegmet(1350);
        IntapiValidationResponse response = bidModifierControllerSteps.validate(clientId, CPMBANNER, request);
        assertThat(response.getSuccess(), is(false));
    }

    @Test
    public void validate_GodRequestInventory_SuccessValidate() {
        ComplexBidModifierWeb request = createRequestInventory(1250);
        IntapiValidationResponse response = bidModifierControllerSteps.validate(clientId, CPMBANNER, request);
        assertThat(response.getSuccess(), is(true));
    }

    private static ComplexBidModifierWeb createInvalidRequest() {
        return createVideoRequest(1400);
    }

    private static ComplexBidModifierWeb createGoodRequest() {
        return createVideoRequest(1250);
    }

    private static ComplexBidModifierWeb createVideoRequest(int multiplierPct) {
        return new ComplexBidModifierWeb()
                .withVideoMultiplier(new BidModifierSingleWeb().withMultiplierPct(multiplierPct));
    }

    private static ComplexBidModifierWeb createRequestInventory(int multiplierPct) {
        List<BidModifierInventoryConditionWeb> conditions = new ArrayList<>();
        conditions.add(new BidModifierInventoryConditionWeb()
                .withMultiplierPct(multiplierPct)
                .withMultiplierType("inpage"));
        return new ComplexBidModifierWeb()
                .withInventoryMultiplier(new BidModifierInventoryWeb().withConditions(conditions));
    }

    private static ComplexBidModifierWeb createRequestAbSegmet(int multiplierPct) {
        List<BidModifierABSegmentConditionWeb> conditions = new ArrayList<>();
        conditions.add(new BidModifierABSegmentConditionWeb().withMultiplierPct(multiplierPct));
        return new ComplexBidModifierWeb()
                .withAbSegmentMultiplier(new BidModifierABSegmentWeb().withAbSegments(conditions));
    }
}
