package ru.yandex.direct.core.entity.userssegments.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.direct.core.entity.adgroup.AdGroupWithUsersSegmentsHelper.complexCpmAdGroupsToMap;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UsersSegmentServiceTest {

    @Autowired
    private UsersSegmentService usersSegmentService;

    @Autowired
    private UsersSegmentRepository usersSegmentRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Before
    public void setUp() {
        adGroup = adGroupSteps.createActiveTextAdGroup();
        adGroupId = adGroup.getAdGroupId();
    }

    private long adGroupId;
    private AdGroupInfo adGroup;

    @Test
    public void addSegments() {
        ComplexCpmAdGroup cpmVideoAdGroup =
                createCpmGroupWithSegments(singletonList(AdShowType.COMPLETE), adGroupId);
        usersSegmentService.addSegments(adGroup.getShard(), complexCpmAdGroupsToMap(singletonList(cpmVideoAdGroup)));
        UsersSegment videoGoal =
                usersSegmentRepository.getSegmentByPrimaryKey(adGroup.getShard(), adGroupId, AdShowType.COMPLETE);
        assertNotNull(videoGoal);
    }

    @Test
    public void addSegmentsWithNullAdGroupId() {
        List<UsersSegment> segments = new ArrayList<>();
        for (AdShowType type : singletonList(AdShowType.COMPLETE)) {
            segments.add(new UsersSegment().withType(type));
        }
        ComplexCpmAdGroup cpmVideoAdGroup =
                new ComplexCpmAdGroup()
                        .withAdGroup(new AdGroup().withId(adGroupId))
                        .withUsersSegments(segments);
        usersSegmentService.addSegments(adGroup.getShard(), complexCpmAdGroupsToMap(singletonList(cpmVideoAdGroup)));
        UsersSegment segment =
                usersSegmentRepository.getSegmentByPrimaryKey(adGroup.getShard(), adGroupId, AdShowType.COMPLETE);
        assertNotNull(segment);
    }

    @Test
    public void updateAdGroupsSegments() {
        ComplexCpmAdGroup cpmVideoAdGroup =
                createCpmGroupWithSegments(asList(AdShowType.COMPLETE, AdShowType.MIDPOINT), adGroupId);

        usersSegmentService.addSegments(adGroup.getShard(), complexCpmAdGroupsToMap(singletonList(cpmVideoAdGroup)));
        List<UsersSegment> segments = usersSegmentRepository.getSegments(adGroup.getShard(), singletonList(adGroupId));

        assertEquals(2, segments.size());

        ComplexCpmAdGroup cpmVideoAdGroupUpdate =
                createCpmGroupWithSegments(singletonList(AdShowType.COMPLETE), adGroupId);

        usersSegmentService.updateAdGroupsSegments(adGroup.getShard(), adGroup.getCampaignId(),
                complexCpmAdGroupsToMap(singletonList(cpmVideoAdGroupUpdate)));

        List<UsersSegment> videoGoalsAfterUpdate =
                usersSegmentRepository.getSegments(adGroup.getShard(), singletonList(adGroupId));

        assertEquals(1, videoGoalsAfterUpdate.size());
        assertEquals(AdShowType.COMPLETE, videoGoalsAfterUpdate.get(0).getType());
    }

    private static ComplexCpmAdGroup createCpmGroupWithSegments(Collection<AdShowType> types, Long adGroupId) {
        List<UsersSegment> segments = new ArrayList<>();
        for (AdShowType type : types) {
            segments.add(new UsersSegment().withType(type).withAdGroupId(adGroupId));
        }
        return new ComplexCpmAdGroup()
                .withAdGroup(new AdGroup().withId(adGroupId))
                .withUsersSegments(segments);
    }
}
