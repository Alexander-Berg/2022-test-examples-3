package ru.yandex.market.logistics.cte.monitoring.solomon

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.*

import javax.annotation.Nonnull

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.*
import org.apache.commons.io.IOUtils

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

import org.springframework.beans.factory.annotation.Autowired

import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.entity.supply.SupplyItem
import ru.yandex.market.logistics.cte.monitoring.solomon.scheduler.SupplyItemInvalidCategoryCheckScheduler
import ru.yandex.market.logistics.cte.repo.MonitoringRepository
import ru.yandex.market.logistics.cte.repo.SupplyItemRepository
import ru.yandex.market.logistics.cte.service.DateTimeService
import ru.yandex.market.logistics.cte.service.SystemPropertyService
import java.time.Instant

class SupplyItemInvalidCategoryCheckSchedulerTest(@Autowired private val monitoringRepository: MonitoringRepository,
                                                  @Autowired private val solomonPushClient: SolomonPushClient,
                                                  @Autowired private val dateTimeService: DateTimeService,
                                                  @Autowired private val supplyRepository: SupplyItemRepository,
                                                  @Autowired private val systemPropertyService: SystemPropertyService
                                                  ) : IntegrationTest() {

    private val captor = argumentCaptor<LocalDateTime>()
    private var scheduler: SupplyItemInvalidCategoryCheckScheduler? = null
    private var date: Instant? = null

    @BeforeEach
    fun init() {
        val spyDateTimeService = Mockito.spy(dateTimeService)
        scheduler = SupplyItemInvalidCategoryCheckScheduler(spyDateTimeService!!,
            monitoringRepository, solomonPushClient, systemPropertyService)
        date = Instant.now()
        Mockito.doReturn(date!!.toEpochMilli()).`when`(spyDateTimeService).localDateTimeToMillis(captor.capture())
    }

    /*
    * Так как особенностью создания записи в базе с новым SupplyItem - применение текущей даты, для теста, проверяющего
    * отрабатывание функции сбора данных мы не меняем параметры и не указываем их явно в базе, так как они все равно
    * будут равны текущей дате
    * */
    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:scheduler/supply-item-invalid-category/existing-invalid-categories-before.xml"),
        DatabaseSetup("classpath:scheduler/supply-item-invalid-category/quality_matrix.xml"))
    @ExpectedDatabase(
        value = "classpath:scheduler/supply-item-invalid-category/existing-invalid-categories-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(IOException::class)
    fun supplyItemInvalidCategoryCheckEventsAreSentCorrect() {
        val expectedMonitoringToSolomon = getFileContent(
            "scheduler/supply-item-invalid-category/monitoring-to-solomon-all-match.json")
            .format(date!!.epochSecond)
        scheduler!!.calculateAndSend()
        Mockito.verify(solomonPushClient).push(expectedMonitoringToSolomon)
    }

    /*
    * Обновляем все createdAt поля для базы более старыми значениями, чтобы не подходить под параметры
    * */
    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:scheduler/supply-item-invalid-category/all-valid-categories-before.xml"),
        DatabaseSetup("classpath:scheduler/supply-item-invalid-category/quality_matrix.xml"))
    @ExpectedDatabase(
        value = "classpath:scheduler/supply-item-invalid-category/all-valid-categories-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(IOException::class)
    fun supplyItemInvalidCategoryCheckEventsAreSentCorrectIfNoneExists() {
        val expectedMonitoringToSolomon = getFileContent(
            "scheduler/supply-item-invalid-category/monitoring-to-solomon-empty.json")
            .format(date!!.epochSecond)
        makeSupplyItemsBeCreatedMoreThanXDaysAgo(supplyRepository.findAll(), 2)
        scheduler!!.calculateAndSend()
        Mockito.verify(solomonPushClient).push(expectedMonitoringToSolomon)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:scheduler/supply-item-invalid-category/existing-invalid-categories-before.xml"),
        DatabaseSetup("classpath:scheduler/supply-item-invalid-category/quality_matrix.xml"))
    @ExpectedDatabase(
        value = "classpath:scheduler/supply-item-invalid-category/existing-invalid-categories-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(IOException::class)
    fun supplyItemInvalidCategoryCheckEventsAreSentCorrectCornerCase() {
        val expectedMonitoringToSolomon = getFileContent(
            "scheduler/supply-item-invalid-category/monitoring-to-solomon-one-item.json")
            .format(date!!.epochSecond)
        makeSupplyItemsBeCreatedMoreThanXDaysAgo(listOf(supplyRepository.findById(1).get()), 3)
        makeSupplyItemBeCornerCase(supplyRepository.findById(2).get())

        scheduler!!.calculateAndSend()
        Mockito.verify(solomonPushClient).push(expectedMonitoringToSolomon)
    }

    private fun makeSupplyItemsBeCreatedMoreThanXDaysAgo(list: List<SupplyItem>, daysMinus: Long) {
        list.forEach { item -> item.createdAt =  dateTimeService.instantToLocalDateTime(date!!).minusDays(daysMinus)}
        supplyRepository.saveAll(list)
    }

    private fun makeSupplyItemBeCornerCase(item: SupplyItem) {
        item.createdAt = dateTimeService.instantToLocalDateTime(date!!).minusDays(2).plusHours(1)
        supplyRepository.save(item)
    }

    @Throws(IOException::class)
    private fun getFileContent(@Nonnull fileName: String): String {
        return IOUtils.toString(
            Objects.requireNonNull(
                ClassLoader.getSystemResourceAsStream(fileName)),
            StandardCharsets.UTF_8).trim { it <= ' ' }
    }
}
