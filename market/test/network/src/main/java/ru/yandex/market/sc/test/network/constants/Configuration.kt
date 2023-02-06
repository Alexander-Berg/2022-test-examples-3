package ru.yandex.market.sc.test.network.constants

import ru.yandex.market.sc.core.data.courier.Courier
import ru.yandex.market.sc.core.data.warehouse.Warehouse
import ru.yandex.market.sc.core.utils.data.ExternalId

object Configuration {
    const val SORTING_CENTER_ID = 3L
    const val SORTING_CENTER_XDOC_ID = 2000L
    const val SORTING_CENTER_MIDDLE_MILE_ID = 59549L
    const val SORTING_CENTER_PVZ_ID = 1005555L

    val WAREHOUSE_YANDEX_ID = ExternalId("warehouse-cats-world")
    val LOT_WAREHOUSE_YANDEX_ID = ExternalId("demo-tpl-sc-wh-yandexId")
    val WAREHOUSE_SHOP_YANDEX_ID = ExternalId("warehouse-e2e-yandex-id")

    const val COURIER_ID = 1408083169L
    const val COURIER_FOR_DISPATCH_ALL_ORDERS = 567890876520L
    const val COURIER_FOR_DISPATCH_WITH_RESORTING = 5674393494L
    const val COURIER_WITH_PARTIAL_ORDER = 8654032743L
    const val COURIER_WITH_EXTRA_ORDER = 3285468922L
    const val COURIER_WITH_LOTS_ONLY = 8290432943L
    const val COURIER_WITH_UNREADY_LOT = 7439035753L
    const val COURIER_WITH_READY_LOT = 9027803378L
    const val COURIER_FOR_CHECK_LIST = 7894028903L

    val LOGISTIC_POINT_TO_EXTERNAL_ID = ExternalId("1")
    val LOGISTIC_POINT_XDOC_NEXT_ID = ExternalId("10000004403")
    const val DELIVERY_SERVICE_MIDDLE_MILE_ID = "239"

    val couriers: Map<Long, Courier> = mapOf(
        COURIER_ID to Courier(id = COURIER_ID, uid = COURIER_ID, name = "Хидео Кадзима"),
        COURIER_FOR_CHECK_LIST to Courier(
            id = COURIER_FOR_CHECK_LIST,
            uid = COURIER_FOR_CHECK_LIST,
            name = "Курьер для проверки списка заказов"
        ),
    )

    val warehouses: Map<ExternalId, Warehouse> = mapOf(
        WAREHOUSE_SHOP_YANDEX_ID to Warehouse(
            id = 941,
            name = "Стандартный склад",
            type = Warehouse.Type.UNKNOWN
        ),
        WAREHOUSE_YANDEX_ID to Warehouse(
            id = 1767,
            name = "Мир котиков",
            type = Warehouse.Type.UNKNOWN
        ),
    )

    val sortingCenters: Map<Long, MockSortingCenter> = mapOf(
        SORTING_CENTER_ID to MockSortingCenter(
            id = SORTING_CENTER_ID,
            partnerId = 1001238768L,
            name = "СЦ E2E тесты",
            token = "e2e_test_token",
            credentials = AccountCredentials("yndx-sc-e2e-3@yandex.ru", "CjF6.9Nj2")
        ),
        SORTING_CENTER_XDOC_ID to MockSortingCenter(
            id = SORTING_CENTER_XDOC_ID,
            partnerId = 1001426218L,
            name = "РЦ Шляпа Гриффиндора",
            token = "qwerty",
            credentials = AccountCredentials("yndx-sc-e2e-2000@yandex.ru", "FrDM.DCsf")
        ),
        SORTING_CENTER_MIDDLE_MILE_ID to MockSortingCenter(
            id = SORTING_CENTER_MIDDLE_MILE_ID,
            partnerId = 1001065556L,
            name = "СЦ МК Киров",
            token = "TxjUC5Nvt9Nq2IurSgHCKZyBzWTcMOcpbjjUAhpBZ8UJEM9Qy4vRCT5o4a0IGqkj",
            credentials = AccountCredentials("yndx-sc-e2e-59549@yandex.ru", "KQl8.OYiN")
        ),
        SORTING_CENTER_PVZ_ID to MockSortingCenter(
            id = SORTING_CENTER_PVZ_ID,
            partnerId = 1001137159L,
            name = "Валин ПВЗ",
            token = "pvt_test",
            credentials = AccountCredentials("yndx-sc-e2e-1005555@yandex.ru", "Wb0p.LsS5")
        )
    )
}
