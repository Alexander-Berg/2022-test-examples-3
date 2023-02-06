package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.core.delivery.DeliveryServiceType
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.mbi.PartnerFulfillmentLinkDTO
import ru.yandex.market.logistics.yard.mbi.PartnerFulfillmentLinksDTO
import ru.yandex.market.logistics.yard_v2.domain.entity.CourierEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.TripPointInfoEntity
import ru.yandex.market.logistics.yard_v2.facade.TripPointInfoFacade
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TripPointInfoFacadeTest(
    @Autowired private val tripPointInfoFacade: TripPointInfoFacade
) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/trip_point_info/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/trip_point_info/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun save() {
        val entity = TripPointInfoEntity(
            id = 75,
            tripId = 1,
            plannedIntervalStart = OffsetDateTime.of(2000, 4, 4, 4, 4, 4, 4, ZoneOffset.UTC),
            courier = CourierEntity(
                id = 1,
                externalId = "11",
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
            ),
            unitType = "unitType",
            whPartnerId = 2,
            partnerId = 3,
            partnerName = "partnerName",
            partnerMarketId = 4
        )
        tripPointInfoFacade.save(entity)
        tripPointInfoFacade.save(entity)
    }
}
