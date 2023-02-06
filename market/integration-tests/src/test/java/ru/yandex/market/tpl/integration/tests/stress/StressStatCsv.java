package ru.yandex.market.tpl.integration.tests.stress;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;

@Log4j2
@RequiredArgsConstructor
public class StressStatCsv {
    private final StressStat stressStat;

    public String generalStatCsv() {
        return buildReport(Map.of("ALL REQUESTS", stressStat.getRequests()));
    }

    public String statByRequestsCsv() {
        Map<String, List<RequestStat>> requestsByEndpoint =
                stressStat.getRequests().stream().collect(Collectors.groupingBy(RequestStat::getEndpoint));
        String s = buildReport(requestsByEndpoint);
        log.info(s);
        return s;
    }

    private String buildReport(Map<String, ? extends Collection<RequestStat>> requestsByEndpoint) {
        var csv = new StringBuilder();
        csv.append("\"endpoint\",\"timestamp\",\"requests\",\"httpStatus200\",\"durationMs99\"\n");
        requestsByEndpoint.forEach((endpoint, stats) -> buildAndAppendIntoTheReport(csv, endpoint, stats));
        return csv.toString();
    }

    private void buildAndAppendIntoTheReport(StringBuilder csv, String endpoint, Collection<RequestStat> requestStats) {
        long globalStartTimestamp = requestStats.stream().mapToLong(RequestStat::getStartTimestamp).min().orElseThrow();
        long globalStopTimestamp = requestStats.stream().mapToLong(RequestStat::getStopTimestamp).max().orElseThrow();
        for (long timestamp = globalStartTimestamp; timestamp < globalStopTimestamp; timestamp += 1000) {
            long requests = 0;
            long httpStatus200 = 0;
            List<Long> durations = new ArrayList<>();
            long durationMs99;
            for (RequestStat requestStat : requestStats) {
                if (requestStat.getStartTimestamp() >= timestamp && requestStat.getStartTimestamp() < timestamp + 1000) {
                    requests++;
                    durations.add(requestStat.getDurationMs());
                    if (requestStat.getHttpStatus() == 200) {
                        httpStatus200++;
                    }
                }
            }
            durations.sort(Comparator.naturalOrder());
            durationMs99 = durations.size() == 0 ? 0 : durations.get((int) ((durations.size() - 1.0) * 0.99));
            if (requests != 0) {
                csv.append(String.format("%s, %s,%d,%d,%d\n",
                        endpoint,
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), MOSCOW_ZONE),
                        requests,
                        httpStatus200,
                        durationMs99
                ));
            }
        }
    }
}
