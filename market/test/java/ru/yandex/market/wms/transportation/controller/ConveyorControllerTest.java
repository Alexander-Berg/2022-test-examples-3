package ru.yandex.market.wms.transportation.controller;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ConveyorControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/before.xml")
    void containerToInfoLastRecent() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response01.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_01"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/before.xml")
    void containerToInfoEmptyError() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response02.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_02"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/before.xml")
    void containerToInfoEmptyContainer() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response03.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_04"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/before.xml")
    void containerToInfoNotFound() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response04.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_03"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/only-old-orders-before.xml")
    void containerToInfoNotFoundOldOrders() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response05.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_01"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/before.xml")
    @DatabaseSetup("/controller/conveyor/container-info/before-with-measure-container.xml")
    void containerToInfoMeasurementToMeasureStation() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response06.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_01")
                        .param("loc", "IN_NOK-01"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/container-info/before.xml")
    @DatabaseSetup("/controller/conveyor/container-info/before-with-measure-container.xml")
    void containerToInfoMeasurementRecreateTO() throws Exception {
        assertApiGetCallOk("controller/conveyor/container-info/response07.json",
                get("/conveyor/container-info").param("containerId", "CONTAINER_01")
                        .param("loc", "MB-NOK-02"));
    }

    private void assertApiGetCallOk(String responseFile, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST")))
                .andExpect(status().isOk())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                mvcResult.getResponse().getContentAsString());
    }

    private void assertApiGetCallError(MockHttpServletRequestBuilder request,
                                       HttpStatus status,
                                       String errorDescription) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST")))
                .andExpect(status().is(status.value()))
                .andReturn();


        if (errorDescription != null) {
            assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .contains(errorDescription);
        }
    }
}
