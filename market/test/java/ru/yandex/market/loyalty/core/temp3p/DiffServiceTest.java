package ru.yandex.market.loyalty.core.temp3p;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.loyalty.core.model.promo3p.Promo3pMsku;
import ru.yandex.market.loyalty.core.service.diff.Diff;
import ru.yandex.market.loyalty.core.service.diff.DiffService;
import ru.yandex.market.loyalty.core.service.diff.MergeDiff;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.service.diff.DiffService.ConflictResolution.ACCEPT_CHANGED;
import static ru.yandex.market.loyalty.core.service.diff.DiffService.ConflictResolution.ACCEPT_CURRENT;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.FIRST_MSKU;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.FIRST_PRICE;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.SECOND_MSKU;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.SECOND_PRICE;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.THIRD_MSKU;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.firstMskuBuilder;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.secondMskuBuilder;
import static ru.yandex.market.loyalty.core.utils.Promo3pMskuFactory.thirdMskuBuilder;

public class DiffServiceTest {

    private DiffService diffService = new DiffService();

    @Test
    public void shouldReturnNoChangesIfNothingHappened() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOfUnchanged(diff),
                containsInAnyOrder(FIRST_MSKU, SECOND_MSKU)
        );
    }

    @Test
    public void shouldFindDeleteOperation() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder(),
                thirdMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Arrays.asList(
                firstMskuBuilder(),
                thirdMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOdDeleted(diff),
                contains(SECOND_MSKU)
        );
    }

    @Test
    public void shouldFindDeleteOperationIfLastItemsDeleted() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder(),
                thirdMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Collections.singletonList(
                firstMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOdDeleted(diff),
                containsInAnyOrder(SECOND_MSKU, THIRD_MSKU)
        );
    }


    @Test
    public void shouldFindDeleteAll() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder(),
                thirdMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Collections.emptyList();

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOdDeleted(diff),
                containsInAnyOrder(FIRST_MSKU, SECOND_MSKU, THIRD_MSKU)
        );
    }

    @Test
    public void shouldFindUpdateOperation() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder().setPrice(SECOND_PRICE.add(BigDecimal.ONE))
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOfUpdated(diff),
                contains(SECOND_MSKU)
        );
        assertEquals(SECOND_PRICE.add(BigDecimal.ONE), diff.getUpdates().get(0).getAfter().build().getPrice());
    }

    @Test
    public void shouldFindInsertOperation() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                thirdMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder(),
                thirdMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOfInserted(diff),
                contains(SECOND_MSKU)
        );
    }

    @Test
    public void shouldFindInsertOperationIfLastItemsInserted() {
        List<Promo3pMsku.Builder> base = Collections.singletonList(
                firstMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder(),
                thirdMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOfInserted(diff),
                containsInAnyOrder(SECOND_MSKU, THIRD_MSKU)
        );
    }

    @Test
    public void shouldFindInsertDeleteAndUpdateOperations() {
        List<Promo3pMsku.Builder> base = Arrays.asList(
                firstMskuBuilder(),
                secondMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Arrays.asList(
                secondMskuBuilder().setPrice(SECOND_PRICE.add(BigDecimal.ONE)),
                thirdMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOdDeleted(diff),
                contains(FIRST_MSKU)
        );

        assertThat(
                keysOfUpdated(diff),
                contains(SECOND_MSKU)
        );

        assertThat(
                keysOfInserted(diff),
                contains(THIRD_MSKU)
        );
    }

    @Test
    public void shouldMayCompareCompletelyDifferentSets() {
        List<Promo3pMsku.Builder> base = Collections.singletonList(
                firstMskuBuilder()
        );

        List<Promo3pMsku.Builder> changed = Collections.singletonList(
                secondMskuBuilder()
        );

        Diff<Promo3pMsku.Builder> diff = diffService.computeDiff(base, changed,
                Promo3pMsku.Builder.getDiffDescriptor());

        assertThat(
                keysOdDeleted(diff),
                contains(FIRST_MSKU)
        );

        assertThat(
                keysOfInserted(diff),
                contains(SECOND_MSKU)
        );

    }

    @Test
    public void testSuccessMergeUNCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = ImmutableList.copyOf(base);

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), hasSize(1));
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testSuccessMergeNUCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = ImmutableList.copyOf(base);

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testSuccessMergeDNCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.emptyList();

        List<Promo3pMsku> current = ImmutableList.copyOf(base);

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), hasSize(1));
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testSuccessMergeNDCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = ImmutableList.copyOf(base);

        List<Promo3pMsku> current = Collections.emptyList();

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }


    @Test
    public void testSuccessMergeXICase() {
        List<Promo3pMsku> base = Collections.emptyList();

        List<Promo3pMsku> changed = ImmutableList.copyOf(base);

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testSuccessMergeIXCase() {
        List<Promo3pMsku> base = Collections.emptyList();

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> current = ImmutableList.copyOf(base);

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), hasSize(1));
    }

    @Test
    public void testSuccessMergeUUCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeUUCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(200))).build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertTrue(merge.hasConflicts());
        assertThat(merge.getIIConflicts(), empty());
        assertThat(merge.getUDConflicts(), empty());
        assertThat(merge.getDUConflicts(), empty());
        assertThat(merge.getUUConflicts(), hasSize(1));
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeResolutionUUCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(200))).build()
        );

        MergeDiff<Promo3pMsku> merge;

        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CHANGED));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), hasSize(1));
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());


        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CURRENT));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testSuccessMergeIICase() {
        List<Promo3pMsku> base = Collections.emptyList();

        List<Promo3pMsku> changed = Collections.singletonList(
                secondMskuBuilder().build()
        );

        List<Promo3pMsku> current = Collections.singletonList(
                secondMskuBuilder().build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeResolutionIICase() {
        List<Promo3pMsku> base = Collections.emptyList();

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().build()
        );

        MergeDiff<Promo3pMsku> merge;

        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CHANGED));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), hasSize(1));

        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CURRENT));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }


    @Test
    public void testConflictMergeIICase() {
        List<Promo3pMsku> base = Collections.emptyList();

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertTrue(merge.hasConflicts());
        assertThat(merge.getIIConflicts(), hasSize(1));
        assertThat(merge.getUDConflicts(), empty());
        assertThat(merge.getDUConflicts(), empty());
        assertThat(merge.getUUConflicts(), empty());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }


    @Test
    public void testSuccessMergeDDCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.emptyList();

        List<Promo3pMsku> current = Collections.emptyList();

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeUDCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.emptyList();

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());


        assertTrue(merge.hasConflicts());
        assertThat(merge.getIIConflicts(), empty());
        assertThat(merge.getUDConflicts(), hasSize(1));
        assertThat(merge.getDUConflicts(), empty());
        assertThat(merge.getUUConflicts(), empty());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeResolutionUDCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        List<Promo3pMsku> current = Collections.emptyList();

        MergeDiff<Promo3pMsku> merge;

        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CHANGED));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), hasSize(1));
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());


        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CURRENT));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeDUCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.emptyList();

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        MergeDiff<Promo3pMsku> merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor());

        assertTrue(merge.hasConflicts());
        assertThat(merge.getIIConflicts(), empty());
        assertThat(merge.getUDConflicts(), empty());
        assertThat(merge.getDUConflicts(), hasSize(1));
        assertThat(merge.getUUConflicts(), empty());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    @Test
    public void testConflictMergeResolutionDUCase() {
        List<Promo3pMsku> base = Collections.singletonList(
                firstMskuBuilder().build()
        );

        List<Promo3pMsku> changed = Collections.emptyList();

        List<Promo3pMsku> current = Collections.singletonList(
                firstMskuBuilder().setPrice(FIRST_PRICE.add(BigDecimal.valueOf(100))).build()
        );

        MergeDiff<Promo3pMsku> merge;

        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CHANGED));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), empty());
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), hasSize(1));
        assertThat(merge.getInserts(), empty());

        merge = diffService.merge(base, changed, current, Promo3pMsku.getDiffItemDescriptor(),
                ImmutableMap.of(FIRST_MSKU, ACCEPT_CURRENT));

        assertFalse(merge.hasConflicts());
        assertThat(merge.getNoChanges(), hasSize(1));
        assertThat(merge.getUpdates(), empty());
        assertThat(merge.getDeletes(), empty());
        assertThat(merge.getInserts(), empty());
    }

    private static Set<String> keysOfUpdated(Diff<Promo3pMsku.Builder> diff) {
        return diff.getUpdates().stream().map(Diff.Update::getAfter).map(Promo3pMsku.Builder::getMsku).collect(Collectors.toSet());
    }

    private static Set<String> keysOdDeleted(Diff<Promo3pMsku.Builder> diff) {
        return diff.getDeletes().stream().map(Diff.Delete::getOldVersion).map(Promo3pMsku.Builder::getMsku).collect(Collectors.toSet());
    }

    private static Set<String> keysOfInserted(Diff<Promo3pMsku.Builder> diff) {
        return diff.getInserts().stream().map(Diff.Insert::getNewVersion).map(Promo3pMsku.Builder::getMsku).collect(Collectors.toSet());
    }

    private static Set<String> keysOfUnchanged(Diff<Promo3pMsku.Builder> diff) {
        return diff.getNoChanges().stream().map(Diff.NoChanges::getItem).map(Promo3pMsku.Builder::getMsku).collect(Collectors.toSet());
    }

}
