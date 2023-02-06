package ru.yandex.market.logistics.yard.controller

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.ff.client.dto.RequestTypeStatusMapContainer
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class TagControllerTest: AbstractSecurityMockedContextualTest() {

    @Test
    fun getTags() {
        val getRequestsResponse = readFromJson(
            "classpath:fixtures/v2/controller/tagcontroller/ff-wf-response.json",
            RequestTypeStatusMapContainer::class.java
        )
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/v2/controller/tagcontroller/expected.json",
        )
        Mockito.`when`(ffWfApiClient.requestTypeStatusMap)
            .thenReturn(getRequestsResponse)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/tags")
                .param("parentGroupId","3")
                .param("parentId","0")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

}
