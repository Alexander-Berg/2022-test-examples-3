package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest

class SupplyControllerGetQualityAttributesForUnitLabelsTest: MvcIntegrationTest() {

    @Test
    @DatabaseSetups(
            DatabaseSetup("classpath:repository/get-attributes-by-unit-label/qattribute.xml"),
            DatabaseSetup("classpath:repository/get-attributes-by-unit-label/unit.xml"),
            DatabaseSetup("classpath:repository/get-attributes-by-unit-label/unit_attribute.xml"),
    )
    fun getAttributesForUnitLabels() {
        testEndpointPostWithLenientCompareMode(
                "/logistic_services/supplies/5/units",
                "controller/getQualityAttributesByUnitLabel/get_quality_attributes_by_unit_label_request.json",
                "controller/getQualityAttributesByUnitLabel/get_quality_attributes_by_unit_label_response.json",
                HttpStatus.OK
        )
    }

}
