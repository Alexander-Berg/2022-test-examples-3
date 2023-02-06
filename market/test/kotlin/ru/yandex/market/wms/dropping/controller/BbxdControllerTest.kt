package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.configuration.CoreWebClientConfig
import ru.yandex.market.wms.dropping.HttpAssert

class BbxdControllerTest : IntegrationTest() {

    @Autowired
    @MockBean
    protected lateinit var coreClient: CoreClient

    @MockBean(name = "coreHttpClientSettings")
    @Qualifier(CoreWebClientConfig.CORE_CLIENT)
    private lateinit var coreHttpClientSettings: HttpClientSettings

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/get-available-boxes-to-drop/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAvailableBoxesToDrop1() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallOk(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/1/request.json",
            responseFile = "controller/dropping/get-available-boxes-to-drop/1/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/2/db.xml")
    fun getAvailableBoxesToDrop2() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallError(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/2/request.json",
            errorFragment = "DropId DRP123 not found",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/3/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/get-available-boxes-to-drop/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getAvailableBoxesToDrop3() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallOk(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/3/request.json",
            responseFile = "controller/dropping/get-available-boxes-to-drop/3/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/4/db.xml")
    fun getAvailableBoxesToDrop4() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallError(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/4/request.json",
            errorFragment = "Не удалось определить количество товаров в коробке",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/5/db.xml")
    fun getAvailableBoxesToDrop5() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1", "BOX2")))
        httpAssert.assertApiCallOk(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/5/request.json",
            responseFile = "controller/dropping/get-available-boxes-to-drop/5/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/7/db.xml")
    fun getAvailableBoxesToDrop7() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1", "BOX2")))
        httpAssert.assertApiCallOk(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/7/request.json",
            responseFile = "controller/dropping/get-available-boxes-to-drop/7/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/10/db.xml")
    fun getAvailableBoxesToDrop10() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallOk(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/10/request.json",
            responseFile = "controller/dropping/get-available-boxes-to-drop/10/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/12/before.xml")
    fun getAvailableBoxesToDrop12() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallError(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/12/request.json",
            errorFragment = "Паллета PLT123 закреплена за другим пользователем WRONG_USER",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-available-boxes-to-drop/13/before.xml")
    fun getAvailableBoxesToDrop13() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallError(
            post("/bbxd/get-available-boxes-to-drop"),
            requestFile = "controller/dropping/get-available-boxes-to-drop/13/request.json",
            errorFragment = "Существует другое задание для SKU Товар ROV0000000000000000359 STORER 465852 и НЗН PLT123",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/choose-drop/1/db.xml")
    fun chooseDrop1() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallOk(
            post("/bbxd/choose-drop"),
            requestFile = "controller/dropping/choose-drop/1/request.json",
            responseFile = "controller/dropping/choose-drop/1/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/choose-drop/3/db.xml")
    fun chooseDrop3() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1", "BOX2")))
        httpAssert.assertApiCallOk(
            post("/bbxd/choose-drop"),
            requestFile = "controller/dropping/choose-drop/3/request.json",
            responseFile = "controller/dropping/choose-drop/3/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/choose-drop/4/db.xml")
    fun chooseDrop4() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1", "BOX2")))
        httpAssert.assertApiCallOk(
            post("/bbxd/choose-drop"),
            requestFile = "controller/dropping/choose-drop/4/request.json",
            responseFile = "controller/dropping/choose-drop/4/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/choose-drop/5/db.xml")
    fun chooseDrop5() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1", "BOX2")))
        httpAssert.assertApiCallOk(
            post("/bbxd/choose-drop"),
            requestFile = "controller/dropping/choose-drop/5/request.json",
            responseFile = "controller/dropping/choose-drop/5/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/choose-drop/7/db.xml")
    fun chooseDrop7() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallOk(
            post("/bbxd/choose-drop"),
            requestFile = "controller/dropping/choose-drop/7/request.json",
            responseFile = "controller/dropping/choose-drop/7/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-skus-on-pallet/1/db.xml")
    fun getSkusOnPallet1() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallOk(
            post("/bbxd/get-skus-on-pallet"),
            requestFile = "controller/dropping/get-skus-on-pallet/1/request.json",
            responseFile = "controller/dropping/get-skus-on-pallet/1/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-skus-on-pallet/2/db.xml")
    fun getSkusOnPallet2() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallError(
            post("/bbxd/get-skus-on-pallet"),
            requestFile = "controller/dropping/get-skus-on-pallet/2/request.json",
            errorFragment = "На отсканированной паллете больше нет коробок",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-skus-on-pallet/3/db.xml")
    fun getSkusOnPallet3() {
        Mockito.`when`(coreClient.getChildContainers("PLT123"))
            .thenReturn(GetChildContainersResponse(listOf("BOX1")))
        httpAssert.assertApiCallError(
            post("/bbxd/get-skus-on-pallet"),
            requestFile = "controller/dropping/get-skus-on-pallet/3/request.json",
            errorFragment = "Паллета PLT123 закреплена за другим пользователем WRONG_USER",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-drop/1/db.xml")
    fun moveDrop1() {
        httpAssert.assertApiCallError(
            post("/bbxd/move-drop"),
            requestFile = "controller/dropping/move-drop/1/request.json",
            errorFragment = "DropId DRP123 not found",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-drop/2/db.xml")
    fun moveDrop2() {
        httpAssert.assertApiCallError(
            post("/bbxd/move-drop"),
            requestFile = "controller/dropping/move-drop/2/request.json",
            errorFragment = "Drop DRP123 is not empty",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-drop/3/db.xml")
    fun moveDrop3() {
        httpAssert.assertApiCallError(
            post("/bbxd/move-drop"),
            requestFile = "controller/dropping/move-drop/3/request.json",
            errorFragment = "Локация BBXD имеет неверный тип",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-drop/4/db.xml")
    fun moveDrop4() {
        httpAssert.assertApiCallError(
            post("/bbxd/move-drop"),
            requestFile = "controller/dropping/move-drop/4/request.json",
            errorFragment = "В локации DROP находятся дропки для разных СД",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-drop/5/db.xml")
    fun moveDrop5() {
        httpAssert.assertApiCallError(
            post("/bbxd/move-drop"),
            requestFile = "controller/dropping/move-drop/5/request.json",
            errorFragment = "В локации DROP находятся дропки для СД Ekat, отличного от Samara",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-drop/6/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-drop/6/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun moveDrop6() {
        httpAssert.assertApiCallOk(
            post("/bbxd/move-drop"),
            requestFile = "controller/dropping/move-drop/6/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-active-task/1/db.xml")
    fun getActiveTask1() {
        httpAssert.assertApiCallOk(
            post("/bbxd/get-active-task"),
            requestFile = "controller/dropping/get-active-task/request.json",
            responseFile = "controller/dropping/get-active-task/1/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-active-task/2/db.xml")
    fun getActiveTask2() {
        httpAssert.assertApiCallError(
            post("/bbxd/get-active-task"),
            requestFile = "controller/dropping/get-active-task/request.json",
            errorFragment = "Incorrect result size: expected 1, actual 2",
            MockMvcResultMatchers.status().is5xxServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-active-task/3/db.xml")
    fun getActiveTask3() {
        httpAssert.assertApiCallOk(
            post("/bbxd/get-active-task"),
            requestFile = "controller/dropping/get-active-task/request.json",
            responseFile = "controller/dropping/get-active-task/3/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-active-task/4/db.xml")
    fun getActiveTask4() {
        httpAssert.assertApiCallOk(
            post("/bbxd/get-active-task"),
            requestFile = "controller/dropping/get-active-task/request.json",
            responseFile = "controller/dropping/get-active-task/4/response.json"
        )
    }
}

