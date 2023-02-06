package ru.yandex.direct.core.entity.userssegments.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus;
import ru.yandex.direct.core.entity.adgroup.model.InternalStatus;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestUserSegments;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppc.tables.VideoSegmentGoals.VIDEO_SEGMENT_GOALS;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UsersSegmentRepositoryTest {

    @Autowired
    private UsersSegmentRepository usersSegmentRepository;
    @Autowired
    private AdGroupSteps adGroupSteps;
    @Autowired
    private DslContextProvider dslContextProvider;

    private AdGroupInfo adGroup;
    private int shard;

    @Before
    public void setUp() {
        adGroup = adGroupSteps.createActiveCpmVideoAdGroup();
        ClientInfo clientInfo = adGroup.getClientInfo();
        shard = clientInfo.getShard();
    }

    @Test
    public void addSegments_AddTwoWithSamePrimaryKey_AddedOne() {
        UsersSegment first = defaultSegment();
        UsersSegment second = defaultSegment();
        usersSegmentRepository.addSegments(adGroup.getShard(), asList(first, second));
        UsersSegment segment = usersSegmentRepository
                .getSegmentByPrimaryKey(adGroup.getShard(), adGroup.getAdGroupId(), AdShowType.START);
        assertNotNull(segment);

        List<UsersSegment> segments = usersSegmentRepository
                .getSegments(adGroup.getShard(), Collections.singletonList(adGroup.getAdGroupId()));
        assertThat(segments, hasSize(1));
        UsersSegment inserted = segments.get(0);
        assertEquals(AdShowType.START, inserted.getType());
    }

    @Test
    public void getSegments_ReturnCorrect() {
        UsersSegment first = defaultSegment();
        usersSegmentRepository.addSegments(adGroup.getShard(), Collections.singletonList(first));
        Map<Long, List<UsersSegment>> segmentsMap = usersSegmentRepository
                .getSegmentsAsMap(adGroup.getShard(), Collections.singletonList(adGroup.getAdGroupId()));
        assertNotNull(segmentsMap);
        List<UsersSegment> segments = segmentsMap.get(adGroup.getAdGroupId());
        assertNotNull(segments);
        assertThat(segments, hasSize(1));
        UsersSegment inserted = segments.get(0);
        assertEquals(AdShowType.START, inserted.getType());
        assertEquals(adGroup.getAdGroupId(), inserted.getAdGroupId());
    }

    @Test
    public void getSegmentsForCreate_FilteredByStatus() {
        UsersSegment segmentNew = defaultSegment()
                .withInternalStatus(InternalStatus.NEW_)
                .withLastSuccessUpdateTime(LocalDateTime.now());
        UsersSegment segmentNotNew = defaultSegment().withType(AdShowType.MIDPOINT)
                .withInternalStatus(InternalStatus.COMPLETE)
                .withLastSuccessUpdateTime(LocalDateTime.now());
        usersSegmentRepository.addSegments(shard, asList(segmentNew, segmentNotNew));

        List<UsersSegment> actualSegments = getAdGroupSegmentsForCreateSegments();
        assertThat("должно было вернуться одно задание", actualSegments, hasSize(1));
        assertThat(actualSegments.get(0).getInternalStatus(), is(InternalStatus.NEW_));
    }

    @Test
    public void getSegmentsForCreate_FilteredByIsDisabled() {
        UsersSegment segmentDisabled = defaultSegment().withIsDisabled(true);
        UsersSegment segmentNotDisabled = defaultSegment().withType(AdShowType.MIDPOINT).withIsDisabled(false);
        usersSegmentRepository.addSegments(shard, asList(segmentDisabled, segmentNotDisabled));

        List<UsersSegment> actualSegments = getAdGroupSegmentsForCreateSegments();
        assertThat("должно было вернуться одно задание", actualSegments, hasSize(1));
        assertThat(actualSegments.get(0).getIsDisabled(), is(false));
    }

    @Test
    public void getSegmentsForCreate_FilteredByExternalAudienceId() {
        UsersSegment segmentWithExternalAudience = defaultSegment()
                .withExternalAudienceId(123L);
        UsersSegment segmentWithoutExternalAudience =
                defaultSegment().withType(AdShowType.MIDPOINT)
                        .withExternalAudienceId(0L);
        usersSegmentRepository.addSegments(shard, asList(segmentWithExternalAudience,
                segmentWithoutExternalAudience));

        List<UsersSegment> actualSegments = getAdGroupSegmentsForCreateSegments();
        assertThat("должно было вернуться одно задание", actualSegments, hasSize(1));
        assertThat(actualSegments.get(0).getExternalAudienceId(), is(0L));
    }

    @Test
    public void getSegmentsForUpdate_FilteredByIsDisabled() {
        UsersSegment segmentDisabled = defaultSegmentForUpdate().withIsDisabled(true);
        UsersSegment segmentNotDisabled = defaultSegmentForUpdate().withType(AdShowType.MIDPOINT)
                .withIsDisabled(false);

        usersSegmentRepository.addSegments(shard, asList(segmentDisabled, segmentNotDisabled));

        List<UsersSegment> actualSegments = getAdGroupSegmentsForUpdateSegments();
        assertThat("должно было вернуться одно задание", actualSegments, hasSize(1));
        assertThat(actualSegments.get(0).getIsDisabled(), is(false));
    }

    @Test
    public void getSegmentsForUpdate_FilteredByExternalAudienceStatus() {
        UsersSegment segmentInProcess = defaultSegmentForUpdate()
                .withExternalAudienceId(1L)
                .withExternalAudienceStatus(ExternalAudienceStatus.IS_PROCESSED);
        UsersSegment segmentProcessed = defaultSegmentForUpdate().withType(AdShowType.MIDPOINT)
                .withExternalAudienceId(1L)
                .withExternalAudienceStatus(ExternalAudienceStatus.PROCESSED);
        UsersSegment segmentFewData = defaultSegmentForUpdate().withType(AdShowType.FIRST_QUARTILE)
                .withExternalAudienceId(1L)
                .withExternalAudienceStatus(ExternalAudienceStatus.FEW_DATA);

        usersSegmentRepository.addSegments(shard, asList(segmentInProcess, segmentProcessed, segmentFewData));

        List<UsersSegment> actualSegments = getAdGroupSegmentsForUpdateSegments();
        assertThat("должно было вернуться два задания", actualSegments, hasSize(2));
        Set<ExternalAudienceStatus> actualStatuses =
                listToSet(actualSegments, UsersSegment::getExternalAudienceStatus);
        assertEquals(actualStatuses, Set.of(ExternalAudienceStatus.PROCESSED, ExternalAudienceStatus.FEW_DATA));
    }

    @Test
    public void getSegmentsForUpdate_FilteredByExternalAudienceId() {
        UsersSegment segmentWithExternalAudience = defaultSegmentForUpdate().withExternalAudienceId(123L);
        UsersSegment segmentWithoutExternalAudience =
                defaultSegmentForUpdate().withType(AdShowType.MIDPOINT).withExternalAudienceId(0L);
        usersSegmentRepository.addSegments(shard, asList(segmentWithExternalAudience,
                segmentWithoutExternalAudience));

        List<UsersSegment> actualSegments = getAdGroupSegmentsForUpdateSegments();
        assertThat("должно было вернуться одно задание", actualSegments, hasSize(1));
        assertThat(actualSegments.get(0).getExternalAudienceId(), not(0L));
    }

    @Test
    public void updateSegment() {
        UsersSegment first = defaultSegment();
        usersSegmentRepository.addSegments(adGroup.getShard(), Collections.singletonList(first));

        // change
        first.setErrorCount(10L);
        first.setIsDisabled(true);
        first.setExternalAudienceStatus(ExternalAudienceStatus.IS_PROCESSED);
        usersSegmentRepository.updateSegment(adGroup.getShard(), first);

        UsersSegment segment = usersSegmentRepository
                .getSegmentByPrimaryKey(adGroup.getShard(), adGroup.getAdGroupId(), AdShowType.START);
        assertNotNull(segment);
        assertEquals(10L, (long) segment.getErrorCount());
        assertEquals(false, segment.getIsDisabled());
    }

    @Test
    public void updateSegmentsDisabledFlag_NotModifyEmptyList() {
        UsersSegment first = new UsersSegment().withAdGroupId(adGroup.getAdGroupId()).withType(AdShowType.START);
        usersSegmentRepository.addSegments(adGroup.getShard(), Collections.singletonList(first));
        DSLContext dslContext = dslContextProvider.ppc(adGroup.getShard());
        usersSegmentRepository.updateSegmentsDisabledFlag(dslContext, Collections.emptyList(), true);
        List<UsersSegment> segments = usersSegmentRepository
                .getSegments(adGroup.getShard(), Collections.singletonList(adGroup.getAdGroupId()));
        assertThat(segments, hasSize(1));
        UsersSegment inserted = segments.get(0);
        assertEquals(AdShowType.START, inserted.getType());
    }

    @Test
    public void updateSegmentsDisabledFlag() {
        UsersSegment first = new UsersSegment().withAdGroupId(adGroup.getAdGroupId()).withType(AdShowType.START);
        usersSegmentRepository.addSegments(adGroup.getShard(), Collections.singletonList(first));
        DSLContext dslContext = dslContextProvider.ppc(adGroup.getShard());
        usersSegmentRepository.updateSegmentsDisabledFlag(dslContext, Collections.singletonList(first), true);
        List<UsersSegment> segments = usersSegmentRepository
                .getSegments(adGroup.getShard(), Collections.singletonList(adGroup.getAdGroupId()));
        assertThat(segments, hasSize(0));
    }

    @Test
    public void getSegmentsForUpdateSegments_testOrder_OldSelectedFirst() {
        dslContextProvider.ppc(shard).truncate(VIDEO_SEGMENT_GOALS);
        UsersSegment first = defaultSegmentForUpdate()
                .withType(AdShowType.MIDPOINT)
                .withLastSuccessUpdateTime(LocalDateTime.of(2018, 1, 1, 0, 0));
        UsersSegment second = defaultSegmentForUpdate()
                .withType(AdShowType.FIRST_QUARTILE)
                .withLastSuccessUpdateTime(LocalDateTime.of(2019, 1, 1, 0, 0));
        usersSegmentRepository.addSegments(shard, asList(first, second));
        UsersSegment selected =
                usersSegmentRepository.getOldestSegmentsForUpdate(shard, adGroup.getAdGroupType(), 1).get(0);
        assertThat(selected, beanDiffer(first));
    }

    private UsersSegment defaultSegment() {
        return TestUserSegments.defaultSegment(adGroup.getAdGroupId(), AdShowType.START)
                .withLastSuccessUpdateTime(LocalDateTime.now());
    }

    private UsersSegment defaultSegmentForUpdate() {
        return TestUserSegments.defaultSegment(adGroup.getAdGroupId(), AdShowType.START)
                .withExternalAudienceStatus(ExternalAudienceStatus.PROCESSED)
                .withExternalAudienceId(123L)
                .withTimeCreated(LocalDateTime.now().withNano(0))
                .withLastSuccessUpdateTime(LocalDateTime.now().withNano(0));
    }

    private List<UsersSegment> getAdGroupSegmentsForCreateSegments() {
        return filterList(usersSegmentRepository.getOldestSegmentsForCreate(shard, adGroup.getAdGroupType(), 100),
                segment -> segment.getAdGroupId().equals(adGroup.getAdGroupId()));
    }

    private List<UsersSegment> getAdGroupSegmentsForUpdateSegments() {
        return filterList(usersSegmentRepository.getOldestSegmentsForUpdate(shard, adGroup.getAdGroupType(), 100),
                segment -> segment.getAdGroupId().equals(adGroup.getAdGroupId()));
    }
}
