package ru.yandex.market.dsm.domain.courier.test

import org.assertj.core.api.Assertions
import ru.yandex.market.dsm.domain.courier.model.Courier
import ru.yandex.mj.generated.server.model.CourierDto

object AssertsCourierFactory {

    fun assertsEquals(
        courierDto: CourierDto,
        courier: Courier
    ) {
        Assertions.assertThat(courierDto.id).isNotEmpty
        Assertions.assertThat(courier.id).isEqualTo(courierDto.id)
        Assertions.assertThat(courier.status.name).isEqualTo(courierDto.status.name)
        Assertions.assertThat(courier.uid).isEqualTo(courierDto.uid)
        Assertions.assertThat(courier.employerId).isEqualTo(courierDto.employerId)
        Assertions.assertThat(courier.routingId).isEqualTo(courierDto.routingId)
        Assertions.assertThat(courier.workplaceNumber).isEqualTo(courierDto.workplaceNumber)
        Assertions.assertThat(courier.personalData.email).isEqualTo(courierDto.email)
        Assertions.assertThat(courier.personalData.name).isEqualTo(courierDto.name)
        Assertions.assertThat(courier.deleted).isEqualTo(courierDto.deleted)
    }

}
