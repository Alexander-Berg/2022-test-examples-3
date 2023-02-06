package ru.yandex.market.abo.logbroker.logistic.lms

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.ADDRESS
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.PHONE
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.SCHEDULE
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.SHIPMENT_TYPE

/**
 * @author zilzilok
 */
class WarehouseChangedTest {

    @ParameterizedTest
    @MethodSource("warehouse changes method source")
    fun `check change type`(jsonDiffs: String, expectedTypes: List<WarehouseChangeValue>) {
        val diffs = MAPPER.readTree(jsonDiffs)

        Assertions.assertEquals(
            expectedTypes,
            enumValues<WarehouseChangeValue>().filter { it.isSomethingChanged(diffs) }
        )
    }

    companion object {
        private val MAPPER = ObjectMapper()

        @JvmStatic
        fun `warehouse changes method source`(): Iterable<Arguments> = listOf(
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": "123001, Москва, Москва, улица Спиридоновка, д. 14",
                    "path": "/address/addressString",
                    "value": "123001, Москва, Москва, улица Спиридоновка, д. 18"
                  }
                ]
                """, listOf(ADDRESS)
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": 0.11,
                    "path": "/address/latitude",
                    "value": 0.21
                  },
                  {
                    "op": "replace",
                    "fromValue": 0.12,
                    "path": "/address/longitude",
                    "value": 0.22
                  }
                ]
                """, listOf<WarehouseChangeValue>()
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": "+78005553535",
                    "path": "/phones/0/number",
                    "value": "+788888888"
                  }
                ]
                """, listOf(PHONE)
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": "+78005553535",
                    "path": "/phones/1/internalNumber",
                    "value": "+788888888"
                  }
                ]
                """, listOf(PHONE)
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": "12:00:00",
                    "path": "/schedule/0/timeTo",
                    "value": "15:00:00"
                  }
                ]
                """, listOf(SCHEDULE)
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": "WITHDRAW",
                    "path": "/shipmentType",
                    "value": "EXPRESS"
                  }
                ]
                """, listOf(SHIPMENT_TYPE)
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": null,
                    "path": "/shipmentType",
                    "value": "EXPRESS"
                  }
                ]
                """, listOf<WarehouseChangeValue>()
            ),
            Arguments.of(
                """
                [
                  {
                    "op": "replace",
                    "fromValue": "123001, Москва, Москва, улица Спиридоновка, д. 14",
                    "path": "/address/addressString",
                    "value": "123001, Москва, Москва, улица Спиридоновка, д. 18"
                  },
                  {
                    "op": "replace",
                    "fromValue": "+78005553535",
                    "path": "/phones/0/number",
                    "value": "+788888888"
                  },
                  {
                    "op": "replace",
                    "fromValue": "12:00:00",
                    "path": "/schedule/0/timeTo",
                    "value": "15:00:00"
                  },
                  {
                    "op": "replace",
                    "fromValue": "WITHDRAW",
                    "path": "/shipmentType",
                    "value": "EXPRESS"
                  }
                ]
                """, listOf(ADDRESS, PHONE, SCHEDULE, SHIPMENT_TYPE)
            )
        )
    }
}
