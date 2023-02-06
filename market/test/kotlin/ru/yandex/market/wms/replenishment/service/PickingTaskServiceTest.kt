package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.model.enums.ReplenishmentType
import ru.yandex.market.wms.common.spring.IntegrationTest

class PickingTaskServiceTest : IntegrationTest() {

    @Autowired
    private val pickingTaskService: PickingTaskService? = null

    @Test
    @DatabaseSetup("/service/split-picking-tasks/1/before.xml")
    @ExpectedDatabase("/service/split-picking-tasks/1/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun splitPendingPickingTask() {
        assertions.assertThat(pickingTaskService?.splitPickingTask("0000000100")).isEqualTo(2)
    }

    @Test
    @DatabaseSetup("/service/split-picking-tasks/2/before.xml")
    @ExpectedDatabase("/service/split-picking-tasks/2/after.xml", assertionMode = NON_STRICT_UNORDERED)
    fun splitInProcessPickingTask() {
        assertions.assertThat(pickingTaskService?.splitPickingTask("0000000100")).isEqualTo(2)
    }

    @Test
    @DatabaseSetup("/service/split-picking-tasks/3/before.xml")
    @ExpectedDatabase("/service/split-picking-tasks/3/before.xml", assertionMode = NON_STRICT_UNORDERED)
    fun noSplitInWrongStatusPickingTask() {
        assertions.assertThatThrownBy { pickingTaskService?.splitPickingTask("0000000100") }
            .hasMessage("400 BAD_REQUEST \"Task 0000000100 has unexpected status HELD_BY_SYSTEM\"")
        assertions.assertThatThrownBy { pickingTaskService?.splitPickingTask("0000000201") }
            .hasMessage("400 BAD_REQUEST \"Task 0000000201 has unexpected status COMPLETED\"")
        assertions.assertThatThrownBy { pickingTaskService?.splitPickingTask("0000000202") }
            .hasMessage("400 BAD_REQUEST \"Task 0000000202 has unexpected status CANCELLED\"")
        assertions.assertThatThrownBy { pickingTaskService?.splitPickingTask("NONE") }
            .hasMessage("Задание не найдено: NONE")

    }

    @Test
    @DatabaseSetup("/service/split-picking-tasks/4/before.xml")
    @ExpectedDatabase("/service/split-picking-tasks/4/before.xml", assertionMode = NON_STRICT_UNORDERED)
    fun noSplitOnMinimumLeftQty() {
        assertions.assertThat(pickingTaskService?.splitPickingTask("0000000100")).isZero
        assertions.assertThat(pickingTaskService?.splitPickingTask("0000000101")).isZero
    }

    @Test
    @DatabaseSetup("/service/get-tasks/1/before.xml")
    fun getByPriority() {
        assertions
            .assertThat(pickingTaskService?.getPickingTask("AREA1", ReplenishmentType.ORDER)?.taskKey)
            .isEqualTo("TASK15")
    }
}
