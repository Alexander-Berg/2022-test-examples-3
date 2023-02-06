package ru.yandex.direct.jobs.segment.log

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.adgroup.model.AdShowType
import ru.yandex.direct.jobs.segment.common.meta.SegmentKey
import ru.yandex.direct.jobs.segment.log.common.CommonIntermediateSegmentYtRepository.Companion.buildCountQuery
import ru.yandex.direct.jobs.segment.log.common.CommonIntermediateSegmentYtRepository.Companion.buildDataQuery
import java.time.LocalDate

class CommonIntermediateSegmentYtRepositoryTest {
    companion object {
        private const val TABLE_PATH = "//home/direct/segments/common/1d"
        private val DATE_FROM = LocalDate.of(2021, 8, 1)
        private val DATE_TO = LocalDate.of(2022, 9, 12)

        private val RESULT_COUNT_NON_EMPTY_SEGMENTS = """
            ${"$"}data = AsList(
                AsStruct(928749875 as groupexportid),
                AsStruct(34872463928385 as groupexportid)
            );
            
            select t.groupexportid as groupexportid, count(*)
            from range(`//home/direct/segments/common/1d`, `2021-08-01`, `2022-09-12`) as t
            join as_table(${"$"}data) as d on t.groupexportid = d.groupexportid
            group by t.groupexportid;
            """.trimIndent()
            
        private val RESULT_DATA_NON_EMPTY_SEGMENTS = """
            ${"$"}data = AsList(
                AsStruct(928749875 as groupexportid),
                AsStruct(34872463928385 as groupexportid)
            );
            
            select t.groupexportid as groupexportid, cryptaidv2
            from range(`//home/direct/segments/common/1d`, `2021-08-01`, `2022-09-12`) as t
            join as_table(${"$"}data) as d on t.groupexportid = d.groupexportid;
            """.trimIndent()
    }

    @Test
    fun buildValidCountQueryOnNonEmptySegments() {
        val segmentKeys = listOf(
            SegmentKey.segmentKey(928749875L, AdShowType.START),
            SegmentKey.segmentKey(34872463928385L, AdShowType.START),
        )
        val query = buildCountQuery(TABLE_PATH, segmentKeys, DATE_FROM, DATE_TO)
        Assertions.assertThat(query).isEqualTo(RESULT_COUNT_NON_EMPTY_SEGMENTS)
    }

    @Test
    fun buildValidDataQueryOnNonEmptySegments() {
        val segmentKeys = listOf(
            SegmentKey.segmentKey(928749875L, AdShowType.START),
            SegmentKey.segmentKey(34872463928385L, AdShowType.START),
        )
        val query = buildDataQuery(TABLE_PATH, segmentKeys, DATE_FROM, DATE_TO)
        Assertions.assertThat(query).isEqualTo(RESULT_DATA_NON_EMPTY_SEGMENTS)
    }
}
