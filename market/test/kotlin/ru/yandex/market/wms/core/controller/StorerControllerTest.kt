package ru.yandex.market.wms.core.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.model.enums.AuthenticationParam
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.core.HttpAssert
import javax.servlet.http.Cookie

/**
 * Tests of [StorerController].
 */
class StorerControllerTest : IntegrationTest() {
    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_empty.xml")
    fun `GET storer empty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/storer"),
            responseFile = "controller/storer/storer/get/storer_empty_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `GET storer`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/storer"),
            responseFile = "controller/storer/storer/get/storer_default_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `GET storer with order by storerKey`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/storer?sort=storerKey"),
            responseFile = "controller/storer/storer/get/storer_default_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `GET storer filter by STORERKEY`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/storer?filter=storerKey==1"),
            responseFile = "controller/storer/storer/get/storer_filter_by_storerkey_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `GET storer filter by TYPE`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/storer?filter=type==SHIP_TO"),
            responseFile = "controller/storer/storer/get/storer_filter_by_type_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `GET storer filter by MULTIPACKING`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/storer?filter=multipacking==true"),
            responseFile = "controller/storer/storer/get/storer_filter_by_multipacking_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `INSERT storer`() {
        val mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/storer/create/STORER/666")
                .content(
                    """
                {
                    "storerKey": "666",
                    "type": "STORER",
                    "company": "Company #1",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "maximumOrders": 600,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """
                )
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        JSONAssert.assertEquals(
            """
                {
                        "storerKey": "666",
                        "type": "STORER",
                        "company": "Company #1",
                        "description": "Description",
                        "receiptValidationTemplate": "100",
                        "inboundLpnLength": 200,
                        "skuSetupRequired": "1",
                        "lpnRollbackNumber": "300",
                        "lpnBarcodeSymbology": "400",
                        "minimumPercent": 500.0,
                        "maximumOrders": 600,
                        "multipacking": true,
                        "cartonGroup": "700",
                        "lpnLength": 800,
                        "nextLpnNumber": "900",
                        "lpnStartNumber": "1000",
                        "caseLabelType": "1100"
                }
            """,
            mvcResult.response.contentAsString,
            STRICT
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `UPDATE storer`() {
        val mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/storer/update/STORER/1")
                .content(
                    """
                {
                    "storerKey": "1",
                    "type": "STORER",
                    "company": "Company #1",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """
                )
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        JSONAssert.assertEquals(
            """
                {
                        "storerKey": "1",
                        "type": "STORER",
                        "company": "Company #1",
                        "description": "Description",
                        "receiptValidationTemplate": "100",
                        "inboundLpnLength": 200,
                        "skuSetupRequired": "1",
                        "lpnRollbackNumber": "300",
                        "lpnBarcodeSymbology": "400",
                        "minimumPercent": 500.0,
                        "multipacking": true,
                        "cartonGroup": "700",
                        "lpnLength": 800,
                        "nextLpnNumber": "900",
                        "lpnStartNumber": "1000",
                        "caseLabelType": "1100"
                }
            """,
            mvcResult.response.contentAsString,
            STRICT
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_duplicates.xml")
    fun `UPDATE only one storer`() {
        val mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/storer/update/SHIP_TO/1")
                .content(
                    """
                {
                    "storerKey": "1",
                    "type": "SHIP_TO",
                    "company": "Company #SHIP_TO",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "maximumOrders": 600,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """
                )
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        JSONAssert.assertEquals(
            """
                {
                    "storerKey": "1",
                    "type": "SHIP_TO",
                    "company": "Company #SHIP_TO",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "maximumOrders": 600,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """,
            mvcResult.response.contentAsString,
            STRICT
        )
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_duplicates.xml")
    fun `UPDATE storer will failed if storer doesn't exist`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/storer/update/SUPPLIER/1")
                .content(
                    """
                {
                    "storerKey": "1",
                    "type": "SUPPLIER",
                    "company": "Company #SUPPLIER",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "maximumOrders": 600,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """
                )
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    internal fun `UPDATE storer will fail if type doesn't match with body`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/storer/update/STORER/1")
                .content(
                    """
                {
                    "storerKey": "1",
                    "type": "SUPPLIER",
                    "company": "Company #SUPPLIER",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "maximumOrders": 600,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """
                )
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    internal fun `UPDATE storer will fail if storerKey doesn't match with body`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/storer/update/SUPPLIER/2")
                .content(
                    """
                {
                    "storerKey": "1",
                    "type": "SUPPLIER",
                    "company": "Company #SUPPLIER",
                    "description": "Description",
                    "receiptValidationTemplate": "100",
                    "inboundLpnLength": 200,
                    "skuSetupRequired": "1",
                    "lpnRollbackNumber": "300",
                    "lpnBarcodeSymbology": "400",
                    "minimumPercent": 500.0,
                    "maximumOrders": 600,
                    "multipacking": true,
                    "cartonGroup": "700",
                    "lpnLength": 800,
                    "nextLpnNumber": "900",
                    "lpnStartNumber": "1000",
                    "caseLabelType": "1100"
                }
            """
                )
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/storer/storer/get/before_fill.xml")
    fun `DELETE storer`() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/storer/delete/STORER/1")
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        // get all the data
        val mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/storer")
                .cookie(Cookie(AuthenticationParam.USERNAME.code, "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        JSONAssert.assertEquals(
            FileContentUtils.getFileContent("controller/storer/storer/get/storer_delete_response.json"),
            mvcResult.response.contentAsString,
            STRICT
        )
    }
}
