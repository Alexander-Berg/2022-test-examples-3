package ru.yandex.market.antifraud.orders.web.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.Participant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.service.ClusterStateService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ClusterNodeEvent;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.NodeEventType;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil.toJsonTree;

/**
 * @author dzvyagin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(ConfigurationController.class)
public class ConfigurationControllerTest {

    @MockBean
    private ConfigurationService configurationService;
    @MockBean
    private LeaderSelector leaderSelector;
    @MockBean
    private ClusterStateService clusterStateService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLeader() throws Exception {
        when(leaderSelector.getLeader()).thenReturn(
                new Participant("leader", true)
        );
        Instant ts = Instant.from(LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.of("UTC+3")));
        when(clusterStateService.lastEvents(anyInt())).thenReturn(List.of(
                ClusterNodeEvent.builder().id(1L).ip("ip").node("node").eventType(NodeEventType.TAKE_LEADERSHIP).addedAt(ts).build()
        ));
        String response = "{\"participant\":{\"id\":\"leader\",\"leader\":true}," +
                "\"lastEvents\":[{\"id\":1,\"node\":\"node\",\"ip\":\"ip\",\"eventType\":\"TAKE_LEADERSHIP\",\"addedAt\":\"2019-12-31T21:00:00Z\"}]}";
        mockMvc.perform(get("/configuration/leader"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void testSaveConfig() throws Exception {
        when(configurationService.save(any(ConfigurationEntity.class))).thenReturn(
                new ConfigurationEntity(1L, ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER, null, toJsonTree(false))
        );
        String request = "{\"parameter\": \"ANTIFRAUD_OFFLINE_BAN_USER\", \"config\": true}";
        String response = "{\"id\":1,\"parameter\":\"ANTIFRAUD_OFFLINE_BAN_USER\",\"config\":false}";
        mockMvc.perform(post("/configuration/set")
                .contentType(APPLICATION_JSON_UTF8)
                .content(request))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void testGetConfig() throws Exception {
        when(configurationService.getEntity(any(ConfigEnum.class), anyBoolean())).thenReturn(
                new ConfigurationEntity(1L, ConfigEnum.ANTIFRAUD_OFFLINE_BAN_USER, null, toJsonTree(false))
        );
        String response = "{\"id\":1,\"parameter\":\"ANTIFRAUD_OFFLINE_BAN_USER\",\"config\":false}";
        mockMvc.perform(
                get("/configuration/find?config=ANTIFRAUD_OFFLINE_BAN_USER&cached=1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }
}
