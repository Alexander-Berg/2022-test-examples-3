package ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.multivalue;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.common.log.service.LogBidModifiersService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.regions.Region;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.multivalue.AbstractBidModifierMultipleValuesTypeSupport.HierarchicalMultiplierAction.DELETE;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.multivalue.AbstractBidModifierMultipleValuesTypeSupport.HierarchicalMultiplierAction.INSERT;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.multivalue.AbstractBidModifierMultipleValuesTypeSupport.HierarchicalMultiplierAction.NOTHING;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.multivalue.AbstractBidModifierMultipleValuesTypeSupport.HierarchicalMultiplierAction.UPDATE;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ComputeDiffTests {
    @Mock
    private ShardHelper shardHelper;

    @Mock
    private LogBidModifiersService logBidModifiersService;

    @Mock
    private ClientService clientService;

    @Test
    public void testSimpleCreateNew() {
        BidModifierGeoTypeSupport typeSupport = new BidModifierGeoTypeSupport(shardHelper, logBidModifiersService,
                clientService);

        List<BidModifierRegionalAdjustment> targetAdjustments =
                asList(new BidModifierRegionalAdjustment()
                                .withRegionId(Region.SAMARA_OBLAST_REGION_ID).withPercent(20).withHidden(false),
                        new BidModifierRegionalAdjustment()
                                .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(30).withHidden(false),
                        new BidModifierRegionalAdjustment()
                                .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false));

        AbstractBidModifierMultipleValuesTypeSupport.DiffInfo<BidModifierRegionalAdjustment>
                diff = typeSupport.computeDiff(targetAdjustments, emptyList(), null, true, now());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(diff.toInsert).containsAll(targetAdjustments);
            softAssertions.assertThat(diff.toDelete).isEmpty();
            softAssertions.assertThat(diff.toUpdate).isEmpty();
            softAssertions.assertThat(diff.hierarchicalMultiplierAction).isEqualTo(INSERT);
        });
    }

    @Test
    public void testAddToExisting() {
        BidModifierGeoTypeSupport typeSupport = new BidModifierGeoTypeSupport(shardHelper, logBidModifiersService,
                clientService);

        List<BidModifierRegionalAdjustment> targetAdjustments =
                asList(new BidModifierRegionalAdjustment()
                                .withRegionId(Region.SAMARA_OBLAST_REGION_ID).withPercent(20).withHidden(false),
                        new BidModifierRegionalAdjustment()
                                .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(30).withHidden(false),
                        new BidModifierRegionalAdjustment()
                                .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false));

        List<BidModifierRegionalAdjustment> existingAdjustments =
                singletonList(new BidModifierRegionalAdjustment()
                        .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(30).withHidden(false));

        BidModifierGeo existingModifier = createEmptyGeoModifier();

        AbstractBidModifierMultipleValuesTypeSupport.DiffInfo<BidModifierRegionalAdjustment> diff =
                typeSupport.computeDiff(targetAdjustments, existingAdjustments, existingModifier, true, now());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(diff.toInsert).containsAll(
                    asList(
                            new BidModifierRegionalAdjustment()
                                    .withRegionId(Region.SAMARA_OBLAST_REGION_ID).withPercent(20).withHidden(false),
                            new BidModifierRegionalAdjustment()
                                    .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false)
                    )
            );
            softAssertions.assertThat(diff.toDelete).isEmpty();
            softAssertions.assertThat(diff.toUpdate).isEmpty();
            softAssertions.assertThat(diff.hierarchicalMultiplierAction).isEqualTo(UPDATE);
        });
    }

    @Test
    public void testUpdateExisting() {
        BidModifierGeoTypeSupport typeSupport = new BidModifierGeoTypeSupport(shardHelper, logBidModifiersService,
                clientService);

        List<BidModifierRegionalAdjustment> targetAdjustments =
                asList(new BidModifierRegionalAdjustment()
                                .withRegionId(Region.SAMARA_OBLAST_REGION_ID).withPercent(20).withHidden(false),
                        new BidModifierRegionalAdjustment()
                                .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(30).withHidden(false),
                        new BidModifierRegionalAdjustment()
                                .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(true));

        List<BidModifierRegionalAdjustment> existingAdjustments =
                asList(new BidModifierRegionalAdjustment().withId(1L)
                                .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(100).withHidden(false),
                        new BidModifierRegionalAdjustment().withId(2L)
                                .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false),
                        new BidModifierRegionalAdjustment().withId(3L)
                                .withRegionId(Region.ULYANOVSK_OBLAST_REGION_ID).withPercent(120).withHidden(true)
                );

        BidModifierGeo existingModifier = createEmptyGeoModifier();

        LocalDateTime now = now();
        AbstractBidModifierMultipleValuesTypeSupport.DiffInfo<BidModifierRegionalAdjustment>
                diff = typeSupport.computeDiff(targetAdjustments, existingAdjustments, existingModifier, true, now);

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(diff.toInsert).containsAll(
                    asList(
                            new BidModifierRegionalAdjustment()
                                    .withRegionId(Region.SAMARA_OBLAST_REGION_ID).withPercent(20).withHidden(false),
                            new BidModifierRegionalAdjustment()
                                    .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(true)
                    )
            );
            softAssertions.assertThat(diff.toDelete).contains(
                    // Потому что его нет в целевом наборе
                    new BidModifierRegionalAdjustment().withId(3L)
                            .withRegionId(Region.ULYANOVSK_OBLAST_REGION_ID).withPercent(120).withHidden(true),
                    // Потому что в целевом наборе он с hidden=true
                    new BidModifierRegionalAdjustment().withId(2L)
                            .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false)
            );
            softAssertions.assertThat(diff.toUpdate).is(matchedBy(contains(hasProperty("model", equalTo(
                    new BidModifierRegionalAdjustment().withId(1L)
                            .withRegionId(Region.VORONEZH_OBLAST_REGION_ID)
                            .withPercent(30).withHidden(false)
                            .withLastChange(now)
            )))));
            softAssertions.assertThat(diff.hierarchicalMultiplierAction).isEqualTo(UPDATE);
        });
    }

    @Test
    public void testDeleteAll() {
        BidModifierGeoTypeSupport typeSupport = new BidModifierGeoTypeSupport(shardHelper, logBidModifiersService,
                clientService);

        List<BidModifierRegionalAdjustment> existingAdjustments =
                asList(new BidModifierRegionalAdjustment().withId(1L)
                                .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(100).withHidden(false),
                        new BidModifierRegionalAdjustment().withId(2L)
                                .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false),
                        new BidModifierRegionalAdjustment().withId(3L)
                                .withRegionId(Region.ULYANOVSK_OBLAST_REGION_ID).withPercent(120).withHidden(true)
                );

        BidModifierGeo existingModifier = createEmptyGeoModifier();

        AbstractBidModifierMultipleValuesTypeSupport.DiffInfo<BidModifierRegionalAdjustment>
                diff = typeSupport.computeDiff(emptyList(), existingAdjustments, existingModifier, true, now());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(diff.toInsert).isEmpty();
            softAssertions.assertThat(diff.toDelete).containsAll(existingAdjustments);
            softAssertions.assertThat(diff.toUpdate).isEmpty();
            softAssertions.assertThat(diff.hierarchicalMultiplierAction).isEqualTo(DELETE);
        });
    }

    @Test
    public void testNoChanges() {
        BidModifierGeoTypeSupport typeSupport = new BidModifierGeoTypeSupport(shardHelper, logBidModifiersService,
                clientService);

        List<BidModifierRegionalAdjustment> existingAdjustments =
                asList(new BidModifierRegionalAdjustment().withId(1L)
                                .withRegionId(Region.VORONEZH_OBLAST_REGION_ID).withPercent(100).withHidden(false),
                        new BidModifierRegionalAdjustment().withId(2L)
                                .withRegionId(Region.NOVOSIBIRSK_OBLAST_REGION_ID).withPercent(40).withHidden(false),
                        new BidModifierRegionalAdjustment().withId(3L)
                                .withRegionId(Region.ULYANOVSK_OBLAST_REGION_ID).withPercent(120).withHidden(true)
                );

        BidModifierGeo existingModifier = createEmptyGeoModifier();

        AbstractBidModifierMultipleValuesTypeSupport.DiffInfo<BidModifierRegionalAdjustment>
                diff = typeSupport.computeDiff(existingAdjustments, existingAdjustments, existingModifier, true, now());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(diff.toInsert).isEmpty();
            softAssertions.assertThat(diff.toDelete).isEmpty();
            softAssertions.assertThat(diff.toUpdate).isEmpty();
            softAssertions.assertThat(diff.hierarchicalMultiplierAction).isEqualTo(NOTHING);
        });
    }
}
