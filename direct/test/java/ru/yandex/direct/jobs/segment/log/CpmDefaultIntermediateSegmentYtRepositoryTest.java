package ru.yandex.direct.jobs.segment.log;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.segment.common.meta.SegmentKey.segmentKey;
import static ru.yandex.direct.jobs.segment.log.cpmdefault.CpmDefaultIntermediateSegmentYtRepository.buildCountQuery;
import static ru.yandex.direct.jobs.segment.log.cpmdefault.CpmDefaultIntermediateSegmentYtRepository.buildDataQuery;

public class CpmDefaultIntermediateSegmentYtRepositoryTest {

    private static final String TABLE_PATH = "//home/direct/segments/1d";
    private static final LocalDate DATE_FROM = LocalDate.of(2021, 8, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2022, 9, 12);

    private static final String RESULT_COUNT_NON_EMPTY_SEGMENTS = "$data = AsList(\n" +
            "    AsStruct('928749875' as groupexportid, 'first-quartile' as action),\n" +
            "    AsStruct('34872463928385' as groupexportid, 'third-quartile' as action),\n" +
            "    AsStruct('7182977389' as groupexportid, 'complete' as action)\n" +
            ");\n" +
            "\n" +
            "select t.groupexportid as groupexportid, t.action as action, count(*)\n" +
            "from range(`//home/direct/segments/1d`, `2021-08-01`, `2022-09-12`) as t\n" +
            "join as_table($data) as d on t.groupexportid = d.groupexportid and t.action = d.action\n" +
            "group by t.groupexportid, t.action;";
    private static final String RESULT_DATA_NON_EMPTY_SEGMENTS = "$data = AsList(\n" +
            "    AsStruct('928749875' as groupexportid, 'first-quartile' as action),\n" +
            "    AsStruct('34872463928385' as groupexportid, 'third-quartile' as action),\n" +
            "    AsStruct('7182977389' as groupexportid, 'complete' as action)\n" +
            ");\n" +
            "\n" +
            "select t.groupexportid as groupexportid, t.action as action, uniqid\n" +
            "from range(`//home/direct/segments/1d`, `2021-08-01`, `2022-09-12`) as t\n" +
            "join as_table($data) as d on t.groupexportid = d.groupexportid and t.action = d.action;";

    @Test
    public void buildValidCountQueryOnNonEmptySegments() {
        List<SegmentKey> segmentKeys = List.of(
                segmentKey(928749875L, AdShowType.FIRST_QUARTILE),
                segmentKey(34872463928385L, AdShowType.THIRD_QUARTILE),
                segmentKey(7182977389L, AdShowType.COMPLETE)
        );
        String query = buildCountQuery(TABLE_PATH, segmentKeys, DATE_FROM, DATE_TO);
        assertThat(query).isEqualTo(RESULT_COUNT_NON_EMPTY_SEGMENTS);
    }

    @Test
    public void buildValidDataQueryOnNonEmptySegments() {
        List<SegmentKey> segmentKeys = List.of(
                segmentKey(928749875L, AdShowType.FIRST_QUARTILE),
                segmentKey(34872463928385L, AdShowType.THIRD_QUARTILE),
                segmentKey(7182977389L, AdShowType.COMPLETE)
        );
        String query = buildDataQuery(TABLE_PATH, segmentKeys, DATE_FROM, DATE_TO);
        assertThat(query).isEqualTo(RESULT_DATA_NON_EMPTY_SEGMENTS);
    }
}
