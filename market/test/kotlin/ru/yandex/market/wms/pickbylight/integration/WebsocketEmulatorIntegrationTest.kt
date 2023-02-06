package ru.yandex.market.wms.pickbylight.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.util.client.TvmTicketProvider
import ru.yandex.market.wms.common.spring.utils.uuid.TimeBasedGenerator
import ru.yandex.market.wms.pickbylight.client.websocket.PickByLightWebSocketClient
import ru.yandex.market.wms.pickbylight.client.websocket.config.PickByLightWebSocketClientConfig
import ru.yandex.market.wms.pickbylight.configuration.DatabaseTestConfiguration
import ru.yandex.market.wms.pickbylight.configuration.PickByLightIntegrationTest
import ru.yandex.market.wms.pickbylight.configuration.PickByLightTestConfiguration
import ru.yandex.market.wms.pickbylight.dao.StationCellDao
import ru.yandex.market.wms.pickbylight.dao.StationDao
import ru.yandex.market.wms.pickbylight.model.ButtonEvent
import ru.yandex.market.wms.pickbylight.model.Color
import ru.yandex.market.wms.pickbylight.model.Station
import ru.yandex.market.wms.pickbylight.model.StationCell
import ru.yandex.market.wms.pickbylight.model.StationSide
import ru.yandex.market.wms.pickbylight.vendor.Vendor
import ru.yandex.market.wms.pickbylight.vendor.axelot.emulator.ModuleNetwork
import java.util.concurrent.Callable
import java.util.concurrent.Exchanger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/**
 * Тут есть особенность - в тесте поднимается эмулятор TCP-контроллера и занимает порт 15003.
 * Если сделать второй тест с эмулятором - то они не должны запускаться параллельно.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [
        DatabaseTestConfiguration::class,
        PickByLightTestConfiguration::class,
        PickByLightWebSocketClientConfig::class,
    ],
    properties = [
        "wms.pick-by-light.emulator.enabled=true"
    ]
)
class WebsocketEmulatorIntegrationTest : PickByLightIntegrationTest() {

    companion object {
        private const val STATION = "S01"
        private val POOL = Executors.newFixedThreadPool(30)
        private val POOL2 = Executors.newFixedThreadPool(30)

        private val station = Station(STATION, Vendor.AXELOT, "localhost", 15003, "0", "1")

        private val allCellsByName: Map<String, StationCell> = (0..999)
            .map {
                StationCell(
                    stationName = STATION,
                    cellName = "$STATION-${"%04d".format(it)}",
                    addressIn = "%04d".format(it),
                    addressOut = "%04d".format(1000 + it)
                )
            }
            .associateBy { it.cellName }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            POOL.shutdownNow()
            POOL2.shutdownNow()
        }
    }

    @Value("\${local.server.port}")
    private var port: Int = 0

    @Value("\${wms.pick-by-light.websocket}")
    private lateinit var webSocketUrl: String

    @Autowired
    @Qualifier("pickByLightTvmTicketProvider")
    private lateinit var tvmTicketProvider: TvmTicketProvider

    @Autowired
    private lateinit var moduleNetwork: ModuleNetwork

    @MockBean
    @Autowired
    private lateinit var stationDao: StationDao

    @MockBean
    @Autowired
    private lateinit var stationCellDao: StationCellDao

    /**
     * Включен TCP эмулятор с путволом на 1000 ячеек.
     * К websocket серверу pick-by-light подключаются несколько клиентов, эмулирующих консолидацию и упаковку.
     * Каждому клиенту выделена своя порция ячеек.
     * Консолидация зажигает 3 модуля и гасит их все при нажатии на один из них.
     * Упаковка зажигает 3 модуля и гасит их по очереди, когда нажимают на соответствующую кнопку.
     * Потом зажигаются следущие 3 модуля и так далее.
     */
    @Test
    fun `test when multiple websocket clients connected and tcp emulator used`() {
        whenever(stationDao.getStation(STATION)).thenReturn(station)
        allCellsByName.values.toList().also {
            whenever(stationCellDao.getStationCells(STATION)).thenReturn(it)
            whenever(stationCellDao.getStationCellsByHostPort(station.host, station.port)).thenReturn(it)
        }

        val inApps = allCellsByName.keys.chunked(20).map { ClientApp(STATION, StationSide.IN, it) }
        val outApps = allCellsByName.keys.chunked(30).map { ClientApp(STATION, StationSide.OUT, it) }

        val futures = POOL.invokeAll(inApps + outApps, 2, TimeUnit.MINUTES)
        println("Result: ${moduleNetwork.getColors()}")
        futures.map { it.get() }
    }

    private fun newClient() =
        PickByLightWebSocketClient(webSocketUrl.format(port), tvmTicketProvider, TimeBasedGenerator(), POOL2)

    fun waitUntil(timeout: Long, unit: TimeUnit, provider: () -> Boolean) {
        var millis = unit.toMillis(timeout)
        while (!provider() && millis-- > 0) {
            Thread.sleep(1)
        }
        if (millis < 0) {
            throw IllegalStateException("Event not happened")
        }
    }

    inner class ClientApp(
        private val station: String,
        private val stationSide: StationSide,
        cellNames: List<String>,
    ) : Callable<Unit> {
        private val cellNamesChunks: List<List<String>> = cellNames.chunked(3)
            .also { if (it.size == 1) throw Error("Must have more than 1 chunk") }
        private val client = newClient()
        private val allowedEvents = mutableListOf<ButtonEvent>()
        private lateinit var exchanger: Exchanger<ButtonEvent>
        private var repetitions = 10

        override fun call() {
            client.addEventListener(::onButtonPushed)

            while (repetitions-- > 0) {
                cellNamesChunks.forEach { cellNames ->
                    allowedEvents += cellNames.map { ButtonEvent(station, it) }
                    val color = randomColor()
                    client.switchOn(station, stationSide, color, cellNames)
                    // проверяем, что модули включились
                    waitUntil(10, TimeUnit.SECONDS) {
                        addresses(cellNames).all { moduleNetwork.getColor(it) == color }
                    }
                    while (allowedEvents.isNotEmpty()) {
                        val expectedEvent = allowedEvents[ThreadLocalRandom.current().nextInt(allowedEvents.size)]
                        exchanger = Exchanger()
                        moduleNetwork.pushButton(addresses(listOf(expectedEvent.cellName)).first())
                        val event = exchanger.exchange(null, 10, TimeUnit.SECONDS)
                        if (!allowedEvents.remove(event)) {
                            throw IllegalStateException("Unexpected event $event, expected: $allowedEvents")
                        }
                        val cellsToSwitchOff = when (stationSide) {
                            // на консолидации гасим все ячейки, которые были активированы
                            StationSide.IN -> {
                                allowedEvents.clear()
                                cellNames
                            }
                            // на упаковке гасим только ту ячейку, где была нажата кнопка
                            StationSide.OUT -> {
                                listOf(event.cellName)
                            }
                        }
                        client.switchOff(station, stationSide, cellsToSwitchOff)
                    }
                }
            }
            // проверяем, что все использованные модули погашены
            waitUntil(10, TimeUnit.SECONDS) {
                addresses(cellNamesChunks.flatten()).all { moduleNetwork.getColor(it) == Color.OFF }
            }
            client.disconnect()
        }

        private fun addresses(cellNames: List<String>) =
            cellNames.map { allCellsByName[it]!!.address(stationSide) }

        private fun onButtonPushed(event: ButtonEvent) {
            exchanger.exchange(event, 10, TimeUnit.SECONDS)
        }

        private fun randomColor(): Color {
            val colors = Color.values().filter { it != Color.OFF }
            return colors[ThreadLocalRandom.current().nextInt(colors.size)]
        }
    }
}
