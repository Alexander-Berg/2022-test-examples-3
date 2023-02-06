package ru.yandex.market.wms.consolidation.modules.preconsolidation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.transportation.client.TransportationClient
import ru.yandex.market.wms.transportation.core.domain.TransportOrderStatus
import ru.yandex.market.wms.transportation.core.findquerygenerator.Filter
import ru.yandex.market.wms.transportation.core.model.response.GetTransportOrdersResponseWithCursor
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent
import java.util.Optional

class PreConsolidationControllerTest2 : IntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var transportationClient: TransportationClient;

    @Autowired
    private lateinit var mapper: ObjectMapper;

    @BeforeEach
    fun reset() {
        Mockito.reset(transportationClient)
    }

    @Test
    @Disabled
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerFreeLineTest() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT123")
    ) { status().is2xxSuccessful }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerContainerIsNotFromBufferTest() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT124")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerContainerIsEmptyTest() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT125")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerContainerBalanceExceptionTest() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT126")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerLineWithAnotherContainerTest() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT127")
    ) { status().is2xxSuccessful }

    @Test
    @DatabaseSetup("/precons/pre-configuration-controller-id-line-is-empty.xml")
    fun getLineForContainerFindStationWithContainerIDLineIsEmptyTest() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT129")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerFindStationWithContainerIDLineIsNotEmptyTest() {
        val content = TransportOrderResourceContent.builder().status(TransportOrderStatus.FINISHED).build();
        val response =
            GetTransportOrdersResponseWithCursor(
                null,
                listOf<TransportOrderResourceContent>(content)
            );
        Mockito.`when`(
            transportationClient
                .getTransportOrders(
                    Mockito.anyInt(), Mockito.anyString(), Mockito.any(Filter::class.java),
                    Mockito.eq(Optional.empty()), Mockito.eq(Optional.empty())
                )
        )
            .thenReturn(response);
        commonGetLineForContainerTest(containerDtoJsonBuilder("PLT129")) { status().is2xxSuccessful };
    }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller-partial-wave.xml")
    fun getLineForContainerFindStationWithContainerIDLineIsNotEmptyTest_partialWaveSorting() {
        val content = TransportOrderResourceContent.builder().status(TransportOrderStatus.FINISHED).build();
        val response =
            GetTransportOrdersResponseWithCursor(
                null,
                listOf<TransportOrderResourceContent>(content)
            );
        Mockito.`when`(
            transportationClient
                .getTransportOrders(
                    Mockito.anyInt(), Mockito.anyString(), Mockito.any(Filter::class.java),
                    Mockito.eq(Optional.empty()), Mockito.eq(Optional.empty())
                )
        )
            .thenReturn(response)
        commonGetLineForContainerTest(containerDtoJsonBuilder("PLT129")) { status().is4xxClientError };
    }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerFindStationWithContainerIDAllLinesHaveActiveOrders() {
        val content = TransportOrderResourceContent.builder().status(TransportOrderStatus.IN_PROGRESS).build();
        val response =
            GetTransportOrdersResponseWithCursor(
                null,
                listOf<TransportOrderResourceContent>(content)
            );
        Mockito.`when`(
            transportationClient
                .getTransportOrders(
                    Mockito.anyInt(), Mockito.anyString(), Mockito.any(Filter::class.java),
                    Mockito.eq(Optional.empty()), Mockito.eq(Optional.empty())
                )
        )
            .thenReturn(response);
        commonGetLineForContainerTest(containerDtoJsonBuilder("PLT129")) { status().is4xxClientError };
    }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun getLineForContainerWaveAlreadyHasLine() = commonGetLineForContainerTest(
        containerDtoJsonBuilder("PLT130")
    ) { status().is2xxSuccessful }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun moveContainerToLineLineNotEqualsWithSelectedTest() = commonMoveContainerToLineTest(
        containerDtoJsonBuilder("PLT123", "STAGE01", "STAGE02")
    ) {
        status().is4xxClientError
        content().json("""{"message":"404 NOT_FOUND \"Line is not equals with selected STAGE01\""}""")
    }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller-already-moved-serial.xml")
    fun moveContainerToLineDoubleRequest() {
        commonMoveContainerToLineTest(
            containerDtoJsonBuilder("PLT123", "STAGE01", "STAGE01")
        ) { status().is5xxServerError
            content().json("""{"message":"Какие-то из товаров уже перемещены"}""")
        }
    }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun moveContainerToLineSelectedLineIsNullTest() = commonMoveContainerToLineTest(
        containerDtoJsonBuilder(containerId = "PLT123", selectedLine = null, destinationLine = "STAGE01")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun moveContainerToLineDestLineIsNullTest() = commonMoveContainerToLineTest(
        containerDtoJsonBuilder(containerId = "PLT123", destinationLine = null, selectedLine = "STAGE01")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun moveContainerToNonsortLineDestLineIsNull() = commonMoveContainerToLineTest(
        containerDtoJsonBuilder(containerId = "PLT123", destinationLine = null, selectedLine = "NSCONS01")
    ) {
        status().is4xxClientError
        content().json("""{"message":"Контейнер PLT123 должен сортироваться, его нельзя разместить в нонсорт-линию NSCONS01"}""")
    }

    @Test
    @DatabaseSetup("/precons/pre-consolidation-controller.xml")
    fun moveContainerToLineBothIsNullTest() = commonMoveContainerToLineTest(
        containerDtoJsonBuilder(containerId = "PLT123")
    ) { status().is4xxClientError }

    @Test
    @DatabaseSetup("/containers-in-wave/before.xml")
    fun getContainersInWaveById() = commonGetContainersInWaveTest(
        ContainersInWaveRequest(ContainersInWaveRequest.Type.CONTAINER_ID, "CONTAINER-1").toJson()
    ) {
        status().is2xxSuccessful
        content().json(FileContentUtils.getFileContent("containers-in-wave/response.json"))
    }

    @Test
    @DatabaseSetup("/containers-in-wave/before.xml")
    fun getContainersInWaveByWavekey() = commonGetContainersInWaveTest(
        ContainersInWaveRequest(ContainersInWaveRequest.Type.WAVE_KEY, "0000233783").toJson()
    ) {
        status().is2xxSuccessful
        content().json(FileContentUtils.getFileContent("containers-in-wave/response.json"))
    }

    private fun Any.toJson(): String = mapper.writeValueAsString(this)

    private fun containerDtoJsonBuilder(
        containerId: String,
        selectedLine: String? = null,
        destinationLine: String? = null
    ) = ContainerDTO(containerId = containerId, selectedLine = selectedLine, destinationLine = destinationLine).toJson()

    private fun commonGetLineForContainerTest(json: String, expectedResult: () -> ResultMatcher) =
        commonQuery(json, "/precons/line-for-container", expectedResult) { url: String -> post(url) }

    private fun commonMoveContainerToLineTest(json: String, expectedResult: () -> ResultMatcher) =
        commonQuery(json, "/precons/move-container-to-line", expectedResult) { url: String -> post(url) }

    private fun commonGetContainersInWaveTest(json: String, expectedResult: () -> ResultMatcher) =
        commonQuery(json, "/precons/containers-in-wave", expectedResult) { url: String -> post(url) }

    private fun commonQuery(
        body: String,
        url: String,
        expectedResult: () -> ResultMatcher,
        buildersSupplier: (String) -> MockHttpServletRequestBuilder
    ) {
        mockMvc.perform(
            buildersSupplier(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(expectedResult())
            .andReturn()
    }
}
