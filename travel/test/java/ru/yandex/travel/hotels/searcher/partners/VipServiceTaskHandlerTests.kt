package ru.yandex.travel.hotels.searcher.partners

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.travel.commons.proto.ECurrency
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit
import ru.yandex.travel.hotels.common.partners.vipservice.VipserviceClient
import ru.yandex.travel.hotels.common.partners.vipservice.model.CancelConditions
import ru.yandex.travel.hotels.common.partners.vipservice.model.Offer
import ru.yandex.travel.hotels.proto.*
import ru.yandex.travel.hotels.searcher.Task
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        VipserviceTaskHandler::class,
        CommonTestConfiguration::class,
        VipServiceTaskHandlerTests.Configuration::class,
    ],
    properties = [
        "spring.profiles.active=test"
    ]
)
class VipServiceTaskHandlerTests {
    @Autowired
    private lateinit var client: VipserviceClient

    @Autowired
    private lateinit var handler: VipserviceTaskHandler

    @TestConfiguration
    class Configuration {
        @Bean
        fun client(): VipserviceClient =
            Mockito.mock(VipserviceClient::class.java)
    }

    private fun searchOffersReq(hotelId: String): TSearchOffersReq =
        TSearchOffersReq.newBuilder()
            .setId(hotelId)
            .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_VIPSERVICE).setOriginalId(hotelId))
            .setOccupancy("1")
            .setCheckInDate("2000-01-01")
            .setCheckOutDate("2000-01-10")
            .setCurrency(ECurrency.C_RUB)
            .setRequestClass(ERequestClass.RC_INTERACTIVE)
            .build()

    private fun processRequest(req: TSearchOffersReq): TSearchOffersRsp {
        val task = Task(req, true)
        handler.startHandle(listOf(task))
        return task.completionFuture.thenApply { task.dumpResult() }.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `test mapping`() {
        val checkIn = LocalDate.of(2000, 1, 1)
        val checkOut = LocalDate.of(2000, 1, 10)
        val hotelId = 99
        val offerHash = "offerHash"
        val price = 100
        val availableRooms = 5
        val roomName = "Люкс"

        whenever(
            client.findOffers(
                hotelIds = listOf(hotelId),
                checkIn = checkIn,
                checkOut = checkOut,
                adults = 1,
                children = emptyList(),
                currency = ECurrency.C_RUB,
            )
        ).thenReturn(
            CompletableFuture.completedFuture(
                arrayOf(
                    Offer(
                        hash = offerHash,
                        currency = ProtoCurrencyUnit.RUB,
                        price = BigDecimal.valueOf(price.toLong()),
                        priceBreakdown = emptyMap(),
                        isVatIncluded = false,
                        vatAmount = BigDecimal.ZERO,
                        cityId = 0,
                        hotelId = hotelId,
                        providerId = 99,
                        roomName = roomName,
                        cancelConditions = CancelConditions(
                            freeCancellationBefore = null,
                            policies = emptyList()
                        ),
                        availableRooms = availableRooms,
                        extras = emptyList(),
                        meals = emptyList(),
                        description = "",
                        bookingInfo = emptySet(),
                        taxes = emptyList(),
                        infoForGuest = null,
                    )
                )
            )
        )

        val rsp = processRequest(searchOffersReq(hotelId.toString()))
        val offers = rsp.offers.offerList

        Assert.assertEquals(1, offers.size)
        Assert.assertFalse(offers[0].id.isNullOrEmpty())
        Assert.assertEquals(offerHash, offers[0].externalId)
        Assert.assertEquals(EOperatorId.OI_VIPSERVICE, offers[0].operatorId)
        Assert.assertEquals(price, offers[0].price.amount)
        Assert.assertEquals(ECurrency.C_RUB, offers[0].price.currency)
        Assert.assertEquals(availableRooms, offers[0].availability)
        Assert.assertEquals("==1", offers[0].capacity)
        Assert.assertEquals(1, offers[0].roomCount)
        Assert.assertEquals("==1", offers[0].singleRoomCapacity)
        Assert.assertFalse(offers[0].landingInfo.landingTravelToken.isNullOrEmpty())
        Assert.assertEquals(roomName, offers[0].displayedTitle.value)
        Assert.assertFalse(offers[0].freeCancellation.value)
    }
}
