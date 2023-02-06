package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import java.nio.charset.StandardCharsets

class NestingControllerTest : IntegrationTest() {

    @ParameterizedTest
    @ValueSource(strings = ["RCP000001", "CDR000001", "BL00000001", "BM00000001"])
    fun checkChildContainerOk(id: String) {
        testGetRequest(
            get("/nesting/checkChild").param("childId", id),
            HttpStatus.OK)
    }

    @ParameterizedTest
    @ValueSource(strings = ["TM00000001", "VS00000001", "CART00001", "WRONGID01"])
    fun checkChildContainerError(id: String) {
        testGetRequestError(
            get("/nesting/checkChild").param("childId", id),
            HttpStatus.BAD_REQUEST,
            "Неподходящий тип дочерней тары $id, id должен соответствовать выражению ^(RCP|CDR|BL|BM)")
    }

    @Test
    @DatabaseSetup("/controller/nesting/check-child-or-get-by-parent-id/before.xml")
    fun checkChildOrGetByParentIdOkChild() {
        testGetRequest(
            get("/nesting/checkChildOrGetByParentId").param("childOrParentId", "RCP03"),
            HttpStatus.OK,
            "controller/nesting/check-child-or-get-by-parent-id/checkChild.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/check-child-or-get-by-parent-id/before.xml")
    fun checkChildOrGetByParentIdOkByParent() {
        testGetRequest(
            get("/nesting/checkChildOrGetByParentId").param("childOrParentId", "TM01"),
            HttpStatus.OK,
            "controller/nesting/check-child-or-get-by-parent-id/getByParentId.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/check-child-or-get-by-parent-id/before.xml")
    fun checkChildOrGetByParentIdError() {
        testGetRequestError(
            get("/nesting/checkChildOrGetByParentId").param("childOrParentId", "WrongId"),
            HttpStatus.BAD_REQUEST,
            "Родительская тара по строке WrongId не найдена, " +
                "для использования в качестве дочерней тары id должен начинаться на RCP или CDR")
    }

    @Test
    @DatabaseSetup("/controller/nesting/parent-possible-usage/before-items-in-parent.xml")
    fun checkParentContainerAllTrue() {
        testGetRequest(
            get("/nesting/parentPossibleUsage").param("parentId", "TM02"),
            HttpStatus.OK,
            "controller/nesting/parent-possible-usage/response-all-true.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/parent-possible-usage/before-items-in-parent.xml")
    fun checkParentContainerIdCleanable() {
        testGetRequest(
            get("/nesting/parentPossibleUsage").param("parentId", "TM00"),
            HttpStatus.OK,
            "controller/nesting/parent-possible-usage/response-id-cleanable.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/parent-possible-usage/before-items-in-parent.xml")
    fun checkParentContainerIdNotCleanable() {
        testGetRequest(
            get("/nesting/parentPossibleUsage").param("parentId", "CDR01"),
            HttpStatus.OK,
            "controller/nesting/parent-possible-usage/response-id-not-cleanable.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/parent-possible-usage/before-items-in-parent.xml")
    fun checkParentContainerSerialCleanable() {
        testGetRequest(
            get("/nesting/parentPossibleUsage").param("parentId", "TM01"),
            HttpStatus.OK,
            "controller/nesting/parent-possible-usage/response-serial-cleanable.json")
    }

    @ParameterizedTest
    @ValueSource(strings = ["TM00000001", "VS00000001", "CART00001"])
    fun checkParentSuccess(id: String) {
        testGetRequest(
            get("/nesting/checkParent").param("parentId", id),
            HttpStatus.OK)
    }

    @ParameterizedTest
    @ValueSource(strings = ["RCP000001", "CDR000001", "BL00000001", "BM00000001", "WRONGID01"])
    fun checkParentWithInvalidParentIdMask(id: String) {
        testGetRequestError(
            get("/nesting/checkParent").param("parentId", id),
            HttpStatus.BAD_REQUEST,
            "Неподходящий тип родительской тары $id")
    }

    @Test
    @DatabaseSetup("/controller/nesting/check-parent/before-non-empty-parent-id.xml")
    fun checkParentWithNonEmptyParentId() {
        testGetRequestError(
            get("/nesting/checkParent").param("parentId", "TM00"),
            HttpStatus.BAD_REQUEST,
            "Родительская тара TM00 содержит УИТы")
    }

    @Test
    fun checkParentWithoutParentId() {
        testGetRequestError(
            get("/nesting/checkParent"),
            HttpStatus.BAD_REQUEST,
            "")
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getParentContainer() {
        testGetRequest(
            get("/nesting/parentContainer").param("childContainerId", "1"),
            HttpStatus.OK,
            "controller/nesting/get-parent-container/response.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getParentContainerNotExist() {
        testGetRequest(
            get("/nesting/parentContainer").param("childContainerId", "CONTAINER_NOT_EXISTS"),
            HttpStatus.OK,
            "controller/nesting/get-parent-container/empty-response.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getParentContainerEmptyParamError() {
        testGetRequest(
            get("/nesting/parentContainer").param("childContainerId", ""),
            HttpStatus.BAD_REQUEST)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getParentContainers() {
        testPostRequest(
            post("/nesting/parentContainers").param("childContainerId", "1"),
            "controller/nesting/get-parent-containers/request.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getChildContainers() {
        testGetRequest(
            get("/nesting/childContainers").param("parentContainerId", "PLT123"),
            HttpStatus.OK,
            "controller/nesting/get-child-containers/response.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getChildContainersNotExist() {
        testGetRequest(
            get("/nesting/childContainers").param("parentContainerId", "CONTAINER_NOT_EXISTS"),
            HttpStatus.OK,
            "controller/nesting/get-child-containers/empty-response.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun getChildContainersEmptyParamError() {
        testGetRequest(
            get("/nesting/childContainers").param("parentContainerId", ""),
            HttpStatus.BAD_REQUEST)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    @ExpectedDatabase(value = "/controller/nesting/create-nesting/after-request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun createNesting() {
        testPostRequest(
            post("/nesting/createNesting"),
            "controller/nesting/create-nesting/request.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun createNestingAnotherNestingExistsError() {
        testPostRequest(
            post("/nesting/createNesting"),
            "controller/nesting/create-nesting/request-another-nesting-exists-error.json",
            HttpStatus.BAD_REQUEST)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun createNestingFewIdenticalChildIdsError() {
        testPostRequest(
            post("/nesting/createNesting"),
            "controller/nesting/create-nesting/request-few-identical-child-ids-error.json",
            HttpStatus.BAD_REQUEST)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun createNestingEmptyList() {
        testPostRequest(
            post("/nesting/createNesting"),
            "controller/nesting/create-nesting/request-empty-list.json",
            HttpStatus.BAD_REQUEST)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    fun createNestingEmptyPair() {
        testPostRequest(
            post("/nesting/createNesting"),
            "controller/nesting/create-nesting/request-empty-pair.json",
            HttpStatus.BAD_REQUEST)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    @ExpectedDatabase(value = "/controller/nesting/create-nesting-with-initiating-user/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun createNestingWithInitiatingUser() {
        testPostRequest(
            post("/nesting/createNesting"),
            "controller/nesting/create-nesting-with-initiating-user/request.json",
            HttpStatus.OK)
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    @ExpectedDatabase(value = "/controller/nesting/delete-by-child-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun deleteNestingByChildId() {
        testPostRequest(
            request = post("/nesting/deleteByChildIds").param("childContainerId", "2"),
            requestFile = "controller/nesting/delete-by-child-id/delete-2.json",
            expectedStatus = HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("/controller/nesting/before.xml")
    @ExpectedDatabase(value = "/controller/nesting/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun deleteNestingByNonExistingChildId() {
        testPostRequest(
            request = post("/nesting/deleteByChildIds"),
            requestFile = "controller/nesting/delete-by-child-id/delete-non-existing.json",
            expectedStatus = HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetup("/controller/nesting/change-parent/before.xml")
    @ExpectedDatabase(value = "/controller/nesting/change-parent/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun changeParentOk() {
        testPostRequest(
            post("/nesting/changeParent"),
            "controller/nesting/change-parent/request.json")
    }

    @Test
    @DatabaseSetup("/controller/nesting/change-parent/before-items-in-parent.xml")
    fun changeParentErrorItemsInParent() {
        testPostRequest(
            post("/nesting/changeParent"),
            "controller/nesting/change-parent/request-items-in-parent.json",
            HttpStatus.BAD_REQUEST,
            "Родительская тара TM01 содержит УИТы")
    }

    @Test
    @DatabaseSetup("/controller/nesting/change-parent/before.xml")
    fun changeParentErrorWrongParentMask() {
        testPostRequest(
            post("/nesting/changeParent"),
            "controller/nesting/change-parent/request-wrong-parent.json",
            HttpStatus.BAD_REQUEST,
            "Неподходящий тип родительской тары WRONG01, id должен соответствовать выражению ^(VS|TM|CART)")
    }

    @Test
    @DatabaseSetup("/controller/nesting/change-parent/before.xml")
    fun changeParentErrorWrongChildMask() {
        testPostRequest(
            post("/nesting/changeParent"),
            "controller/nesting/change-parent/request-wrong-child.json",
            HttpStatus.BAD_REQUEST,
            "Неподходящий тип дочерней тары WRONG01, id должен соответствовать выражению ^(RCP|CDR|BL|BM)")
    }

    private fun testGetRequestError(
        request: MockHttpServletRequestBuilder,
        expectedStatus: HttpStatus,
        errorDescription: String,
    ) = testGetRequest(request, expectedStatus, null, errorDescription)

    private fun testGetRequest(
        request: MockHttpServletRequestBuilder,
        expectedStatus: HttpStatus = HttpStatus.OK,
        expectedFileResponse: String? = null,
        errorDescription: String? = null,
    ) {
        val mvcResult = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().`is`(expectedStatus.value()))
            .andReturn()

        if (null != expectedFileResponse) {
            JsonAssertUtils.assertFileNonExtensibleEquals(expectedFileResponse,
                mvcResult.response.getContentAsString(StandardCharsets.UTF_8))
        }

        if (null != errorDescription) {
            assertions.assertThat(mvcResult.response.getContentAsString(StandardCharsets.UTF_8))
                .contains(errorDescription)
        }
    }

    private fun testPostRequest(
        request: MockHttpServletRequestBuilder,
        requestFile: String,
        expectedStatus: HttpStatus = HttpStatus.OK,
        errorDescription: String? = null,
    ) {
        val mvcResult = mockMvc.perform(request
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent(requestFile)))
            .andExpect(status().`is`(expectedStatus.value()))
            .andReturn()

        if (errorDescription != null) {
            assertions.assertThat(mvcResult.response.getContentAsString(StandardCharsets.UTF_8))
                .contains(errorDescription)
        }
    }
}
