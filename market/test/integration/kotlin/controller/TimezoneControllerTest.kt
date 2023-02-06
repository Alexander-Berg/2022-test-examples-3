package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.apache.http.entity.ContentType
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.model.dto.TimezoneDTO
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseTimezoneService
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import java.time.ZoneId
import java.util.*

class TimezoneControllerTest : AbstractContextualTest() {

    @Autowired
    lateinit var geobaseProviderApi: GeobaseProviderApi

    @Autowired
    lateinit var geobaseTimezoneService: GeobaseTimezoneService

    private val INVALID_WAREHOUSE_ID: Long = 0
    private val ZERO_REGION_ID: Long = 0
    private val OMSK_REGION_ID: Long = 66
    private val SPB_REGION_ID: Long = 2
    private val SPB_TZNAME: String = "Europe/Moscow"
    private val OMSK_TZNAME: String = "Asia/Omsk"

    private val EKB_REGION_ID: Long = 54
    private val MARKET_EKB_HAREHOUSE: Long = 300
    private val EKB_TZNAME: String = "Asia/Yekaterinburg"

    @Test
    @DatabaseSetup("classpath:fixtures/controller/timezone/get-new-timezone-and-save/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/timezone/get-new-timezone-and-save/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun getNewTimezoneAndSaveSuccessfully() {
        Mockito.`when`(geobaseProviderApi.getZoneByRegionId(OMSK_REGION_ID))
            .thenReturn(TimezoneDTO(OMSK_REGION_ID, OMSK_TZNAME))

        val zoneByRegionIdForOmsk = geobaseTimezoneService.getZoneByRegionId(OMSK_REGION_ID)

        Assert.assertEquals(ZoneId.of(OMSK_TZNAME), zoneByRegionIdForOmsk)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/timezone/get-timezone/get-new-timezone.xml")
    fun getTimezoneSuccessfully() {
        val zoneByRegionIdForSpb = geobaseTimezoneService.getZoneByRegionId(SPB_REGION_ID)

        Assert.assertEquals(ZoneId.of(SPB_TZNAME), zoneByRegionIdForSpb)
    }

    @Test
    fun failedGetZoneForInvalidRegionID() {

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/get-zone/" + ZERO_REGION_ID)
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("\"UTC\""))

    }

    @Test
    fun testGetZoneForWarehouseIdSuccessfully() {
        Mockito.`when`(lmsClient!!.getPartner(MARKET_EKB_HAREHOUSE)).thenReturn(
            Optional.of(
                PartnerResponse
                    .newBuilder()
                    .locationId(EKB_REGION_ID.toInt())
                    .build()
            )
        )

        Mockito.`when`(geobaseProviderApi.getZoneByRegionId(EKB_REGION_ID))
            .thenReturn(TimezoneDTO(EKB_REGION_ID, EKB_TZNAME))

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/get-zone/by-warehouse/$MARKET_EKB_HAREHOUSE")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(content().json("{\"tzname\":\"$EKB_TZNAME\"}"))

    }

    @Test
    fun failedGetZoneForInvalidWarehouseId() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/get-zone/by-warehouse/$INVALID_WAREHOUSE_ID")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isNotFound)
            .andExpect(content().string("{\"message\":\"Cannot find warehouse with id $INVALID_WAREHOUSE_ID\"}"))
    }
}
