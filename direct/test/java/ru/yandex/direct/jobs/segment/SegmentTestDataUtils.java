package ru.yandex.direct.jobs.segment;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;
import ru.yandex.direct.jobs.segment.log.SegmentSourceData;

import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForUpdateSegment;
import static ru.yandex.direct.jobs.segment.common.SegmentUtils.segmentKeyExtractor;
import static ru.yandex.direct.jobs.segment.log.SegmentSourceData.sourceData;

public class SegmentTestDataUtils {
    private SegmentTestDataUtils() {
        // no instantiating
    }

    public static UsersSegment usersSegment(LocalDate lastReadLogDate) {
        return usersSegment(123L, lastReadLogDate);
    }

    public static UsersSegment usersSegment(Long adGroupId, LocalDate lastReadLogDate) {
        return usersSegment(adGroupId, AdShowType.COMPLETE, lastReadLogDate);
    }

    public static UsersSegment usersSegment(Long adGroupId, AdShowType type, LocalDate lastReadLogDate) {
        return readyForUpdateSegment(adGroupId, 12345L)
                .withType(type)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());
    }

    public static SegmentKey segmentKey(UsersSegment segment) {
        return segmentKeyExtractor().apply(segment);
    }

    public static SegmentSourceData oneSegmentSourceData(SegmentKey segmentKey, Set<BigInteger> uids) {
        return sourceData(List.of(segmentKey), Map.of(segmentKey, uids),
                LocalDate.now(), LocalDate.now(), SegmentContentType.YUID);
    }

    public static SegmentSourceData oneSegmentSourceData(
            SegmentKey segmentKey, Set<BigInteger> uids, LocalDate lastReadLogDate) {
        return sourceData(List.of(segmentKey), Map.of(segmentKey, uids),
                lastReadLogDate, LocalDate.now(), SegmentContentType.YUID);
    }
}
