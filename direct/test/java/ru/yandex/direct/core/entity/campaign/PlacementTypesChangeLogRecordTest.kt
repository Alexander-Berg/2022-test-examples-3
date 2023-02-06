package ru.yandex.direct.core.entity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.core.entity.campaign.model.PlacementType
import ru.yandex.direct.utils.fromJson

@RunWith(Parameterized::class)
internal class PlacementTypesChangeLogRecordTest(
    @Suppress("unused") private val name: String,
    private val oldState: Set<PlacementType>?,
    private val newState: Set<PlacementType>?,
    private val expected: Map<String, Any>
) {
    companion object {
        const val OPERATOR_UID = 3478273225L
        const val CAMPAIGN_ID = 44444455544L

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): Iterable<Array<Any?>> {
            return arrayListOf(
                arrayOf("from empty", emptySet<PlacementType>(), setOf(PlacementType.SEARCH_PAGE),
                    mapOf(
                        "cids" to listOf(CAMPAIGN_ID),
                        "uid" to OPERATOR_UID,
                        "param" to mapOf(
                            "cid" to CAMPAIGN_ID,
                            "old_placement_types" to listOf<String>(),
                            "new_placement_types" to listOf("SEARCH_PAGE")
                        )
                    )
                ),
                arrayOf("to empty", setOf(PlacementType.ADV_GALLERY), emptySet<PlacementType>(),
                    mapOf(
                        "cids" to listOf(CAMPAIGN_ID),
                        "uid" to OPERATOR_UID,
                        "param" to mapOf(
                            "cid" to CAMPAIGN_ID,
                            "old_placement_types" to listOf("ADV_GALLERY"),
                            "new_placement_types" to listOf<String>()
                        )
                    )
                ),
                arrayOf("from null", null, setOf(PlacementType.ADV_GALLERY),
                    mapOf(
                        "cids" to listOf(CAMPAIGN_ID),
                        "uid" to OPERATOR_UID,
                        "param" to mapOf(
                            "cid" to CAMPAIGN_ID,
                            "old_placement_types" to null,
                            "new_placement_types" to listOf("ADV_GALLERY")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun formatLogRecordTest() {
        val logRecord = PlacementTypesChangeLogRecord(OPERATOR_UID, CAMPAIGN_ID, oldState, newState)
        val formatLogRecord = logRecord.formatLogRecord()
        val json = formatLogRecord.substring(20) // отрежем время
        val parsed = fromJson<Map<String, Any>>(json)
        assertThat(parsed).containsAllEntriesOf(expected)
    }
}
