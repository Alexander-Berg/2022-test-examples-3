package ru.yandex.direct.core.entity.bidmodifiers.get.retargeting;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealType;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;
import static ru.yandex.direct.dbschema.ppc.tables.RetargetingGoals.RETARGETING_GOALS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Если в таблице retargeting_goals нет целей, соответсвующих ret_cond_id, корректировка всё равно будет возвращена")
public class GetBidModifiersRetargetingWithUnexistingGoalsTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private CampaignInfo campaign;
    private Long modifierId;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
        Long retCondId = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo()).getRetConditionId();

        List<AbstractBidModifierRetargetingAdjustment> retargetingAdjustments =
                createDefaultRetargetingAdjustments(retCondId);
        MassResult<List<Long>> result = bidModifierService.add(
                singletonList(
                        createEmptyRetargetingModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRetargetingAdjustments(retargetingAdjustments)
                ), campaign.getClientId(), campaign.getUid());

        modifierId = result.get(0).getResult().get(0);

        // Удаляем goal из таблицы, чтобы ret_cond_id ссылался на несуществующую запись
        DSLContext dslContext = dslContextProvider.ppc(campaign.getShard());
        dslContext.deleteFrom(RETARGETING_GOALS)
                .where(RETARGETING_GOALS.RET_COND_ID.eq(retCondId))
                .execute();
    }

    @Test
    public void bidModifierIsRetrievedOkByCampaignIdTest() {
        List<BidModifier> gotModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.RETARGETING_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN),
                        campaign.getUid());

        List<AbstractBidModifierRetargetingAdjustment> gotAdjustments =
                ((BidModifierRetargeting) gotModifiers.get(0)).getRetargetingAdjustments();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(gotAdjustments).isNotNull();
            softAssertions.assertThat(gotAdjustments).hasSize(1);
            softAssertions.assertThat(gotAdjustments).is(matchedBy(
                    contains(hasProperty("accessible", equalTo(false)))
            ));
        });
    }

    @Test
    public void bidModifierIsRetrievedOkByIdTest() {
        ArrayListMultimap<BidModifierType, Long> idsByType = ArrayListMultimap.create();
        idsByType.put(getRealType(modifierId), getRealId(modifierId));

        List<BidModifier> gotModifiers =
                bidModifierService.getByIds(campaign.getClientId(), idsByType,
                        campaign.getUid());

        List<AbstractBidModifierRetargetingAdjustment> gotAdjustments =
                ((BidModifierRetargeting) gotModifiers.get(0)).getRetargetingAdjustments();

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(gotAdjustments).isNotNull();
            softAssertions.assertThat(gotAdjustments).hasSize(1);
            softAssertions.assertThat(gotAdjustments).is(matchedBy(
                    contains(hasProperty("accessible", equalTo(false)))
            ));
        });
    }
}
