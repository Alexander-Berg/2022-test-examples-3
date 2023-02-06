package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest

class QualityAttributeControllerTest: MvcIntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/empty.xml")
    @ExpectedDatabase(value = "classpath:controller/quality-attribute/create/qattribute.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(Exception::class)
    fun addQualityAttribute() {
        testEndpointPostStatus(
            "/quality_attribute/create",
            "controller/quality-attribute/create/request.json",
            HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/quality-attribute/create/qattribute.xml")
    @ExpectedDatabase(value = "classpath:controller/quality-attribute/create/qattribute.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(Exception::class)
    fun addQualityAttributeWithDuplicateNameAndRefId() {
        testEndpointPostStatus(
            "/quality_attribute/create",
            "controller/quality-attribute/create/request.json", HttpStatus.CONFLICT
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/quality-attribute/create/qattribute.xml")
    @ExpectedDatabase(value = "classpath:controller/quality-attribute/create/qattribute.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    @Throws(Exception::class)
    fun addQualityAttributeWithNullValues() {
        testEndpointPostStatus(
            "/quality_attribute/create",
            "controller/quality-attribute/create/request_with_null_values.json", HttpStatus.BAD_REQUEST
        )
    }

    @Test
    @DatabaseSetup("classpath:controller/quality-attribute/create/quality_group.xml")
    @ExpectedDatabases(
        ExpectedDatabase(value = "classpath:controller/quality-attribute/create/qattribute.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT),
        ExpectedDatabase(value = "classpath:controller/quality-attribute/create/group_attribute.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT))
    @Throws(Exception::class)
    fun addQualityAttributeWithGroup() {
        testEndpointPostStatus(
            "/quality_attribute/create",
            "controller/quality-attribute/create/request_with_group.json",
            HttpStatus.OK
        )
    }

    @Test
    @Throws(Exception::class)
    @DatabaseSetup("classpath:service/empty.xml")
    fun addQualityAttributeNoSuchGroup() {
        testEndpointPostStatus(
            "/quality_attribute/create",
            "controller/quality-attribute/create/request_with_group.json",
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @Test
    fun listQualityAttributeValues() {
        testGetEndpoint(
            "/quality_attribute/value/list",
            LinkedMultiValueMap(),
            "controller/quality-attribute/quality-attribute-value-list/response.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/qattribute.xml")
    )
    fun qualityAttributeList() {
        testGetEndpoint(
            "/quality_attribute/list",
            LinkedMultiValueMap(),
            "controller/quality-attribute/list/response.json",
            HttpStatus.OK)
    }
}
