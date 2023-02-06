package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbschema.ppc.tables.Campaigns;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefectIds.IdParametrized.ADGROUP_NOT_FOUND;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
@Description("Запрос на отключение наборов корректировок с указанием невалидных идентификаторов")
public class ToggleBidModifiersInvalidIdsTest {
    private static final long ZERO_ID = 0L;
    private static final long NEGATIVE_ID = -1L;
    private static final long NONEXISTENT_ID = 123456L;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    public static final Function<Long, BidModifier> BIDMODIFIER_BY_CAMPAIGN =
            (id) -> new UntypedBidModifier().withCampaignId(id);
    public static final Function<Long, BidModifier> BIDMODIFIER_BY_ADGROUP =
            (id) -> new UntypedBidModifier().withAdGroupId(id);
    private static Long groupDeleted;
    private static Long campaignDeleted;
    private static Long groupAnother;
    private static Long campaignAnother;

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    private CampaignInfo campaignInfo;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public Supplier<Long> idSupplier;

    @Parameterized.Parameter(2)
    public String filedName;

    @Parameterized.Parameter(3)
    public Function<Long, BidModifier> bidModifierFactory;

    @Parameterized.Parameter(4)
    public DefectId expectedError;

    @Parameterized.Parameters(name = "field = {2}, test = {0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Zero Id", (Supplier<Long>) () -> ZERO_ID, "campaignId",
                        BIDMODIFIER_BY_CAMPAIGN, MUST_BE_VALID_ID},
                {"Zero Id", (Supplier<Long>) () -> ZERO_ID, "adGroupId",
                        BIDMODIFIER_BY_ADGROUP, MUST_BE_VALID_ID},
                {"Negative Id", (Supplier<Long>) () -> NEGATIVE_ID, "campaignId",
                        BIDMODIFIER_BY_CAMPAIGN, MUST_BE_VALID_ID},
                {"Negative Id", (Supplier<Long>) () -> NEGATIVE_ID, "adGroupId",
                        BIDMODIFIER_BY_ADGROUP, MUST_BE_VALID_ID},
                {"Nonexistent Id", (Supplier<Long>) () -> NONEXISTENT_ID, "campaignId",
                        BIDMODIFIER_BY_CAMPAIGN, CAMPAIGN_NOT_FOUND},
                {"Nonexistent Id", (Supplier<Long>) () -> NONEXISTENT_ID, "adGroupId",
                        BIDMODIFIER_BY_ADGROUP, ADGROUP_NOT_FOUND},
                {"Deleted Id", (Supplier<Long>) () -> campaignDeleted, "campaignId",
                        BIDMODIFIER_BY_CAMPAIGN, CAMPAIGN_NOT_FOUND},
                {"Deleted Id", (Supplier<Long>) () -> groupDeleted, "adGroupId",
                        BIDMODIFIER_BY_ADGROUP, ADGROUP_NOT_FOUND},
                {"Another client Id", (Supplier<Long>) () -> campaignAnother, "campaignId",
                        BIDMODIFIER_BY_CAMPAIGN, CAMPAIGN_NOT_FOUND},
                {"Another client Id", (Supplier<Long>) () -> groupAnother, "adGroupId",
                        BIDMODIFIER_BY_ADGROUP, ADGROUP_NOT_FOUND},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        campaignInfo = campaignSteps.createActiveTextCampaign();

        // создаем клиента
        ClientInfo agency = clientSteps.createDefaultClientWithRole(RbacRole.AGENCY);

        // создаем и удаляем кампанию
        CampaignInfo campaignInfoDeleted = campaignSteps.createActiveTextCampaign();
        campaignDeleted = campaignInfoDeleted.getCampaignId();
        DSLContext dslContext = ppcDslContextProvider.ppc(agency.getShard());
        dslContext.delete(Campaigns.CAMPAIGNS)
                .where(Campaigns.CAMPAIGNS.CID.eq(campaignDeleted))
                .execute();

        // создаем и удаляем группу
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        groupDeleted = adGroupInfo.getAdGroupId();
        adGroupService.deleteAdGroups(adGroupInfo.getUid(), adGroupInfo.getClientId(), singletonList(groupDeleted));

        AdGroupInfo adGroupInfoAnother = adGroupSteps.createActiveTextAdGroup(agency);
        CampaignInfo campaignInfoAnother = adGroupInfoAnother.getCampaignInfo();
        groupAnother = adGroupInfoAnother.getAdGroupId();
        campaignAnother = campaignInfoAnother.getCampaignId();
    }

    @Test
    public void invalidIdsTest() {
        UntypedBidModifier bidModifier =
                (UntypedBidModifier) bidModifierFactory.apply(idSupplier.get())
                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                        .withEnabled(true);

        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(singletonList(bidModifier), campaignInfo.getClientId(),
                        campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(filedName)), expectedError)));
    }
}
