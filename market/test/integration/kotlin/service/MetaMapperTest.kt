package ru.yandex.market.logistics.calendaring.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.config.idm.IdmRoleSecurityConfigurationAdapter
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MetaMapperTest(
    @Autowired var geobaseProviderApi: GeobaseProviderApi,
) : AbstractContextualTest() {

    @BeforeEach
    fun init() {
        setUpMockLmsGetLocationZone(geobaseProviderApi)
    }

    @Test
    @DatabaseSetup(
        "classpath:" +
            "fixtures/controller/calendaring-info/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml",
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/service/mapper/meta-field-mapping.xml"
    )
    fun testMetaMapping() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent(
                "fixtures/service/mapper/response_with_mapped_meta.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(
        "classpath:" +
            "fixtures/controller/calendaring-info/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml",
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/service/mapper/meta-field-mapping-when-label-null.xml"
    )
    fun testMetaMappingWhenLabelIsNull() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent(
                "fixtures/service/mapper/response_with_mapped_meta-when-label-null.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }


    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml",
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/service/mapper/non-meta-field-mapping.xml"
    )
    fun testNonMetaFieldMapping() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent(
                "fixtures/service/mapper/response_with_mapped_non_meta_field.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml",
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/service/mapper/meta-template-mapping.xml"
    )
    fun testTemplateMapping() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent(
                "fixtures/service/mapper/response_with_mapped_template.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml",
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/service/mapper/meta-mapping-with-cast.xml"
    )
    fun testMappingWithCast() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent(
                "fixtures/service/mapper/response_with_mapped_meta_cast.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup(
        "classpath:fixtures/service/mapper/before.xml",
        "classpath:fixtures/controller/calendaring-info/userHasIdmRole.xml",
        "classpath:fixtures/service/mapper-selector/before.xml",
        "classpath:fixtures/service/mapper/meta-mapping-with-filter.xml"
    )
    fun testMappingWithFilter() {
        val testDate = LocalDate.of(2021, 5, 17)
        setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate)

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/123/" + testDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/INBOUND")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "test_login")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val expectedJson: String = FileContentUtils
            .getFileContent(
                "fixtures/service/mapper/response_with_mapped_meta_filter.json")
        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }


    private fun setUpMockLmsGetWarehousesGatesScheduleByPartnerId(testDate: LocalDate) {
        setupLmsGateSchedule(
            warehouseIds = listOf(123),
            from = LocalTime.of(9, 0),
            to= LocalTime.of(20, 0),
            workingDays = setOf(testDate)
        )
    }

}
