package ru.yandex.direct.core.entity.bidmodifiers.add.position;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.trafaretPositionsIntersection;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultTrafaretPositionAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientTrafaretPositionModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Негативные сценарии добавления корректировок ставок на позицию")
public class AddBidModifiersTrafaretPositionNegativeTest {

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroup;
    private Long campaignId;

    @Before
    public void before() {
        adGroup = adGroupSteps.createActiveTextAdGroup();
        campaignId = adGroup.getCampaignId();
    }

    @Test
    @Description("Добавим две одинаковые корректировки двумя запросами")
    public void sameTrafaretPositionAdjustmentsInSomeRequestTest() {
        addBidModifiers(
                singletonList(
                        createEmptyClientTrafaretPositionModifier()
                                .withCampaignId(campaignId)
                                .withTrafaretPositionAdjustments(
                                        Collections.singletonList(createDefaultTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.ALONE)))));
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientTrafaretPositionModifier()
                                .withCampaignId(campaignId)
                                .withTrafaretPositionAdjustments(
                                        Collections.singletonList(createDefaultTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.ALONE)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("trafaretPositionAdjustments")),
                        trafaretPositionsIntersection())));
    }

    @Test
    @Description("Добавим две одинаковые корректировки одним запросом")
    public void sameTrafaretPositionAdjustmentsInOneRequestTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientTrafaretPositionModifier()
                                .withCampaignId(campaignId)
                                .withTrafaretPositionAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierTrafaretPositionAdjustment()
                                                        .withTrafaretPosition(TrafaretPosition.ALONE)
                                                        .withPercent(110),
                                                new BidModifierTrafaretPositionAdjustment()
                                                        .withTrafaretPosition(TrafaretPosition.ALONE)
                                                        .withPercent(110)
                                        )
                                )));
        assertThat(result.getValidationResult().flattenErrors(), contains(
                validationError(path(index(0), field("trafaretPositionAdjustments"), index(0)),
                        new Defect<>(BidModifiersDefectIds.GeneralDefects.DUPLICATE_ADJUSTMENT)),
                validationError(path(index(0), field("trafaretPositionAdjustments"), index(1)),
                        new Defect<>(BidModifiersDefectIds.GeneralDefects.DUPLICATE_ADJUSTMENT))));
    }

    @Test
    @Description("Добавляем корректировку ставок на группу")
    public void trafaretPositionAdjustmentToGroupTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientTrafaretPositionModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withTrafaretPositionAdjustments(
                                        Collections.singletonList(createDefaultTrafaretPositionAdjustment()))));
        assertThat(result.getValidationResult().flattenErrors(), contains(validationError(path(index(0), field(
                "trafaretPositionAdjustments"), index(0)), notSupportedMultiplier())));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, adGroup.getClientId(), adGroup.getUid());
    }
}
