package ru.yandex.market.wms.consolidation.integration;

import com.amazonaws.util.json.Jackson
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.BaseTestConfig
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao
import ru.yandex.market.wms.common.spring.service.LineSelectionService
import ru.yandex.market.wms.consolidation.client.ConsolidationClient
import ru.yandex.market.wms.consolidation.client.config.ConsolidationWebClientConfig
import ru.yandex.market.wms.consolidation.config.ConsolidationClientTestConfig
import ru.yandex.market.wms.consolidation.modules.preconsolidation.service.PreConsolidationMovingService
import ru.yandex.market.wms.consolidation.modules.preconsolidation.service.PreConsolidationSelectionLineService
import ru.yandex.market.wms.transportation.client.TransportationClient

@SpringBootTest(classes = [ConsolidationWebClientConfig::class,
    ConsolidationClientTestConfig::class,
    BaseTestConfig::class,
    IntegrationTestConfig::class])
class NonSortConsolidationTest (
    private @Autowired val moveService : PreConsolidationMovingService,
    private @Autowired val lineService : PreConsolidationSelectionLineService,
    private @Autowired val taskDao: TaskDetailDao,
    private @Autowired val lineSelectionService: LineSelectionService
    ) : IntegrationTest() {

    @Autowired
    @MockBean
    private val transportationClient: TransportationClient? = null

    @Autowired
    @SpyBean
    private var consolidationClient: ConsolidationClient?=null

    @BeforeEach
    fun reset() {
        Mockito.reset(transportationClient)
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/before/orders-picks.xml",
        "/integration/before/several-orders-with-nonsort.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/after/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/after/several-orders-with-nonsort.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortHappy(){
        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/before/orders-picks.xml",
        "/integration/before/several-orders-with-nonsort.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/after/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/after/several-orders-with-nonsort.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortHappyClient(){
        consolidationClient = Mockito.spy(consolidationClient)!!
        Mockito.doAnswer {
            val url = it.getArgument<String>(0).toString()
            val obj = it.getArgument<Any>(1)
            mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Jackson.toJsonString(obj))
            )
        }.`when`(consolidationClient)?.postRequest(anyString(), any<Any>())

        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        consolidationClient?.moveContainerToLine(destination.containerId, destination.destinationLine!!)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        consolidationClient?.moveContainerToLine(destination2.containerId, destination2.destinationLine!!)
    }


    @Test
    @DatabaseSetup(value = [
        "/integration/nonsort-canceled-order/before/1/orders-picks.xml",
        "/integration/nonsort-canceled-order/before/1/balances.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/1/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/1/balances.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortWithCanceledOrder1(){

        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/nonsort-canceled-order/before/2/orders-picks.xml",
        "/integration/nonsort-canceled-order/before/2/balances.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/2/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/2/balances.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortWithCanceledOrder2(){

        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/nonsort-canceled-order/before/3/orders-picks.xml",
        "/integration/nonsort-canceled-order/before/3/balances.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/3/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/3/balances.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortWithCanceledOrder2ReverseConsolidationOrder(){
        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)

        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)
     }

    @Test
    @DatabaseSetup(value = [
        "/integration/nonsort-canceled-order/before/5/orders-picks.xml",
        "/integration/nonsort-canceled-order/before/5/balances.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/5/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/5/balances.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortCanceledOneItemInOrder(){
        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
     }

    @Test
    @DatabaseSetup(value = [
        "/integration/nonsort-canceled-order/before/4/orders-picks.xml",
        "/integration/nonsort-canceled-order/before/4/balances.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/4/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/4/balances.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortWithAllCanceledOrders(){
        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/nonsort-canceled-order/before/6/orders-picks.xml",
        "/integration/nonsort-canceled-order/before/6/balances.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/6/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/nonsort-canceled-order/after/6/balances.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortWithCanceledOrderInTwoCarts(){
        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
    }


    @Test
    @DatabaseSetup(value = [
        "/integration/before/orders-picks.xml",
        "/integration/before/several-orders-with-nonsort.xml"
    ])
    fun lineSelectionTest(){
        var destination = lineService.getLineForContainer("CART001")
        assertThat(destination.destinationLine).isEqualTo("SORT-CONS")

        destination = lineService.getLineForContainer("CART002");
        assertThat(destination.destinationLine).isEqualTo("SORT-CONS")

        destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")

        destination = lineService.getLineForContainer("CART004");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-2")
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/before/orders-picks.xml",
        "/integration/before/several-orders-with-nonsort.xml"
    ])
    fun lineSelectionAfterNonSortMoveTest(){
        var destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        destination = lineService.getLineForContainer("CART001")
        assertThat(destination.destinationLine).isEqualTo("SORT-CONS")

        destination = lineService.getLineForContainer("CART004");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        destination = lineService.getLineForContainer("CART002");
        assertThat(destination.destinationLine).isEqualTo("SORT-CONS")
    }

    @Test
    @DatabaseSetup(value = [
        "/integration/before/orders-picks.xml",
        "/integration/before/several-orders-with-nonsort.xml"
    ])
    fun lineSelectionAfterSimpleMoveTest(){
        var taskLine = lineSelectionService.getLineIfSomeContainersIntoThisLine("CART001")
        assertThat(taskLine).isEmpty

        var destination = lineService.getLineForContainer("CART001")
        assertThat(destination.destinationLine).isEqualTo("SORT-CONS")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        taskLine = lineSelectionService.getLineIfSomeContainersIntoThisLine("CART002")
        assertThat(taskLine).isNotEmpty
        assertThat(taskLine.get()).isEqualTo("SORT-CONS")

        destination = lineService.getLineForContainer("CART002")
        assertThat(destination.destinationLine).isEqualTo("SORT-CONS")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)


        destination = lineService.getLineForContainer("CART003")
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        destination = lineService.getLineForContainer("CART004")
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-2")

    }

    @Test
    @DatabaseSetup(value = [
        "/integration/lost-pick-details/before/orders-picks.xml",
        "/integration/lost-pick-details/before/several-orders-with-nonsort.xml"
    ])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/integration/lost-pick-details/after/orders-picks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/integration/lost-pick-details/after/several-orders-with-nonsort.xml" , assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun consolidateNonSortNoPickDetails(){
        val destination = lineService.getLineForContainer("CART003");
        assertThat(destination.destinationLine).isEqualTo("NS-CONS-1")
        moveService.moveContainerToLine(destination.containerId, destination.destinationLine, destination.destinationLine)

        val destination2 = lineService.getLineForContainer("CART004");
        assertThat(destination2.destinationLine).isEqualTo("NS-CONS-2")
        moveService.moveContainerToLine(destination2.containerId, destination2.destinationLine, destination2.destinationLine)
    }
}
