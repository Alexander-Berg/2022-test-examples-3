package ru.yandex.direct.core.entity.banner.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.update.ExecutionStep;

import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.dbutil.SqlUtils.ID_NOT_SET;

@CoreTest
public class BannersSuspendResumeOperationTest {
    BannersSuspendResumeOperation operation;
    BannerWithSystemFields banner;
    long bannerId = nextLong();
    long adGroupId = nextLong();

    private ExecutionStep<BannerWithSystemFields> step;
    private long someGeo = nextLong();
    private long someOtherGeo = nextLong();
    private boolean resume = true;

    @Before
    public void setUp() throws Exception {
        operation = mock(BannersSuspendResumeOperation.class);
        step = mock(ExecutionStep.class);
        banner = new TextBanner().withId(bannerId).withAdGroupId(adGroupId);

        final List<AppliedChanges<BannerWithSystemFields>> appliedChanges = Stream
                .of(new ModelChanges<>(bannerId, BannerWithSystemFields.class))
                .map(mc -> mc.process(LocalDateTime.now().minusDays(1L), BannerWithSystemFields.LAST_CHANGE))
                .map(mc -> mc.process(1L, BannerWithSystemFields.BS_BANNER_ID))
                .map(c -> c.applyTo(banner))
                .collect(Collectors.toList());
        when(step.getAppliedChangesForExecution()).thenReturn(appliedChanges);
    }

    @Test
    public void getAdGroupsForBsSyncTest_affected() {
        Map<Long, List<Long>> bannerMinusGeo = Collections.singletonMap(bannerId, Collections.singletonList(someGeo));
        Map<Long, List<Long>> adGroupGeo = Collections.singletonMap(adGroupId, Collections.singletonList(someGeo));
        Set<Long> result = BannersSuspendResumeOperation.getAdGroupsForBsSync(bannerMinusGeo, adGroupGeo,
                step.getAppliedChangesForExecution(), resume);
        assertThat(result, equalTo(Collections.singleton(adGroupId)));
    }

    @Test
    public void getAdGroupsForBsSyncTest_empty() {
        Map<Long, List<Long>> bannerMinusGeo = Collections.singletonMap(bannerId, Collections.singletonList(someGeo));
        Map<Long, List<Long>> adGroupGeo = Collections.singletonMap(adGroupId, Collections.singletonList(someOtherGeo));
        Set<Long> result = BannersSuspendResumeOperation.getAdGroupsForBsSync(bannerMinusGeo, adGroupGeo,
                step.getAppliedChangesForExecution(), resume);
        assertThat("Группа не отправлена в БК", result, empty());
    }

    @Test
    public void getAdGroupsForBsSyncTest_bannerNeverSentToBs_OnResumeReturnOneAdgroup() {
        Map<Long, List<Long>> bannerMinusGeo = Collections.singletonMap(bannerId, Collections.singletonList(someGeo));
        Map<Long, List<Long>> adGroupGeo = Collections.singletonMap(adGroupId, Collections.singletonList(someOtherGeo));
        step.getAppliedChangesForExecution().forEach(ac -> ac.modify(BannerWithSystemFields.BS_BANNER_ID,
                ID_NOT_SET));
        Set<Long> result = BannersSuspendResumeOperation.getAdGroupsForBsSync(bannerMinusGeo, adGroupGeo,
                step.getAppliedChangesForExecution(), true);
        assertThat("Группа отправлена в БК", result, not(empty()));
    }

    @Test
    public void getAdGroupsForBsSyncTest_bannerBsIdMissing_OnResumeReturnEmpty() {
        Map<Long, List<Long>> bannerMinusGeo = Collections.singletonMap(bannerId, Collections.singletonList(someGeo));
        Map<Long, List<Long>> adGroupGeo = Collections.singletonMap(adGroupId, Collections.singletonList(someOtherGeo));
        step.getAppliedChangesForExecution().forEach(ac -> ac.modify(BannerWithSystemFields.BS_BANNER_ID, null));
        Set<Long> result = BannersSuspendResumeOperation.getAdGroupsForBsSync(bannerMinusGeo, adGroupGeo,
                step.getAppliedChangesForExecution(), true);
        assertThat("Группа не отправлена в БК", result, empty());
    }

    @Test
    public void getAdGroupsForBsSyncTest_bannerNeverSentToBs_OnSuspendReturnEmpty() {
        Map<Long, List<Long>> bannerMinusGeo = Collections.singletonMap(bannerId, Collections.singletonList(someGeo));
        Map<Long, List<Long>> adGroupGeo = Collections.singletonMap(adGroupId, Collections.singletonList(someOtherGeo));
        step.getAppliedChangesForExecution().forEach(ac -> ac.modify(BannerWithSystemFields.BS_BANNER_ID, 0L));
        Set<Long> result = BannersSuspendResumeOperation.getAdGroupsForBsSync(bannerMinusGeo, adGroupGeo,
                step.getAppliedChangesForExecution(), false);
        assertThat("Группа не отправлена в БК", result, empty());
    }

    @Test
    public void onAppliedChangesValidated_normalResume() {
        banner = banner.withStatusShow(false).withStatusBsSynced(StatusBsSynced.YES);
        AppliedChanges<BannerWithSystemFields> ac =
                onAppliedChangesValidatedTest(BannerWithSystemFields.STATUS_SHOW, Boolean.TRUE);
        Assert.assertTrue(ac.hasActuallyChangedProps());
        Assert.assertTrue(ac.changed(BannerWithSystemFields.STATUS_SHOW));
        Assert.assertTrue(ac.changed(BannerWithSystemFields.STATUS_BS_SYNCED));
        Assert.assertTrue(ac.changed(BannerWithSystemFields.LAST_CHANGE));
    }

    @Test
    public void onAppliedChangesValidated_alreadyResumed() {
        banner = banner.withStatusShow(true).withStatusBsSynced(StatusBsSynced.YES);
        AppliedChanges<BannerWithSystemFields> ac =
                onAppliedChangesValidatedTest(BannerWithSystemFields.STATUS_SHOW, Boolean.TRUE);
        Assert.assertFalse(ac.hasActuallyChangedProps());
    }

    private AppliedChanges<BannerWithSystemFields> onAppliedChangesValidatedTest(
            ModelProperty<? super BannerWithSystemFields, Boolean> property,
            Boolean val) {
        AppliedChanges<BannerWithSystemFields> ac = new ModelChanges<>(bannerId, BannerWithSystemFields.class)
                .process(val, property)
                .applyTo(banner);

        when(step.getValidAppliedChangesWithIndex()).thenReturn(Collections.singletonMap(0, ac));
        doCallRealMethod().when(operation).onAppliedChangesValidated(any());
        operation.onAppliedChangesValidated(step);
        return ac;
    }
}
