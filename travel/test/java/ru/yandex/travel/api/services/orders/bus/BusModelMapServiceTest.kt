package ru.yandex.travel.api.services.orders.bus

import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.travel.api.config.common.EncryptionConfigurationProperties
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter
import ru.yandex.travel.api.services.dictionaries.country.DummyCountryDataProvider
import ru.yandex.travel.api.services.dictionaries.train.settlement.TrainSettlementDataProvider
import ru.yandex.travel.api.services.dictionaries.train.station.TrainStationDataProvider
import ru.yandex.travel.api.services.dictionaries.train.time_zone.TrainTimeZoneDataProvider
import ru.yandex.travel.api.services.orders.BusModelMapService
import ru.yandex.travel.api.spec.DocumentType
import ru.yandex.travel.api.spec.Gender
import ru.yandex.travel.bus.model.*
import ru.yandex.travel.buses.backend.proto.EPointKeyType
import ru.yandex.travel.buses.backend.proto.TPointKey
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit
import ru.yandex.travel.commons.proto.ProtoUtils
import ru.yandex.travel.dicts.rasp.proto.TSettlement
import ru.yandex.travel.dicts.rasp.proto.TStation
import ru.yandex.travel.dicts.rasp.proto.TTimeZone
import ru.yandex.travel.orders.proto.TOrderServiceInfo
import ru.yandex.travel.orders.proto.TServiceInfo
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@RunWith(MockitoJUnitRunner::class)
class BusModelMapServiceTest {
    private val stationDataProvider: TrainStationDataProvider = mock()
    private val settlementDataProvider: TrainSettlementDataProvider = mock()
    private val timezoneDataProvider: TrainTimeZoneDataProvider = mock()
    private val busModelMapService = BusModelMapService(
        ApiTokenEncrypter(EncryptionConfigurationProperties("123")),
        DummyCountryDataProvider(),
        stationDataProvider,
        settlementDataProvider,
        timezoneDataProvider
    )

    private val ekbPointInfo = BusPointInfo().also { pointInfo ->
        pointInfo.type = EPointKeyType.POINT_KEY_TYPE_SETTLEMENT
        pointInfo.id = 54
        pointInfo.pointKey = "c54"
        pointInfo.supplierDescription = "Екатеринбург, Автовокзал"
        pointInfo.title = "Екатеринбург"
        pointInfo.timezone = "Asia/Yekaterinburg"
        pointInfo.longitude = 60.605514
        pointInfo.latitude = 56.838607
    }

    private val ekbStationPointInfo = BusPointInfo().also { pointInfo ->
        pointInfo.type = EPointKeyType.POINT_KEY_TYPE_STATION
        pointInfo.id = 9635953
        pointInfo.pointKey = "s9635953"
        pointInfo.supplierDescription = "Екатеринбург, Автовокзал"
        pointInfo.title = "Екатеринбург, Северный автовокзал"
        pointInfo.address = "ул. Вокзальная, д. 15А; ст. метро \"Уральская\""
        pointInfo.timezone = "Asia/Yekaterinburg"
        pointInfo.longitude = 60.599591
        pointInfo.latitude = 56.858097
    }

    private val orphanStationPointInfo = BusPointInfo().also { pointInfo ->
        pointInfo.type = EPointKeyType.POINT_KEY_TYPE_STATION
        pointInfo.id = 9650963
        pointInfo.pointKey = "s9650963"
        pointInfo.supplierDescription = "Большая Лая"
        pointInfo.title = "Большая Лая, поворот"
        pointInfo.address = ""
        pointInfo.timezone = "Asia/Yekaterinburg"
        pointInfo.longitude = 59.882967118173
        pointInfo.latitude = 58.0431393817267
    }

    private val ekbPointKey = TPointKey.newBuilder()
        .setType(EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)
        .setId(ekbPointInfo.id)
        .build()

    private val ekbStationPointKey = TPointKey.newBuilder()
        .setType(EPointKeyType.POINT_KEY_TYPE_STATION)
        .setId(ekbStationPointInfo.id)
        .build()

    private val orphanStationPointKey = TPointKey.newBuilder()
        .setType(EPointKeyType.POINT_KEY_TYPE_STATION)
        .setId(orphanStationPointInfo.id)
        .build()

    private val spbPointInfo = BusPointInfo().also { pointInfo ->
        pointInfo.id = 2
        pointInfo.type = EPointKeyType.POINT_KEY_TYPE_SETTLEMENT
        pointInfo.pointKey = "c2"
        pointInfo.title = "Санкт-Петербург"
        pointInfo.timezone = "Europe/Moscow"
        pointInfo.supplierDescription = "Санкт-Петербург, Адмиралтейство"
        pointInfo.longitude = 30.315868
        pointInfo.latitude = 59.939095
    }

    @Test
    fun testBuildBusServiceInfo() {
        val busReservation = BusReservation()
        val ride = BusRide()
        ride.benefits = ArrayList()
        ride.departureTime = Instant.now().plus(6, ChronoUnit.HOURS)
        ride.arrivalTime = Instant.now().plus(8, ChronoUnit.HOURS)
        ride.rideId = "test-ride-id"
        ride.pointFrom = ekbPointInfo
        ride.pointTo = spbPointInfo
        ride.bus = "Mercedes 62 места"
        ride.supplier = BusLegalEntity()
        ride.supplier.registerType = BusRegisterType.COMPANY
        ride.supplier.registerNumber = "12345678901"
        ride.supplier.legalName = "ООО \"Люкс-тур\""
        busReservation.ride = ride
        val order = BusesOrder()
        order.id = "test-order-id"
        order.status = BusOrderStatus.BOOKED
        val passenger = BusesPassenger()
        passenger.documentNumber = "1234 123456"
        passenger.documentType = BusDocumentType.RU_PASSPORT
        passenger.citizenship = "RU"
        passenger.firstName = "Иван"
        passenger.middleName = "Иванович"
        passenger.lastName = "Иванов"
        passenger.gender = BusGenderType.MALE
        passenger.birthday = LocalDate.of(1990, 5, 8)
        passenger.ticketType = BusTicketType.FULL
        passenger.seatId = "11"
        passenger.seatPartnerId = "11"
        busReservation.requestPassengers = listOf(passenger)
        val ticket = BusesTicket()
        ticket.id = "test-ticket-id"
        ticket.status = BusTicketStatus.BOOKED
        ticket.price = Money.of(2500, ProtoCurrencyUnit.RUB)
        ticket.partnerFee = Money.of(100, ProtoCurrencyUnit.RUB)
        ticket.yandexFee = Money.of(500, ProtoCurrencyUnit.RUB)
        ticket.passenger = passenger
        order.tickets = listOf(ticket)
        busReservation.order = order
        val tServiceInfo = TOrderServiceInfo.newBuilder().setServiceInfo(
            TServiceInfo.newBuilder().setPayload(ProtoUtils.toTJson(busReservation))
                .setGenericOrderItemState(EOrderItemState.IS_CONFIRMED).build()
        ).build()
        val dto = busModelMapService.buildBusServiceInfo(tServiceInfo, "document-url", "order-id")
        assertThat(dto.getTickets(0).seat).isEqualTo("11")
        assertThat(dto.getTickets(0).price.ticket.value).isEqualTo(2400f)
        assertThat(dto.getTickets(0).price.total.value).isEqualTo(3000f)
        assertThat(dto.getTickets(0).passenger.documentType).isEqualTo(DocumentType.Enum.ru_national_passport)
        assertThat(dto.getTickets(0).passenger.sex).isEqualTo(Gender.Enum.male)
        assertThat(dto.hasDownloadBlankToken()).isTrue
    }

    private fun setupProviders() {
        val ekbTimezoneId = 1

        whenever(timezoneDataProvider.getById(ekbTimezoneId)).thenReturn(
            TTimeZone.newBuilder()
                .setCode(ekbPointInfo.timezone)
                .build()
        )

        whenever(settlementDataProvider.getById(ekbPointInfo.id)).thenReturn(
            TSettlement.newBuilder()
                .setId(ekbPointInfo.id)
                .setTitleDefault(ekbPointInfo.title)
                .setTimeZoneId(ekbTimezoneId)
                .setLatitude(ekbPointInfo.latitude)
                .setLongitude(ekbPointInfo.longitude)
                .build()
        )

        whenever(stationDataProvider.getById(ekbStationPointInfo.id)).thenReturn(
            TStation.newBuilder()
                .setId(ekbStationPointInfo.id)
                .setTitleDefault(ekbStationPointInfo.title)
                .setTimeZoneId(ekbTimezoneId)
                .setLatitude(ekbStationPointInfo.latitude)
                .setLongitude(ekbStationPointInfo.longitude)
                .setLocalAddress(ekbStationPointInfo.address)
                .setSettlementId(ekbPointInfo.id)
                .build()
        )

        whenever(stationDataProvider.getById(orphanStationPointInfo.id)).thenReturn(
            TStation.newBuilder()
                .setId(orphanStationPointInfo.id)
                .setTitleDefault(orphanStationPointInfo.title)
                .setTimeZoneId(ekbTimezoneId)
                .setLatitude(orphanStationPointInfo.latitude)
                .setLongitude(orphanStationPointInfo.longitude)
                .setLocalAddress(orphanStationPointInfo.address)
                .build()
        )
    }

    @Test
    fun `makePointInfo of blank pointKey`() {
        assertThat(
            busModelMapService.makePointInfo("dummy description", TPointKey.getDefaultInstance())
        ).isEqualTo(
            BusPointInfo.builder()
                .supplierDescription("dummy description")
                .build()
        )
    }

    @Test
    fun `makePointInfo of settlement`() {
        setupProviders()

        assertThat(
            busModelMapService.makePointInfo(
                ekbPointInfo.supplierDescription,
                ekbPointKey
            )
        ).isEqualTo(ekbPointInfo)
    }

    @Test
    fun `makePointInfo of station`() {
        setupProviders()

        assertThat(
            busModelMapService.makePointInfo(
                ekbStationPointInfo.supplierDescription,
                ekbStationPointKey
            )
        ).isEqualTo(ekbStationPointInfo)
    }

    @Test
    fun `makeTitlePointInfo from matched settlement`() {
        setupProviders()

        assertThat(
            busModelMapService.makeTitlePointInfo(
                ekbStationPointInfo.supplierDescription,
                ekbPointKey,
                ekbStationPointKey,
            )
        ).isEqualTo(ekbPointInfo.toBuilder()
            .source(BusPointInfoSource.MATCHING)
            .build())
    }

    @Test
    fun `makeTitlePointInfo from matched station`() {
        setupProviders()

        assertThat(
            busModelMapService.makeTitlePointInfo(
                orphanStationPointInfo.supplierDescription,
                orphanStationPointKey,
                orphanStationPointKey,
            )
        ).isEqualTo(orphanStationPointInfo.toBuilder()
            .source(BusPointInfoSource.MATCHING)
            .build())
    }

    @Test
    fun `makeTitlePointInfo from matched station parent`() {
        setupProviders()

        assertThat(
            busModelMapService.makeTitlePointInfo(
                ekbStationPointInfo.supplierDescription,
                ekbStationPointKey,
                ekbStationPointKey,
            )
        ).isEqualTo(ekbPointInfo.toBuilder()
            .source(BusPointInfoSource.MATCHING_PARENT)
            .build())
    }

    @Test
    fun `makeTitlePointInfo from query settlement`() {
        setupProviders()

        assertThat(
            busModelMapService.makeTitlePointInfo(
                orphanStationPointInfo.supplierDescription,
                orphanStationPointKey,
                ekbPointKey,
            )
        ).isEqualTo(ekbPointInfo.toBuilder()
            .supplierDescription(orphanStationPointInfo.supplierDescription)
            .source(BusPointInfoSource.QUERY)
            .build())

        assertThat(
            busModelMapService.makeTitlePointInfo(
                orphanStationPointInfo.supplierDescription,
                TPointKey.getDefaultInstance(),
                ekbPointKey,
            )
        ).isEqualTo(ekbPointInfo.toBuilder()
            .supplierDescription(orphanStationPointInfo.supplierDescription)
            .source(BusPointInfoSource.QUERY)
            .build())
    }

    @Test
    fun `makeTitlePointInfo from query station parent`() {
        setupProviders()

        assertThat(
            busModelMapService.makeTitlePointInfo(
                orphanStationPointInfo.supplierDescription,
                orphanStationPointKey,
                ekbStationPointKey,
            )
        ).isEqualTo(ekbPointInfo.toBuilder()
            .supplierDescription(orphanStationPointInfo.supplierDescription)
            .source(BusPointInfoSource.QUERY_PARENT)
            .build())

        assertThat(
            busModelMapService.makeTitlePointInfo(
                orphanStationPointInfo.supplierDescription,
                TPointKey.getDefaultInstance(),
                ekbStationPointKey,
            )
        ).isEqualTo(ekbPointInfo.toBuilder()
            .supplierDescription(orphanStationPointInfo.supplierDescription)
            .source(BusPointInfoSource.QUERY_PARENT)
            .build())
    }

    @Test
    fun `makeTitlePointInfo from query station`() {
        setupProviders()

        assertThat(
            busModelMapService.makeTitlePointInfo(
                orphanStationPointInfo.supplierDescription,
                TPointKey.getDefaultInstance(),
                orphanStationPointKey,
            )
        ).isEqualTo(orphanStationPointInfo.toBuilder()
            .source(BusPointInfoSource.QUERY)
            .build())
    }
}
