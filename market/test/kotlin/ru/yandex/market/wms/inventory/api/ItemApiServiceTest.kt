package ru.yandex.market.wms.inventory.api

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.Test

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemApiServiceTest(
    private val jdbcTemplate: JdbcTemplate
) : AbstractApiTest() {
    @BeforeEach
    fun setUp() = jdbcTemplate.execute("alter sequence item_id_seq restart with 1;")

    //1)ean нет в identity
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/item/find-by-ean/ean-not-found/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/item/find-by-ean/ean-not-found/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun eanNotFound() {
        assertApiCallClientError(
            "json/api/item/find-by-ean/ean-not-found/request.json",
            MockMvcRequestBuilders.post("/item/find-by-ean"),
            "ITEM_NOT_FOUND"
        )
    }

    //2) такие циферки есть в identity, но это не еан
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/item/find-by-ean/illegal-identity/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/item/find-by-ean/illegal-identity/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun illegalIdentityScanned() {
        assertApiCallClientError(
            "json/api/item/find-by-ean/illegal-identity/request.json",
            MockMvcRequestBuilders.post("/item/find-by-ean"),
            "IDENTITY_SCANNED_ILLEGAL"
        )
    }

    //3) есть две разных партии с таким еаном ->
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/item/find-by-ean/duplicate-ean/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/item/find-by-ean/duplicate-ean/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun itemNotFoundUniquely() {
        assertApiCallClientError(
            "json/api/item/find-by-ean/duplicate-ean/request.json",
            MockMvcRequestBuilders.post("/item/find-by-ean"),
            "ITEM_NOT_FOUND_UNIQUELY"
        )
    }

    //4) есть 1 партия с таким еаном
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/item/find-by-ean/success/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/item/find-by-ean/success/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun itemFindByEanSuccess() {
        assertApiCall(
            "json/api/item/find-by-ean/success/request.json",
            "json/api/item/find-by-ean/success/response.json",
            MockMvcRequestBuilders.post("/item/find-by-ean"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    // Scan already invented sku
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/item/find-by-ean/already-invented/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/item/find-by-ean/already-invented/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun skuAlreadyInvented() {
        assertApiCallClientError(
            "json/api/item/find-by-ean/already-invented/request.json",
            MockMvcRequestBuilders.post("/item/find-by-ean"),
            "SKU_ALREADY_INVENTED"
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/item/find-by-ean/duplicate-ean-one-invented/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/item/find-by-ean/duplicate-ean-one-invented/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun skuDuplicateOneAlreadyInvented() {
        assertApiCall(
            "json/api/item/find-by-ean/duplicate-ean-one-invented/request.json",
            "json/api/item/find-by-ean/duplicate-ean-one-invented/response.json",
            MockMvcRequestBuilders.post("/item/find-by-ean"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }
}
