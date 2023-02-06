package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.configuration.CoreWebClientConfig
import ru.yandex.market.wms.dropping.HttpAssert

class DroppingControllerTest : IntegrationTest() {

    @Autowired
    @MockBean
    protected lateinit var coreClient: CoreClient

    @MockBean(name = "coreHttpClientSettings")
    @Qualifier(CoreWebClientConfig.CORE_CLIENT)
    private lateinit var coreHttpClientSettings: HttpClientSettings

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/dropping/get-drop-info/db.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/get-drop-info/db.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun dropInfoOkEmpty() {
        httpAssert.assertApiCallOk(
            post("/get-drop-info"),
            "controller/dropping/get-drop-info/ok-empty-request.json",
            "controller/dropping/get-drop-info/ok-empty-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-drop-info/db.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/get-drop-info/db.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun dropInfoOkInProgress() {
        httpAssert.assertApiCallOk(
            post("/get-drop-info"),
            "controller/dropping/get-drop-info/ok-in-progress-request.json",
            "controller/dropping/get-drop-info/ok-in-progress-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/1-many-orders-in-parcel.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/1-many-orders-in-parcel.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationErrorManyOrders() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/1-many-orders-in-parcel.json",
            "В посылке P000000001 несколько заказов",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/3-order-shipped.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/3-order-shipped.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationOrderShipped() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/3-order-shipped.json",
            "уже привязана к грузовику",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/4-different-delivery.xml")
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/controller/dropping/put-parcel-on-drop/validationerrors/4-different-delivery.xml",
            assertionMode = NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/controller/dropping/put-parcel-on-drop/validationerrors/4-different-delivery-err.xml",
            assertionMode = NON_STRICT_UNORDERED
        )
    )
    fun putParcelValidationDifferentDeliveries() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/4-different-delivery.json",
            ", а дропка будет доставлена ",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/5-incorrect-esd.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/5-incorrect-esd.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationIncorrectESD() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/5-incorrect-esd.json",
            "ПДО дропки - 2020-04-01, ПДО посылки - 2020-04-03",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/5_2-incorrect-esd-2-future.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/5_2-incorrect-esd-2-future.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationIncorrectESDTwoDifferentDaysAfterToday() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/5-incorrect-esd.json",
            "ПДО дропки - 2020-04-06, ПДО посылки - 2020-04-05",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/5_3-incorrect-esd-before-cutoff.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/5_3-incorrect-esd-before-cutoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationIncorrectESDBeforeCutoff() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/5-incorrect-esd.json",
            "ПДО дропки - 2020-03-31, ПДО посылки - 2020-04-01",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/drop-shipped/db.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/drop-shipped/db.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `drop is already shipped`() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/drop-shipped/request.json",
            "Дропка DRP000001 уже отгружена",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/5_4-incorrect-esd-thee-to-empty.xml")
    fun putParcelValidationIncorrectESDThree() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/5_4-incorrect-esd-three-3.json",
        )
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/5_4-incorrect-esd-three-2.json",
            "ПДО дропки - 2020-04-02, ПДО посылки - 2020-04-01",
            MockMvcResultMatchers.status().is4xxClientError
        )
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/5-incorrect-esd.json",
            "ПДО дропки - 2020-04-02, ПДО посылки - 2020-03-31",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/6-already-loaded.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/6-already-loaded.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationAlreadyLoaded() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/6-already-loaded.json",
            "Посылка P000000001 находится в статусе LOADED(8)",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/7-wrong-parcel-id-format.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/7-wrong-parcel-id-format.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelValidationWrongParcelIdFormat() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/7-wrong-parcel-id-format.json",
            "Номер посылки PL00000001 имеет неверный формат",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/1-success-no-lines-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/1-success-no-lines-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel on not empty drop`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/2-success-only-lines-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/2-success-only-lines-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel on empty drop with default loc`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/id-table/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/id-table/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel on empty drop with loc from ID table`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/id-table/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/2-success-only-lines-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/2-success-only-lines-with-scanned-loc-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel on empty drop with scanned loc`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request-with-loc.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/2-success-only-lines-before.xml")
    fun `put parcel on empty drop with WRONG scanned loc`() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request-with-wrong-loc.json",
            "Сортировать посылки по СД можно только в ячейках для сортировки по СД. Текущая ячейка: PACK",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/3-success-full-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/3-success-full-order-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelSuccessFullOrder() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    /**
     * статус заказа не меняется и в историю не проставляется
     */
    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/3-success-full-order-92-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/3-success-full-order-92-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel successfully when full order and status 92`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    /**
     * статус заказа не меняется, в историю не проставляется
     */
    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/new-history/not-last-box-65-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/new-history/not-last-box-65-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel successfully when NOT LAST box and status 65 and using new history`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    /**
     * статус заказа меняется, в историю проставляется
     */
    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/new-history/last-box-65-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/new-history/last-box-65-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel successfully when LAST box and status 65 and using new history`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    /**
     * статус заказа не меняется, в историю не проставляется
     */
    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/new-history/not-last-box-92-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/new-history/not-last-box-92-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel successfully when NOT LAST box and status 92 and using new history`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    /**
     * статус заказа не меняется, но в историю проставляется
     */
    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/new-history/last-box-92-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/new-history/last-box-92-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel successfully when LAST box and status 92 and using new history`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/4-success-from-one-to-another-before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/4-success-from-one-to-another-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun putParcelSuccessFromOneToAnother() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/dropping/put-parcel-on-drop/successes/drop-moved/before.xml",
        "/controller/dropping/put-parcel-on-drop/successes/drop-moved/config.xml"
    )
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/drop-moved/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel successfully when drop already moved from initial location`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/drop-moved/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/drop-moved/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/drop-moved/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `FAIL to put parcel when drop already moved from initial location`() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/drop-moved/request.json",
            "Сортировать посылки по СД можно только в ячейках для сортировки по СД. Текущая ячейка: LOC1",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/must-scan-loc/db.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/must-scan-loc/db.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `FAIL to put parcel on default loc when must scan loc for empty drop`() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/must-scan-loc/request.json",
            "Нужно сканировать ячейку, дефолтные ячейки запрещены: DROP",
            MockMvcResultMatchers.status().is4xxClientError
        )

        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/must-scan-loc/request-with-loc.json",
            "Нужно сканировать ячейку, дефолтные ячейки запрещены: DROP",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/dropping/put-parcel-on-drop/successes/with-err/before.xml",
        "/controller/dropping/put-parcel-on-drop/successes/with-err/err-minute-ago-before.xml"
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/controller/dropping/put-parcel-on-drop/successes/with-err/after.xml",
            assertionMode = NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/controller/dropping/put-parcel-on-drop/successes/with-err/err-minute-ago-after.xml",
            assertionMode = NON_STRICT_UNORDERED
        )
    )
    fun `put parcel successfully with error happened a minute ago`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/dropping/put-parcel-on-drop/successes/with-err/before.xml",
        "/controller/dropping/put-parcel-on-drop/successes/with-err/err-long-time-ago-before.xml"
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/controller/dropping/put-parcel-on-drop/successes/with-err/after.xml",
            assertionMode = NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/controller/dropping/put-parcel-on-drop/successes/with-err/err-long-time-ago-after.xml",
            assertionMode = NON_STRICT_UNORDERED
        )
    )
    fun `put parcel successfully with error happened long time ago`() {
        httpAssert.assertApiCallOk(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/successes/with-err/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/successes/with-err/err-drop-name-after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel on drop with WRONG name`() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/successes/request-with-wrong-drop-name.json",
            "ШК DRP0000001DRP0000001 не подходит",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/put-parcel-on-drop/validationerrors/8-wrong-pickdetail-status-on-drop.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/put-parcel-on-drop/validationerrors/8-wrong-pickdetail-status-on-drop.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `put parcel on drop with picks in LOADED status`() {
        httpAssert.assertApiCallError(
            post("/put-parcel-on-drop"),
            "controller/dropping/put-parcel-on-drop/validationerrors/8-wrong-pickdetail-status-on-drop.json",
            "Дропка DRP0000001 перемещена в машину",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsSuccess() {
        httpAssert.assertApiCallOk(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/successes/request.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsEmptyFromDropError() {
        httpAssert.assertApiCallError(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/errors/from-empty-drop-request.json",
            "На дропке DRP0000001 нет посылок",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsEmptyToDropError() {
        httpAssert.assertApiCallError(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/errors/from-empty-to-drop-request.json",
            "На дропке DRP0000001 нет посылок",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsBadParcelVerificationError() {
        httpAssert.assertApiCallError(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/errors/bad-parcel-request.json",
            "На дропке DRP0000002 отсутствует P000000003",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsBadDropNameError() {
        httpAssert.assertApiCallError(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/errors/bad-drop-request.json",
            "ШК DP0000003 не подходит",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsSelfError() {
        httpAssert.assertApiCallError(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/errors/self-request.json",
            "Дропка назначения DRP0000002 совпадает с дропкой источника DRP0000002",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/move-parcels-between-drops/successes/before.xml")
    @ExpectedDatabase(
        value = "/controller/dropping/move-parcels-between-drops/successes/before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun moveParcelsBadCarrierError() {
        httpAssert.assertApiCallError(
            post("/move-parcels-between-drops"),
            "controller/dropping/move-parcels-between-drops/errors/bad-carrier-request.json",
            "Посылка должна быть доставлена Test carrier 2, а дропка будет доставлена Test carrier 1",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-parcel-info/db.xml")
    @ExpectedDatabase(value = "/controller/dropping/get-parcel-info/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getParcelInfoOk() {
        httpAssert.assertApiCallOk(
            post("/get-parcel-info"),
            requestFile = "controller/dropping/get-parcel-info/ok-request.json",
            responseFile = "controller/dropping/get-parcel-info/ok-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-parcel-info/db.xml")
    @ExpectedDatabase(value = "/controller/dropping/get-parcel-info/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getParcelInfoCancelled() {
        httpAssert.assertApiCallOk(
            post("/get-parcel-info"),
            requestFile = "controller/dropping/get-parcel-info/cancelled-request.json",
            responseFile = "controller/dropping/get-parcel-info/cancelled-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-parcel-info/db.xml")
    @ExpectedDatabase(value = "/controller/dropping/get-parcel-info/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getParcelInfoOkWhenDropped() {
        httpAssert.assertApiCallOk(
            post("/get-parcel-info"),
            requestFile = "controller/dropping/get-parcel-info/ok-dropped-request.json",
            responseFile = "controller/dropping/get-parcel-info/ok-dropped-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-parcel-info/db.xml")
    @ExpectedDatabase(value = "/controller/dropping/get-parcel-info/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getParcelInfoWrongParcelIdFormat() {
        httpAssert.assertApiCallError(
            post("/get-parcel-info"),
            requestFile = "controller/dropping/get-parcel-info/parcel-format-error-request.json",
            errorFragment = "Номер посылки PL00000001 имеет неверный формат",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/dropping/get-parcel-info/db.xml")
    @ExpectedDatabase(value = "/controller/dropping/get-parcel-info/db.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getParcelInfoNotFound() {
        httpAssert.assertApiCallError(
            post("/get-parcel-info"),
            requestFile = "controller/dropping/get-parcel-info/not-found-error-request.json",
            errorFragment = "Посылка P000002002 не найдена в системе",
            MockMvcResultMatchers.status().is4xxClientError
        )
    }
}
