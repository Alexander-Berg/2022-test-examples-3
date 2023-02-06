package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.dto.ServiceCenterItemToSendUpdateDTO
import ru.yandex.market.logistics.cte.client.dto.ServiceCenterItemsToSendUpdateDTO
import ru.yandex.market.logistics.cte.client.dto.UpdateServiceCenterItemsToSendErrorCode
import ru.yandex.market.logistics.cte.client.enums.ServiceCenterItemStatus
import java.time.LocalDateTime

internal class ServiceCenterItemsToSendServiceTest(
    @Autowired private val serviceCenterItemsToSendService: ServiceCenterItemsToSendService
) : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/service-center-items-to-send/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/service-center-items-to-send/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun updateItemsOkTest() {
        val item1 = ServiceCenterItemToSendUpdateDTO(
            itemId = 1,
            productAppearance = "нормально",
            components =  "полная",
            serviceCenterId =  3L,
            serviceCenterSentAt = LocalDateTime.of(2022, 5, 27, 12, 0, 0, 0),
            serviceCenterReceivedAt = LocalDateTime.of(2022, 5, 27, 14, 0, 0, 0),
            status = ServiceCenterItemStatus.PROCESSING,
            serialNumber = "N111",
            imei = "000-111",
            defectDescription = "Не подходит",
            serviceCenterResultId = 1
        )
        val item2 = ServiceCenterItemToSendUpdateDTO(
            itemId = 2,
            productAppearance = "удовлетворительно",
            components = "нет упаковки", 3L,
            serviceCenterSentAt = LocalDateTime.of(2022, 5, 26, 12, 0, 0, 0),
            serviceCenterReceivedAt = LocalDateTime.of(2022, 5, 26, 15, 0, 0, 0),
            status = ServiceCenterItemStatus.PROCESSING,
            serialNumber = "N222",
            imei = "000-222",
            defectDescription = "Был сломан",
            serviceCenterResultId = 1
        )
        val serviceCenterItemsToSendUpdateDTO = ServiceCenterItemsToSendUpdateDTO(listOf(item1, item2))

        serviceCenterItemsToSendService.updateItems(serviceCenterItemsToSendUpdateDTO)
    }

    @Test
    @DatabaseSetup("classpath:service/service-center-items-to-send-when-forbidden-delete/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/service-center-items-to-send-when-forbidden-delete/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun updateItemsWhenForbiddenToDeleteTest() {

        val item1 = ServiceCenterItemToSendUpdateDTO(
            itemId = 1,
            productAppearance = "нормально",
            components =  "полная",
            serviceCenterId =  3L,
            serviceCenterSentAt = LocalDateTime.of(2022, 5, 27, 12, 0, 0, 0),
            serviceCenterReceivedAt = LocalDateTime.of(2022, 5, 27, 14, 0, 0, 0),
            status = ServiceCenterItemStatus.DELETED,
            serialNumber = "N111",
            imei = "000-111",
            defectDescription = "Не подходит",
            serviceCenterResultId = 1
        )
        val item2 = ServiceCenterItemToSendUpdateDTO(
            itemId = 2,
            productAppearance = "удовлетворительно",
            components = "нет упаковки", 3L,
            serviceCenterSentAt = LocalDateTime.of(2022, 5, 26, 12, 0, 0, 0),
            serviceCenterReceivedAt = LocalDateTime.of(2022, 5, 26, 15, 0, 0, 0),
            status = ServiceCenterItemStatus.DELETED,
            serialNumber = "N222",
            imei = "000-222",
            defectDescription = "Был сломан",
            serviceCenterResultId = null
        )

        val serviceCenterItemsToSendUpdateDTO = ServiceCenterItemsToSendUpdateDTO(listOf(item1, item2))

        val updateItemsResponse = serviceCenterItemsToSendService.updateItems(serviceCenterItemsToSendUpdateDTO)

        assertions.assertThat(updateItemsResponse.updatedItemsCount).isEqualTo(2)
        assertions.assertThat(updateItemsResponse.errors[0].code).isEqualTo(UpdateServiceCenterItemsToSendErrorCode.INVALID_STATUS_TO_DELETE)
        assertions.assertThat(updateItemsResponse.errors[0].message)
            .isEqualTo("Status change was skipped for the item 2. It's forbidden to change the status from SENT_TO_ASC to DELETED")
    }

    @Test
    @DatabaseSetup("classpath:service/service-center-items-to-send/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/service-center-items-to-send/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun updateItemsWhenEmptyInputTest() {
        val item1 = ServiceCenterItemToSendUpdateDTO(
            itemId = 1,
            productAppearance = "",
            components = null,
            serviceCenterId = null,
            serviceCenterSentAt = null,
            serviceCenterReceivedAt = null,
            status = null,
            serialNumber = null,
            imei = null,
            defectDescription = null,
            serviceCenterResultId = null
        )
        val serviceCenterItemsToSendUpdateDTO = ServiceCenterItemsToSendUpdateDTO(listOf(item1))

        serviceCenterItemsToSendService.updateItems(serviceCenterItemsToSendUpdateDTO)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:service/service-center-items-to-send/before_task_items.xml"),
        DatabaseSetup("classpath:service/service-center-items-to-send/before_tasks.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase("classpath:service/service-center-items-to-send/after_task_items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
        ExpectedDatabase("classpath:service/service-center-items-to-send/after_tasks_decrement_first.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
    )
    fun shouldDecrementCounter() {
        val taskCounter = serviceCenterItemsToSendService.getTasksCountOrThrow(1)
        assertions.assertThat(taskCounter).isEqualTo(2)
        val newTaskCounter = serviceCenterItemsToSendService.decrementCounterAndUpdateStatus(listOf(1, 2), 1)
        assertions.assertThat(newTaskCounter).isEqualTo(1)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:service/service-center-items-to-send/before_task_items.xml"),
        DatabaseSetup("classpath:service/service-center-items-to-send/before_tasks.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase("classpath:service/service-center-items-to-send/after_task_items_update_status.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
        ExpectedDatabase("classpath:service/service-center-items-to-send/after_tasks_update_status.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
    )
    fun shouldDecrementCounterAndChangeStatus() {
        val taskCounter = serviceCenterItemsToSendService.getTasksCountOrThrow(2)
        assertions.assertThat(taskCounter).isEqualTo(1)
        val newTaskCounter = serviceCenterItemsToSendService.decrementCounterAndUpdateStatus(listOf(1, 2), 2)
        assertions.assertThat(newTaskCounter).isEqualTo(0)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:service/service-center-items-to-send/before_task_items.xml"),
        DatabaseSetup("classpath:service/service-center-items-to-send/before_tasks.xml"),
    )
    @ExpectedDatabases(
        ExpectedDatabase("classpath:service/service-center-items-to-send/after_task_items_update_status.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
        ExpectedDatabase("classpath:service/service-center-items-to-send/before_tasks.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
    )
    fun shouldNotDecrementZeroCounter() {
        val taskCounter = serviceCenterItemsToSendService.getTasksCountOrThrow(3)
        assertions.assertThat(taskCounter).isEqualTo(0)
        val newTaskCounter = serviceCenterItemsToSendService.decrementCounterAndUpdateStatus(listOf(1, 2), 3)
        assertions.assertThat(newTaskCounter).isEqualTo(0)
    }

}
