package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.CourierEntity
import ru.yandex.market.logistics.yard_v2.facade.CourierFacade

class CourierFacadeTest(@Autowired private val courierFacade: CourierFacade) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/courier/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/courier/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveAll() {
        val result = courierFacade.saveAll(
            listOf(
                CourierEntity(
                    externalId = "external_id",
                    name = "name",
                    surname = "surname",
                    patronymic = "patronymic",
                    carModel = "car_model",
                    carNumber = "car_number",
                    carBrand = "car_brand",
                    carTrailerNumber = "car_trailer_number",
                    phone = "phone",
                    phoneAdditional = "phone_additional",
                    ownershipType = "ownership_type",
                    courierUid = "courier_uid"
                )
            )
        )

        assertions().assertThat(result.size).isEqualTo(1)
        assertions().assertThat(result[0].id).isEqualTo(1)
        assertions().assertThat(result[0].externalId).isEqualTo("external_id")
        assertions().assertThat(result[0].name).isEqualTo("name")
        assertions().assertThat(result[0].surname).isEqualTo("surname")
        assertions().assertThat(result[0].patronymic).isEqualTo("patronymic")
        assertions().assertThat(result[0].carModel).isEqualTo("car_model")
        assertions().assertThat(result[0].carNumber).isEqualTo("car_number")
        assertions().assertThat(result[0].carBrand).isEqualTo("car_brand")
        assertions().assertThat(result[0].carTrailerNumber).isEqualTo("car_trailer_number")
        assertions().assertThat(result[0].phone).isEqualTo("phone")
        assertions().assertThat(result[0].phoneAdditional).isEqualTo("phone_additional")
        assertions().assertThat(result[0].ownershipType).isEqualTo("ownership_type")
        assertions().assertThat(result[0].courierUid).isEqualTo("courier_uid")

    }
}
