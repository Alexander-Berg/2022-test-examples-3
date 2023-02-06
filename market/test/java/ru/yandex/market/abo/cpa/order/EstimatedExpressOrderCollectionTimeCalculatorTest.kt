package ru.yandex.market.abo.cpa.order

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.cpa.order.EstimatedExpressOrderCollectionTimeCalculator.Companion.MINUTES_FOR_ORDER_COLLECTION
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import ru.yandex.market.abo.cpa.lms.model.LmsPartnerWorkingDay

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 31.05.2021
 */
class EstimatedExpressOrderCollectionTimeCalculatorTest {

    private val calculator: EstimatedExpressOrderCollectionTimeCalculator =
        EstimatedExpressOrderCollectionTimeCalculator(
            pgRoJdbcTemplate = mock(),
            lmsClient = mock(),
            logisticsService = mock(),
            checkouterService = mock(),
            cpaExpressOrderEstimatedTimeRepo = mock(),
            lomService = mock(),
        )

    @Test
    fun `order estimated time in working period`() {
        val currentDate = LocalDate.now()
        val orderCreationDateTime = INTAKE_TIME_FROM.atDate(currentDate)
        val expectedEstimatedTime = orderCreationDateTime.plusMinutes(MINUTES_FOR_ORDER_COLLECTION)
        val lomEstimatedDateTime = expectedEstimatedTime.plusSeconds(30)

        val actualEstimatedTime = calculator.getEstimatedTime(
            lomEstimatedDateTime,
            orderCreationDateTime,
            currentDate,
            buildIntakesByDay(currentDate.dayOfWeek)
        )

        assertEquals(expectedEstimatedTime, actualEstimatedTime)
    }

    @Test
    fun `order estimated time in working period but less than creation time + 30 min`() {
        val currentDate = LocalDate.now()
        val orderCreationDateTime = INTAKE_TIME_FROM.atDate(currentDate)
        val expectedEstimatedTime = orderCreationDateTime.plusMinutes(MINUTES_FOR_ORDER_COLLECTION)
        val lomEstimatedDateTime = expectedEstimatedTime.minusMinutes(1)

        val actualEstimatedTime = calculator.getEstimatedTime(
            lomEstimatedDateTime, orderCreationDateTime, currentDate, buildIntakesByDay(currentDate.dayOfWeek)
        )

        assertEquals(expectedEstimatedTime, actualEstimatedTime)
    }

    @Test
    fun `order estimated time in before working period`() {
        val currentDate = LocalDate.now()
        val lomEstimatedDateTime = INTAKE_TIME_FROM.atDate(currentDate).minusMinutes(20)

        val expectedEstimatedTime = INTAKE_TIME_FROM.atDate(currentDate).plusMinutes(MINUTES_FOR_ORDER_COLLECTION)
        val actualEstimatedTime = calculator.getEstimatedTime(
            lomEstimatedDateTime, LocalDateTime.now(), currentDate, buildIntakesByDay(currentDate.dayOfWeek)
        )

        assertEquals(expectedEstimatedTime, actualEstimatedTime)
    }

    @Test
    fun `order estimated time in after working period`() {
        val currentDate = LocalDate.now()
        val lomEstimatedDateTime = INTAKE_TIME_TO.atDate(currentDate).plusMinutes(10)

        val expectedEstimatedTime =
            INTAKE_TIME_FROM.atDate(currentDate.plusDays(1)).plusMinutes(MINUTES_FOR_ORDER_COLLECTION)
        val actualEstimatedTime = calculator.getEstimatedTime(
            lomEstimatedDateTime, LocalDateTime.now(), currentDate, buildIntakesByDay(currentDate.dayOfWeek)
        )

        assertEquals(expectedEstimatedTime, actualEstimatedTime)
    }

    @Test
    fun `order estimated date not equals shipment date`() {
        val currentDate = LocalDate.now()
        val shipmentDate = currentDate.plusDays(1)

        val processingDateTime = INTAKE_TIME_FROM.atDate(currentDate).plusMinutes(50)

        val expectedEstimatedTime = INTAKE_TIME_FROM.atDate(shipmentDate).plusMinutes(MINUTES_FOR_ORDER_COLLECTION)
        val actualEstimatedTime = calculator.getEstimatedTime(
            processingDateTime, LocalDateTime.now(), shipmentDate, buildIntakesByDay(currentDate.dayOfWeek)
        )

        assertEquals(expectedEstimatedTime, actualEstimatedTime)
    }

    private fun buildIntakesByDay(day: DayOfWeek): Map<Int, LmsPartnerWorkingDay> = mapOf(
        day.value to createIntake(day.value),
        day.plus(1).value to createIntake(day.plus(1).value)
    )

    private fun createIntake(day: Int): LmsPartnerWorkingDay = LmsPartnerWorkingDay(
        day, INTAKE_TIME_FROM, INTAKE_TIME_TO
    )

    companion object {
        private val INTAKE_TIME_FROM = LocalTime.of(10, 0)
        private val INTAKE_TIME_TO = LocalTime.of(18, 0)
    }
}
