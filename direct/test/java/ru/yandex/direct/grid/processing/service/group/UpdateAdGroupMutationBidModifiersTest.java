package ru.yandex.direct.grid.processing.service.group;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import graphql.ExecutionResult;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;

import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toComplexBidModifier;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdGroupMutationBidModifiersTest extends UpdateAdGroupMutationBaseTest {

    @Before
    public void init() {
        super.initTestData();
    }

    @Test
    public void checkUpdateAdGroupBidModifier_useBidModifierFalse() {
        checkUpdateAdGroupBidModifier(withNewBidModifier(), false,
                toListBidModifiers(toComplexBidModifier(initialBidModifier())));
    }

    @Test
    public void checkUpdateAdGroupBidModifier_useBidModifierNull() {
        checkUpdateAdGroupBidModifier(withNewBidModifier(), null,
                toListBidModifiers(toComplexBidModifier(initialBidModifier())));
    }

    @Test
    public void checkUpdateAdGroupBidModifier_newBidModifier() {
        checkUpdateAdGroupBidModifier(withNewBidModifier(), true,
                toListBidModifiers(toComplexBidModifier(withNewBidModifier())));
    }

    @Test
    public void checkUpdateAdGroupBidModifier_updateBidModifier() {
        checkUpdateAdGroupBidModifier(updatedBidModifier(), true,
                toListBidModifiers(toComplexBidModifier(updatedBidModifier())));
    }

    @Test
    public void checkUpdateAdGroupBidModifier_deleteBidModifier() {
        checkUpdateAdGroupBidModifier(initialBidModifier().withBidModifierDemographics(null), true,
                Collections.emptyList());
    }

    private void checkUpdateAdGroupBidModifier(GdUpdateBidModifiers updateBidModifiers,
                                               @Nullable Boolean useBidModifiers, List<BidModifier> expectedModifiers) {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setBidModifiers(updateBidModifiers)
                .setUseBidModifiers(useBidModifiers)
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);


        checkBidModifiersDbState(expectedModifiers);

        List<BidModifierAdjustment> expectedAdjustments = getAdjustmentsList(expectedModifiers);
        checkBidModifiersAdjustmentsDbState(expectedAdjustments);
        softAssertions.assertAll();
    }

    private GdUpdateBidModifiers initialBidModifier() {
        return requestBuilder().getGdUpdateBidModifiers(adGroupBidModifierInfo);
    }

    private GdUpdateBidModifiers withNewBidModifier() {
        return requestBuilder().getGdUpdateBidModifiers(adGroupBidModifierInfo)
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withAdGroupId(textAdGroupInfo.getAdGroupId())
                        .withCampaignId(textAdGroupInfo.getCampaignId())
                        .withEnabled(true)
                        .withType(GdBidModifierType.MOBILE_MULTIPLIER)
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withOsType(null)
                                .withPercent(1000)));
    }

    private GdUpdateBidModifiers updatedBidModifier() {
        return initialBidModifier().withBidModifierDemographics(initialBidModifier().getBidModifierDemographics()
                .withEnabled(!initialBidModifier().getBidModifierDemographics().getEnabled())
                .withAdjustments(Collections
                        .singletonList(initialBidModifier().getBidModifierDemographics().getAdjustments().get(0)
                                .withPercent(DEFAULT_PERCENT + 10))));
    }

    private List<BidModifier> toListBidModifiers(ComplexBidModifier complexBidModifier) {
        ArrayList<BidModifier> bidModifiers = new ArrayList<>();
        ifNotNull(complexBidModifier.getAbSegmentModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getBannerTypeModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getDemographyModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getDesktopModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getGeoModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getInventoryModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getMobileModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getPerformanceTgoModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getRetargetingModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getVideoModifier(), bidModifiers::add);
        ifNotNull(complexBidModifier.getTrafaretPositionModifier(), bidModifiers::add);
        return bidModifiers;
    }

    private void checkBidModifiersAdjustmentsDbState(List<BidModifierAdjustment> expectedBidModifierAdjustments) {
        softAssertions.assertThat(getAdjustmentsList(getActualBidModifiers()))
                .usingElementComparatorIgnoringFields("id", "lastChange")
                .containsExactlyElementsOf(expectedBidModifierAdjustments);
    }

    private List<BidModifierAdjustment> getAdjustmentsList(List<BidModifier> bidModifiers) {
        Map<BidModifierType, BidModifier>
                bidModifiersMap = listToMap(bidModifiers, BidModifier::getType, Function.identity());
        List<BidModifierAdjustment> bidModifierAdjustments = new ArrayList<>();
        if (bidModifiersMap.get(BidModifierType.MOBILE_MULTIPLIER) != null) {
            bidModifierAdjustments
                    .add(((BidModifierMobile) bidModifiersMap.get(BidModifierType.MOBILE_MULTIPLIER))
                            .getMobileAdjustment());
        }
        if (bidModifiersMap.get(BidModifierType.DEMOGRAPHY_MULTIPLIER) != null) {
            bidModifierAdjustments
                    .addAll(((BidModifierDemographics) bidModifiersMap.get(BidModifierType.DEMOGRAPHY_MULTIPLIER))
                            .getDemographicsAdjustments());
        }
        return bidModifierAdjustments;
    }
}
