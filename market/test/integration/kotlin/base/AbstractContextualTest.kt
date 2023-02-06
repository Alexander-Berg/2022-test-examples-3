package ru.yandex.market.logistics.calendaring.base

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.nhaarman.mockitokotlin2.*
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.UnknownHttpStatusCodeException
import ru.yandex.market.ff.client.dto.quota.*
import ru.yandex.market.logistics.calendaring.config.DbqueueNotActiveIntegrationTestConfig
import ru.yandex.market.logistics.calendaring.config.IntegrationTestConfig
import ru.yandex.market.logistics.calendaring.model.dto.TimezoneDTO
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@SpringBootTest(
    classes = [
        DbqueueNotActiveIntegrationTestConfig::class,
        IntegrationTestConfig::class,
    ]
)
@DbUnitConfiguration(
    databaseConnection = ["dbUnitDatabaseConnection", "dbqueueDatabaseConnection"],
    dataSetLoader = NullableColumnsDataSetLoader::class
)
@DatabaseSetups(DatabaseSetup(value = ["classpath:fixtures/request-status.xml"]))
abstract class AbstractContextualTest : IntegrationTest() {

    private val regionId = 213L
    private val partnerResponseDefaultBuilder = PartnerResponse.newBuilder()
        .locationId(regionId.toInt())
        .readableName("Test")
        .params(listOf(PartnerExternalParam("IS_CALENDARING_ENABLED", "", "false")))
        .partnerType(PartnerType.FULFILLMENT)


    protected fun setUpMockFfwfGetQuota(
        dates: Set<LocalDate> = setOf(LocalDate.of(2021, 5, 17)),
        items: Long = 50L,
        pallets: Long = 2L
    ) {
        Mockito.`when`(ffwfClientApi!!.getQuota(any())).thenReturn(GetQuotaResponseDto.builder()
            .availableQuotasForServices(listOf(
                AvailableQuotasForServiceDto.builder()
                    .availableQuotasForDates(dates.map {
                        AvailableQuotasForDateDto.builder()
                            .date(it)
                            .items(items)
                            .pallets(pallets).build()
                    }
                    ).build()
            )).build())
    }

    protected fun setUpMockFfwfGetQuotas(
        date: LocalDate = LocalDate.of(2021, 5, 17)
    ) {
        Mockito.`when`(ffwfClientApi!!.takeOrUpdateConsolidatedQuotas(any())).thenReturn(TakeQuotasManyBookingsResponseDto.builder()
            .quotaDate(date).build())
    }

    protected fun setUpMockFfwfTakeQuota(localDate: LocalDate? = LocalDate.of(2021, 5, 17)) {
        Mockito.`when`(ffwfClientApi!!.takeQuota(any()))
            .thenReturn(TakeQuotaResponseDto(localDate))
    }

    protected fun setUpMockFfwfFindByBookingId(bookingId: Long, quotaDate: LocalDate) {
        Mockito.`when`(ffwfClientApi!!.findByBookingId(bookingId)).thenReturn(
            TakeQuotaResponseDto(quotaDate)
        )
    }

    protected fun setUpMockFfwfTakeQuotaHttpStatusCodeException(
        statusCode: Int
    ): OngoingStubbing<TakeQuotaResponseDto> {
        return Mockito.`when`(ffwfClientApi!!.takeQuota(any()))
            .thenThrow(HttpStatus.resolve(statusCode)?.let { HttpClientErrorException(it) }
                ?: UnknownHttpStatusCodeException(statusCode, "unknown", null, null, null))
    }

    protected fun setUpMockFfwfTakeQuotaReturnAfterHttpStatusCodeException(statusCode: Int) {
        setUpMockFfwfTakeQuotaHttpStatusCodeException(statusCode)
            .thenReturn(TakeQuotaResponseDto(LocalDate.of(2021, 5, 17)))
    }


    protected fun setUpMockLmsGetLocationZone(
        geobaseProviderApi: GeobaseProviderApi,
        zone: ZoneId = ZoneId.of("Europe/Moscow")
    ) {

        Mockito.`when`(lmsClient!!.getLogisticsPoint(any())).thenReturn(
            Optional.of(
                LogisticsPointResponse.newBuilder().address(
                    Address.newBuilder().locationId(regionId.toInt()).build()
                ).build()
            )
        )

        Mockito.`when`(lmsClient!!.getPartner(any())).thenReturn(Optional.of(partnerResponseDefaultBuilder.build()))

        Mockito.`when`(geobaseProviderApi.getZoneByRegionId(eq(regionId)))
            .thenReturn(TimezoneDTO(regionId, zone.toString()))
    }

    protected fun setUpMockLmsGetSchedule(gateType: GateTypeResponse) {
        Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(any(), any(), any())).thenReturn(
            MockParametersHelper.mockGatesScheduleResponse(
                MockParametersHelper.mockAvailableGatesResponse(
                    setOf(1L, 2L, 3L), EnumSet.of(gateType),
                    MockParametersHelper.mockGatesSchedules(
                        LocalTime.of(9, 0),
                        LocalTime.of(20, 0),
                        LocalDate.of(2021, 5, 17),
                    )
                ),
            )
        )
    }

    protected fun setupLmsGateSchedule(
        warehouseIds: List<Long> = listOf(1L),
        from: LocalTime = LocalTime.of(10, 0),
        to: LocalTime = LocalTime.of(22, 0),
        gateType: GateTypeResponse = GateTypeResponse.INBOUND,
        workingDays: Set<LocalDate> = setOf(LocalDate.of(2021, 5, 17), LocalDate.of(2021, 5, 18)),
        gates: Set<Long> = setOf(1L, 2L)
    ) {
        for (warehouseId in warehouseIds) {
            Mockito.`when`(lmsClient!!.getWarehousesGatesCustomScheduleByPartnerId(eq(warehouseId), any(), any())).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                    MockParametersHelper.mockAvailableGatesResponse(
                        gates, EnumSet.of(gateType),
                        workingDays.map {
                            MockParametersHelper.mockGatesSchedules(
                                it,
                                from,
                                to
                            )
                        }
                    ),
                )
            )
        }

    }

    protected fun setUpMockLmsGetPartner(partnerType: PartnerType = PartnerType.FULFILLMENT) {
        Mockito.`when`(lmsClient!!.getPartner(any())).thenReturn(
            Optional.ofNullable(partnerResponseDefaultBuilder.partnerType(partnerType).build())
        )
    }

    protected fun verifyBasicFfwfCommunication(
        getQuotaInvCnt: Int = 1,
        takeQuotaInvCnt: Int = 1,
    ): KArgumentCaptor<TakeQuotaDto> {
        verify(ffwfClientApi!!, times(getQuotaInvCnt)).getQuota(any())
        val takeQuotaCaptor = argumentCaptor<TakeQuotaDto>()
        verify(ffwfClientApi!!, times(takeQuotaInvCnt)).takeQuota(takeQuotaCaptor.capture())
        return takeQuotaCaptor
    }

}
