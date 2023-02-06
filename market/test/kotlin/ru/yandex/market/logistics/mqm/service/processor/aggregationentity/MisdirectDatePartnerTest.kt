package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.additionaldata.BasePlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.additionaldata.MisdirectAdditionalData
import ru.yandex.market.logistics.mqm.entity.additionaldata.PlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.aggregationentity.LocationAggregationEntity
import ru.yandex.market.logistics.mqm.entity.aggregationentity.OrderAggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import java.time.Instant
import java.time.LocalDate

class MisdirectDatePartnerTest {
    private val converter = AggregationEntityConverter();
    private val producer = MisdirectProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        Assertions.assertThat(producer.isEligible(preparePlanFact())).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_ORDER"]
    )
    @DisplayName("Процессор не применяется, если неправильный EntityType")
    fun isEligibleReturnFalseForWrongType(entityType: EntityType) {
        val planFact = preparePlanFact(entityType = entityType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Процессор не применяется, если нет нужной AdditionalData")
    fun isEligibleReturnFalseForWrongAdditionalData() {
        val planFact = preparePlanFact(data = BasePlanFactAdditionalData())
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["RETURNING"]
    )
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntityDirectFlow(orderStatus: OrderStatus) {
        val testSortingCenterName = "Test name"
        val planFact = preparePlanFact().apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T06:00:00.00Z")
            setData(
                MisdirectAdditionalData(
                    sortingCenterName = testSortingCenterName,
                    orderStatus = orderStatus,
                )
            )
        }
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            order = OrderAggregationEntity(directFlow = true),
            locationTo = LocationAggregationEntity(address = testSortingCenterName)
        )
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntityReverseFlow() {
        val testSortingCenterName = "Test name"
        val planFact = preparePlanFact().apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T06:00:00.00Z")
            setData(
                MisdirectAdditionalData(
                    sortingCenterName = testSortingCenterName,
                    orderStatus = OrderStatus.RETURNING,
                )
            )
        }
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            order = OrderAggregationEntity(directFlow = false),
            locationTo = LocationAggregationEntity(address = testSortingCenterName)
        )
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntityNextDay() {
        val testSortingCenterName = "Test name"
        val planFact = preparePlanFact().apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T12:00:01.00Z")
            setData(
                MisdirectAdditionalData(
                    sortingCenterName = testSortingCenterName,
                    orderStatus = OrderStatus.RETURNING,
                )
            )
        }
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            order = OrderAggregationEntity(directFlow = false),
            locationTo = LocationAggregationEntity(address = testSortingCenterName)
        )
    }

    private fun preparePlanFact(
        entityType: EntityType = EntityType.LOM_ORDER,
        data: PlanFactAdditionalData = MisdirectAdditionalData(
            sortingCenterName = null,
            orderStatus = null,
        )
    ): PlanFact {
        val testOrder = LomOrder(id = 1, platformClientId = PlatformClient.BERU.id)
        val planFact = PlanFact(
            entityType = entityType,
            entityId = testOrder.id
        )
        planFact.entity = testOrder
        planFact.setData(data)
        return planFact
    }
}
