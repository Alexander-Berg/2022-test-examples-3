package ru.yandex.market.logistics.mqm.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.ChangeOrderRequestType

@DisplayName("Тест сервиса заявок изменения заказа")
class ChangeOrderRequestServiceImplTest: AbstractContextualTest() {
    @Autowired
    private lateinit var changeOrderRequestService: ChangeOrderRequestService

    @Test
    @DisplayName("Проверка успешной загрузки по идентификаторам")
    @DatabaseSetup("/service/change_order_request_service/before/find_by_ids.xml")
    fun successLoadByIds() {
        val requests = changeOrderRequestService.findByIdsWithPayloads(listOf(21031272L))

        val request = requests.single()

        request.changeOrderRequestPayloads.size shouldBe 2
    }

    @Test
    @DisplayName("Проверка findByOrderIdWithTypes")
    @DatabaseSetup("/service/change_order_request_service/before/two_change_rdd_cor.xml")
    fun successFindLastSuccessDeliveryDateChange() {
        val changeOrderRequests = changeOrderRequestService.findByOrderIdWithTypes(
            orderId = 1,
            requestTypes = setOf(ChangeOrderRequestType.RECALCULATE_ROUTE_DATES)
        )
        assertSoftly {
            changeOrderRequests shouldNotBe null
            changeOrderRequests.size shouldBe 1
            changeOrderRequests.first().requestType shouldBe ChangeOrderRequestType.RECALCULATE_ROUTE_DATES
        }
    }

    @Test
    @DisplayName("Проверка findByOrderIdWithTypes, вернуть пустой список если нет cor")
    fun findLastSuccessDeliveryDateChangeReturnNull() {
        val changeOrderRequests = changeOrderRequestService.findByOrderIdWithTypes(
            orderId = 1,
            requestTypes = setOf(ChangeOrderRequestType.RECALCULATE_ROUTE_DATES)
        )
        changeOrderRequests shouldBe emptyList()
    }
}
