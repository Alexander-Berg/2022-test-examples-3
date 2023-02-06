package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.util.List;

import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus;
import ru.yandex.direct.core.entity.adgroup.model.InternalStatus;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;

import static java.util.Arrays.asList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class TestUserSegments {

    public static UsersSegment readyForCreateSegment(long adGroupId) {
        return new UsersSegment()
                .withAdGroupId(adGroupId)
                .withType(AdShowType.START)
                .withTimeCreated(LocalDateTime.now().minusDays(500))
                .withLastSuccessUpdateTime(LocalDateTime.now().minusDays(500))
                .withErrorCount(0L)
                .withExternalAudienceId(0L)
                .withSegmentOwnerUid(0L)
                .withIsDisabled(false)
                .withInternalStatus(InternalStatus.NEW_)
                .withExternalAudienceStatus(ExternalAudienceStatus.FEW_DATA);
    }

    public static UsersSegment readyForUpdateSegment(long adGroupId, Long segmentOwnerUid) {
        return new UsersSegment()
                .withAdGroupId(adGroupId)
                .withType(AdShowType.START)
                .withTimeCreated(LocalDateTime.now().minusDays(500))
                .withLastSuccessUpdateTime(LocalDateTime.now().minusDays(2))
                .withErrorCount(0L)
                .withExternalAudienceId(123L)
                .withSegmentOwnerUid(segmentOwnerUid)
                .withIsDisabled(false)
                .withInternalStatus(InternalStatus.COMPLETE)
                .withExternalAudienceStatus(ExternalAudienceStatus.PROCESSED);
    }

    public static UsersSegment defaultSegment(long adGroupId, AdShowType type) {
        return fillSegmentSystemFields(new UsersSegment()).withAdGroupId(adGroupId).withType(type);
    }

    public static UsersSegment fillSegmentSystemFields(UsersSegment segment) {
        return segment.withTimeCreated(LocalDateTime.now())
                .withErrorCount(0L)
                .withExternalAudienceId(0L)
                .withSegmentOwnerUid(0L)
                .withIsDisabled(false)
                .withInternalStatus(InternalStatus.NEW_)
                .withExternalAudienceStatus(ExternalAudienceStatus.FEW_DATA);
    }

    public static List<UsersSegment> createAllTypesSegments() {
        return segmentsFromTypes(
                AdShowType.START,
                AdShowType.FIRST_QUARTILE,
                AdShowType.MIDPOINT,
                AdShowType.MIDPOINT,
                AdShowType.COMPLETE);
    }

    public static List<UsersSegment> segmentsFromTypes(AdShowType... adShowTypes) {
        return mapList(asList(adShowTypes), adShowType -> new UsersSegment().withType(adShowType));
    }
}
