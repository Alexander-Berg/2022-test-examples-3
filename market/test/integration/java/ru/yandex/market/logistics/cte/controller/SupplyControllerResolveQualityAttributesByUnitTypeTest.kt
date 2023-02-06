package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest
import ru.yandex.market.logistics.cte.client.enums.UnitType

class SupplyControllerResolveQualityAttributesByUnitTypeTest: MvcIntegrationTest() {

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qattribute.xml"),
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/group.xml"),
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qmatrix_group.xml"),
    )
    fun matrixFound() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("unitType", UnitType.BOX.id)
        }

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-unit_type",
            params,
            "controller/resolveQualityAttributesByUnitType/matrix_found_response.json", HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qattribute.xml"),
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/group.xml"),
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qmatrix_group.xml"),
    )
    fun noMatrix() {

        val params = LinkedMultiValueMap<String, String>().apply{
            add("unitType", UnitType.PALLET.id)
        }

        testGetEndpoint(
            "/logistic_services/quality-attributes/find-by-unit_type",
            params,
            "controller/resolveQualityAttributesByUnitType/no_matrix_response.json", HttpStatus.OK
        )
    }
}
