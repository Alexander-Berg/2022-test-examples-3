package ru.yandex.direct.jobs.segment.log;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

public class IntermediateSegmentYtRepositoryMock implements IntermediateSegmentYtRepository {

    private Map<LocalDate, Map<SegmentKey, List<BigInteger>>> dayLog = new HashMap<>();

    public void putData(LocalDate date, SegmentKey segmentKey, List<BigInteger> data) {
        dayLog.computeIfAbsent(date, d -> new HashMap<>()).put(segmentKey, data);
    }

    public void putEmptyData(LocalDate date) {
        dayLog.computeIfAbsent(date, d -> new HashMap<>());
    }

    @Override
    public Function<LocalDate, String> intermediateTablePathProvider() {
        return null;
    }

    @Override
    public LocalDate getMostFreshLogDate() {
        return StreamEx.of(dayLog.keySet())
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    @Override
    public LocalDate getOldestLogDate() {
        return StreamEx.of(dayLog.keySet())
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }

    @Override
    public Map<SegmentKey, Long> getCount(Collection<SegmentKey> segmentKeys, LocalDate from, LocalDate to) {
        Map<SegmentKey, Long> result = new HashMap<>();
        EntryStream.of(dayLog)
                .filterKeys(day -> day.isAfter(from) || day.isEqual(from))
                .filterKeys(day -> day.isBefore(to) || day.isEqual(to))
                .forKeyValue((date, dayData) -> {
                    segmentKeys.forEach(segmentKey -> {
                        List<BigInteger> segmentData = dayData.get(segmentKey);
                        if (!isEmpty(segmentData)) {
                            result.put(segmentKey, result.getOrDefault(segmentKey, 0L) + segmentData.size());
                        }
                    });
                });
        return result;
    }

    @Override
    public Map<SegmentKey, Set<BigInteger>> getData(Collection<SegmentKey> segmentKeys, LocalDate from, LocalDate to) {
        Map<SegmentKey, Set<BigInteger>> result = new HashMap<>();
        EntryStream.of(dayLog)
                .filterKeys(day -> day.isAfter(from) || day.isEqual(from))
                .filterKeys(day -> day.isBefore(to) || day.isEqual(to))
                .forKeyValue((date, dayData) -> {
                    segmentKeys.forEach(segmentKey -> {
                        List<BigInteger> segmentData = dayData.get(segmentKey);
                        if (!isEmpty(segmentData)) {
                            result.computeIfAbsent(segmentKey, sk -> new HashSet<>()).addAll(segmentData);
                        }
                    });
                });
        return result;
    }

    @Override
    public SegmentContentType getContentType() {
        return SegmentContentType.YUID;
    }
}
