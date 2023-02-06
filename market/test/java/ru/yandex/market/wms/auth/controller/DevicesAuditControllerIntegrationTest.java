package ru.yandex.market.wms.auth.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;

class DevicesAuditControllerIntegrationTest extends AuthIntegrationTest {

    /**
     * Получение сессий по фильтру, содержащиму Дату события
     */
    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getSingleSessionForParamsWithEventDateTimeHappyPath() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/devices?filter=eventDateTime=='2020-03-22 08:00:00'&sort=eventDateTime"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/response-main-session.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getSessionsForParamsWithEventDateTimeHappyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices?filter=eventDateTime>'2020-03-03 00:00:00'"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/response-main-session.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getNoSessionsForParamsWithEventDateTime() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices?filter=eventDateTime<'2020-01-01 00:00:00'"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/empty-response.json")))
                .andReturn();
    }

    /**
     * Получение сессий по фильтру, содержащиму Пользователя
     */
    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getSessionsForParamsWithUsernameHappyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices?filter=username=='AD2'&sort=username"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/response-main-session.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getNoSessionsForParamsWithUsername() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices?filter=username=='AD333'"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/empty-response.json")))
                .andReturn();
    }

    /**
     * Получение сессий по фильтру, содержащиму Идентификатор устройства
     */
    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getSessionsForParamsWithDeviceIdHappyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices?filter=deviceId=='123456789'&sort=deviceId"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/response-main-session.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getNoSessionsForParamsWithDeviceId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices?filter=deviceId=='222'"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/empty-response.json")))
                .andReturn();
    }

    /**
     * Получение сессий с пустыми фильтрами
     */
    @Test
    @DatabaseSetup("/db/controller/device-audit/setup.xml")
    public void getSessionsForParamsWithFakeHappyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/devices"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(
                        FileContentUtils.getFileContent("controller/device-audit/response-some-sessions.json")))
                .andReturn();
    }
}
