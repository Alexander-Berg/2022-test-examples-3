package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.yandex.market.wms.common.model.enums.ReplenishmentType
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.utils.uuid.FixedListTestUuidGenerator
import ru.yandex.market.wms.replenishment.config.OrderReplenishmentTestConfig

@SpringBootTest(classes = [IntegrationTestConfig::class, OrderReplenishmentTestConfig::class])
class OrderReplenishmentServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var replenishmentService: ReplenishmentService
    @Autowired
    private lateinit var moveTaskService: MoveTaskService
    @Autowired
    private lateinit var listUuidGenerator: FixedListTestUuidGenerator

    @BeforeEach
    private fun resetUuidGen() {
        listUuidGenerator.reset()
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order one item`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/1.1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/1.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `several orders one item`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/1.2/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/1.2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order several items`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/2/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order one item two ids at one location`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/7/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/7/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should be up task two ids at one location same sku`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/8/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/8/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `should be up task two ids at one location different sku`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/9/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/9/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `zero balance test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/3/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `several orders`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/4/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `not valid orders`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/create-tasks/5/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `down task priority test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 1 шт  на паллете 10 шт
     * Есть 1 новая проблема для SKU01 на 1шт
     * Ожидание 1 итоговая задача на 2шт с одной паллеты
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 1 шт  на паллете 10 шт
     * Есть 3 новых проблемы для SKU01 на 2 шт каждая
     * Ожидание 1 итоговая задача на 7шт с одной паллеты
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/2/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 1 шт  на паллете 10 шт
     * Есть 1 новая проблема для SKU01 на 1шт
     * Ожидание REPL_PICK меняет тип на REPL_ORD_PK 1 итоговая задача на 1шт с одной паллеты
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/3/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 3`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 1 шт
     * на паллете PALLETE01 в таре ID01 SKU01 10 шт
     * на паллете PALLETE01 в таре ID01 SKU02 10 шт
     * на паллете PALLETE01 в таре ID01 SKU03 10 шт
     * Есть 1 новая проблема для SKU03 на 1шт
     * Ожидание REPL_PICK меняет тип на REPL_ORD_PK, REP_MOVE на REP_ORD_MV, добавляется задача REP_ORD_PK на SKU03
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/3.1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/3.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test exist task for other sku in same id`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 4 шт  на паллете 10 шт
     * Есть 3 новых проблемы для SKU01 на 1,2,3 шт
     * Ожидание REPL_PICK меняет тип на REPL_ORD_PK 1 итоговая задача на 6шт с одной паллеты
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/4/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 4`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 1 шт  на паллете 3 шт ( нет стоков на других ячейках)
     * Есть 3 новых проблемы для SKU01 на 1,2,3 шт
     * Ожидание REPL_PICK меняет тип на REPL_ORD_PK 1 итоговая задача на 3шт с одной паллеты, 1 проблема в OUT_OF_STOCK
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/5/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 5`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 1 шт  на паллете 3 шт ( есть стоки на других ячейках)
     * Есть 3 новых проблемы для SKU01 на 1,2,3 шт
     * Ожидание REPL_PICK меняет тип на REPL_ORD_PK на 3шт с одной паллеты,
     * на 1 проблему создаются новые задачи
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/6/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/6/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 6`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 5 шт  на паллете 10 шт ( есть стоков на других ячейках)
     * Есть 1 новая проблема для SKU01 на 10 шт
     * Ожидание создается 1 новая задача REPL_ORD_PK на другую паллету
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/7/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/7/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 7`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 8 шт  на паллете 10 шт (стоки на других палетах есть)
     * Есть 1 новая проблема для SKU01 на 5 шт
     * Ожидание: новая задача на другую паллету
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/8/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/8/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 8`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 5 шт  на паллете 10 шт (стоков на других палетах нет)
     * Есть 1 новая проблема для SKU01 на 10 шт
     * Ожидание: REPL_PICK меняет тип на REPL_ORD_PICK и обновляет кол-во до 10шт
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/9/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/9/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 9`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 2 задачи с типом REPL_PICK для SKU01 на 2 и 3 шт  на паллете 10 шт (стоков на других палетах нет)
     * Есть 1 новая проблема для SKU01 на 1 шт
     * Ожидание: REPL_PICK меняет тип на REPL_ORD_PICK
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/9.1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/9.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge test 10`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK     для SKU01 на 20 шт  на паллете 50 шт (PALLETE1)
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 10 шт  на паллете 50 шт (PALLETE2)
     * Есть 3 новых проблемы для SKU01 на 30,40,20 шт
     * Ожидание: у REPL_PICK кол-во меняется до 50 у REPL_ORD_PICK до 40, новых задач не создается
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/10/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/10/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 20 шт  на паллете 50 шт (PALLETE1)
     * Есть 1 задача с типом REPL_PICK для SKU01 на 10 шт  на паллете 40 шт (PALLETE2)
     * Есть 3 новых проблемы для SKU01 на 30,40,20 шт
     * Ожидание: у REPL_PICK меняется тип и увеличивается кол-во, проставляется приоритет
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/11/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/11/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REP_ORD_PK для SKU01 на 10 шт на паллете 40 шт (PALLETE2) uomqty = 0
     * Есть 2 новых проблемы для SKU01 на 1,2 шт
     * Ожидание: у REP_ORD_PK увеличивается кол-во uomqty с 0 до 3
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/11.1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/11.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks uom qty update 1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }


    /**
     * Есть 1 задача с типом REP_ORD_PK для SKU01 на 10 шт на паллете 40 шт (PALLETE2) uomqty = 9
     * Есть 2 новых проблемы для SKU01 на 1,2 шт
     * Ожидание: у REP_ORD_PK увеличивается кол-во uomqty с 9 до 12 qty c 10 до 12
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/11.2/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/11.2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks uom qty update 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }


    /**
     * Есть 1 задача с типом REPL_PICK для SKU01 на 10 шт  на паллете 30 шт (PALLETE1)
     * Есть 3 новых проблемы для SKU01 на 30, 40, 20 шт
     * Ожидание: у REPL_PICK меняется тип и увеличивается кол-во, проставляется приоритет, 2 проблемы идут в OUT_OF_STOCK
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/12/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/12/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks insufficient stocks`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 2 задачи с типом REPL_ORD_PICK для SKU01 на 30, 20 шт  на паллете 100 шт (PALLETE1)
     * Есть 3 новых проблемы для SKU01 на 30, 40, 20 шт
     * Ожидание: 2 проблемы назначаются на текущие задачи, 1 проблема (40 шт) идет в OUT_OF_STOCK
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/13/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/13/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks insufficient stocks2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 30 шт  на паллете 50 шт (PALLETE1)
     * Есть 1 новая проблемы для SKU01 на 30
     * Ожидание: у задачи REPL_ORD_PICK увеличивается кол-во до 50 (берем все что есть на паллете)
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/14/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/14/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks insufficient stocks3`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть задача REP_ORD_PK на 10 шт на паллете 30 шт (PALLETE01)
     * на 2 других по 20 шт (PALLETE02,PALLETE03)
     * Есть проблема на 50 шт
     * Ожидание: Задача REP_ORD_PK кол-во до 30 шт, создаются задачи на спуск/отбор из 2ух локаций
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/15/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/15/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks several pallets repl order pick`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * Есть задача REP_PICK на 10 шт на паллете 20 шт (PALLETE01)
     * на 2 других по 10 и 20 шт (PALLETE02,PALLETE03)
     * Есть проблема на 50 шт
     * Ожидание: Задача REP_PICK меняет тип и кол-во до 20 шт, создаются задачи на спуск/отбор из 2ух локаций
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/22/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/22/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `merge several tasks several pallets repl pick`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 ID01 SKU01 40
     * PALLETE1 ID02 SKU02 10
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 30
     * Есть 1 проблема для SKU02 на 1 шт
     * Ожидание: добавелние новой задачи на отбор 1 шт в текущую группу задач
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/16/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/16/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `several sku on pallet test1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 ID01 SKU01 40 шт expire 2020.02.02
     * PALLETE1 ID02 SKU01 10 шт expire 2020.01.01
     * Есть 1 проблема для SKU01 на 5 шт
     * Ожидание: создается задача на отбор для PALLETE1 ID02
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/17/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/17/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `fifo one pallet two ids test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 ID01 SKU01 40 шт expire 2020.02.02
     * PALLETE1 ID02 SKU01 10 шт expire 2020.03.03
     * Есть 1 проблема для SKU01 на 5 шт
     * Ожидание: создается задача на отбор для PALLETE1 ID01
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/18/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/18/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `fifo one pallet two ids test2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 ID01 SKU01 40 шт expire 2020.02.02
     * PALLETE2 ID02 SKU01 10 шт expire 2020.01.01
     * Есть 1 проблема для SKU01 на 5 шт
     * Ожидание: создается отдельная задача на спуск/отбор для PALLETE2 ID02
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/19/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/19/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `fifo two pallet two ids test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 ID01 SKU01 40 шт expire 2020.02.02
     * PALLETE2 ID02 SKU01 10 шт expire 2020.01.01
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 30 с PALLETE1 ID01
     * Есть 1 проблема для SKU01 на 5 шт
     * Ожидание: создается отдельная задача на спуск для PALLETE2 ID02
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/20/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/20/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `fifo two pallet two ids existing task test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 ID01 SKU01 40 шт expire 2020.02.02
     * PALLETE2 ID02 SKU01 10 шт expire 2020.03.03
     * Есть 1 задача с типом REPL_ORD_PICK для SKU01 на 30 с PALLETE1 ID01
     * Есть 1 проблема для SKU01 на 5 шт
     * Ожидание: текущая задача увеличит кол-во до 35 шт и изменит приоритет
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/21/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/21/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `fifo two pallet two ids existing task with merge test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * не используем паллеты для пополнений под оборачиваемость, на которые есть задачи пополнений под заказ
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/23/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/23/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `dont use order replenishment pallets`() {
        replenishmentService.createTurnoverReplenishmentTasks()
    }


    /**
     * PALLETE1 SKU01 3x10 шт в коробках ID01-ID10
     * Есть проблема на 1 шт SKU01
     * Настройки: минимальный процент отбора 10% c паллеты, минимальное кол-во которое оставляем на паллете 30%
     * Ожидание: задача на отбор 1 коробки (3шт) для проблемы на 1шт
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test 1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 SKU01 3x10 шт в коробках ID01-ID10
     * Есть проблема на 1 шт SKU01
     * Настройки: минимальный процент отбора 10% c паллеты, минимальное кол-во которое оставляем на паллете 95%
     * Ожидание: задачи на отбор 10 коробок (всей паллеты)
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/2/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 SKU01 от 1 шт до 10 шт в коробках ID01-ID10 всего 55 шт
     * Есть проблема на 4 шт SKU01
     * Настройки: минимальный процент отбора 10% c паллеты, минимальное кол-во которое оставляем на паллете 30%
     * Ожидание: должны отобрать 6 шт. Задачи на отбор коробок ID01,ID02,ID03
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/3/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test 3`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 SKU01 3x10 шт в коробках ID01-ID10
     * Есть 5 проблем каждая на 1 шт SKU01
     * Настройки: минимальный процент отбора 30% c паллеты, минимальное кол-во которое оставляем на паллете 30%
     * Ожидание: задача на отбор 3 коробок по 3шт
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/4/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test 4`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE01 SKU01 4x3 шт в коробках ID01-ID04 (30% - 4 шт)
     * PALLETE02 SKU01 6x3 шт в коробках ID05-ID10
     * Есть 5 проблем каждая на 1 шт SKU01
     * Настройки: минимальный процент отбора 30% c паллеты, минимальное кол-во которое оставляем на паллете 10%
     * Ожидание: задача на отбор 2 коробок по 3шт c паллеты PALLETE01
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/5/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `take from pallet with less qty 01`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE01 SKU01 6x3 шт в коробках ID01-ID06
     * PALLETE02 SKU01 4x3 шт в коробках ID07-ID10 (60% ~ 7 шт)
     * Есть 5 проблем каждая на 1 шт SKU01
     * Настройки: минимальный процент отбора 10% c паллеты, минимальное кол-во которое оставляем на паллете 60%
     * Ожидание: задача на отбор всех коробок по 3шт c паллеты PALLETE02
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/6/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/6/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `take from pallet with less qty 02`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     * PALLETE1 SKU01 3x10 шт в коробках ID01-ID10
     * Есть проблема на 1 шт SKU01
     * Настройки: минимальный процент отбора 0 c паллеты, минимальное кол-во которое оставляем на паллете 0
     * Ожидание: задача на отбор 1 коробки (3шт) для проблемы на 1шт
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/7/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/7/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test no uit`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**  PALLETE1 SKU01 3x10 шт в коробках ID01-ID10
     * Есть 5 проблем каждая на 1 шт SKU01
     * Настройки: минимальный процент отбора 0 c паллеты, минимальное кол-во которое оставляем на паллете 0
     * Ожидание: задача на отбор 2 коробок по 3шт
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/8/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/8/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test no uit 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**  PALLETE1 SKU01 3x10 шт в коробках ID01-ID10
     * Есть 5 проблем каждая на 1 шт SKU01
     * Настройки: минимальный процент отбора 25% c паллеты, минимальное кол-во которое оставляем на паллете 0
     * Ожидание: задача на отбор 3 коробок по 3шт
     */
    @Test
    @DatabaseSetup("/service/order/picking-qty/9/before.xml")
    @ExpectedDatabase(
        value = "/service/order/picking-qty/9/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `qty increase test no uit 3`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/merge-tasks/24/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/24/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `priority update test 1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/merge-tasks/25/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/25/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `priority update test 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    @Test
    @DatabaseSetup("/service/order/merge-tasks/26/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/26/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `priority update test 3`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  нет ни одного заказа в -3, отменяем задачи
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/27/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/27/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel not actual problems test 1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  есть 1 заказ в -3, не отменяем задачи
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/28/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/28/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel not actual problems test 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  есть 1 актуальный заказ на 2 группы задач, не отменяем задачи
     */
    @Test
    @DatabaseSetup("/service/order/merge-tasks/30/before.xml")
    @ExpectedDatabase(
        value = "/service/order/merge-tasks/30/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `dont cancel actual problems with several task groups`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Увеличиваем qty у задачи на отбор для спущенной в буфер паллеты
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/1/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add picking tasks from buffer happy 1`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Увеличиваем qty у задачи на отбор для паллеты в процессе спуска
     *  Добавляем новую задачу на новую SKU
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/2/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add picking tasks from buffer happy 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Добавляем новую задачу на отбор для паллеты ожидающей подъема
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/3/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add picking tasks for lift waiting pallet`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Добавляем новую задачу на отбор для паллеты с которой отбирают
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/5/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add picking tasks for pallet in active picking`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Не добавляем новую задачу на отбор для паллеты в процессе подъема (status=2)
     *  Увеличиваем кол-во у задачи ожидающей спуска
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/6/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/6/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `do not add picking tasks for lifting pallet 2`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Не добавляем новую задачу на отбор для паллеты в процессе подъема (status=3)
     *  Созадем новые задачи
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/7/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/7/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `do not add picking tasks for lifting pallet 3`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }

    /**
     *  Блокируем подъем если отбор не завершен
     *  Разблокируем отбор
     */
    @Test
    @DatabaseSetup("/service/order/create-tasks/in-process/4/before.xml")
    @ExpectedDatabase(
        value = "/service/order/create-tasks/in-process/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `block lift task after with active picking task`() {
        moveTaskService.getTask("4-02", null, ReplenishmentType.ORDER)
    }

    @Test
    fun `empty test`() {
        replenishmentService.createOrderReplenishmentTasks(ReplenishmentType.ORDER)
    }
}
