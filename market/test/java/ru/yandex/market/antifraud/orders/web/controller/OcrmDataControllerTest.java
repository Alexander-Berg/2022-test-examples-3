package ru.yandex.market.antifraud.orders.web.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudItemLimitRule;
import ru.yandex.market.antifraud.orders.service.BuyerDataService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.ItemLimitRulesService;
import ru.yandex.market.antifraud.orders.service.PersonalRestrictionsService;
import ru.yandex.market.antifraud.orders.service.RoleService;
import ru.yandex.market.antifraud.orders.service.UnglueService;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdType;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.AntifraudNodeType;
import ru.yandex.market.antifraud.orders.web.dto.ItemLimitRulesRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsPojo;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.PersonalRestrictionsUidsPojo;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlacklistType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEvent;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEventGroup;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerBlockBatchRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerModifyRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.RefundPolicy;
import ru.yandex.market.antifraud.orders.web.dto.crm.RuleTriggerEvent;
import ru.yandex.market.antifraud.orders.web.dto.crm.UnglueEdgeRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.UnglueNodeRequestDto;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.antifraud.orders.service.UserMarkerResolver.BAD_ACC_MARKER;
import static ru.yandex.market.antifraud.orders.service.UserMarkerResolver.RESELLER_MARKER;

/**
 * @author dzvyagin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(OcrmDataController.class)
public class OcrmDataControllerTest {

    private static final BuyerModifyRequestDto crmRequest = BuyerModifyRequestDto.builder()
            .customerUid(234)
            .authorUid(123)
            .authorLogin("zzz")
            .reason("OCRM_567")
            .build();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuyerDataService buyerDataService;

    @MockBean
    private UnglueService unglueService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private ItemLimitRulesService itemLimitRulesService;

    @MockBean
    private ConfigurationService configurationService;

    @MockBean
    private PersonalRestrictionsService personalRestrictionsService;

    @Before
    public void init() {
        when(configurationService.ocrmEndpointsEnabled()).thenReturn(true);
    }

    @Test
    public void getInfo() throws Exception {
        BuyerInfoDto info = BuyerInfoDto.builder()
                .uid(123L)
                .blacklist(false)
                .blacklistType(BlacklistType.NOT_BLACKLISTED)
                .vip(false)
                .roleName("default")
                .refundPolicy(RefundPolicy.SIMPLE)
                .roleDescription("default role")
                .userMarkers(new HashSet<>(Arrays.asList(
                        BAD_ACC_MARKER,
                        RESELLER_MARKER
                )))
                .build();
        when(buyerDataService.getBuyerInfo(anyLong())).thenReturn(completedFuture(info));
        String infoResponseJson =
                "{\"uid\":123,\"roleName\":\"default\",\"roleDescription\":\"default role\",\"vip\":false," +
                        "\"blacklist\":false,\"blacklistType\":\"NOT_BLACKLISTED\",\"refundPolicy\":\"SIMPLE\"," +
                        "\"userMarkers\":[{\"name\":\"reseller\",\"showName\":\"Реселлер\"," +
                        "\"description\":\"Реселллер\",\"type\":\"BAD\"},{\"name\":\"bad_acc\"," +
                        "\"showName\":\"Подозрительный аккаунт\",\"description\":\"Массово созданный аккаунт\"," +
                        "\"type\":\"BAD\"}]}\n";
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(get("/crm/buyer/info")
                        .param("uid", "123")
                        .accept(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json(infoResponseJson));
    }

    @Test
    public void getBlockings() throws Exception {
        Instant ts1 = LocalDate.of(2019, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant ts2 = LocalDate.of(2019, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC);
        when(buyerDataService.getEventsForUser(anyLong(), any()))
                .thenReturn(List.of(
                        new BlockingEvent(123L, ts1, BlockingType.LOYALTY, List.of(
                                new RuleTriggerEvent("r1", "d1"),
                                new RuleTriggerEvent("r2", "d2")
                        )),
                        new BlockingEvent(123L, ts2, BlockingType.LOYALTY, List.of(
                                new RuleTriggerEvent("r1", "d1"),
                                new RuleTriggerEvent("r2", "d2")
                        ))
                ));
        String infoResponseJson =
                "{\"uid\":123,\"page\":0,\"pageSize\":10,\"blockingType\":\"LOYALTY\",\"blockings\":[{\"uid\":123," +
                        "\"timestamp\":\"2019-11-02T00:00:00Z\",\"type\":\"LOYALTY\"," +
                        "\"ruleTriggerEvents\":[{\"ruleName\":\"r1\",\"description\":\"d1\"},{\"ruleName\":\"r2\"," +
                        "\"description\":\"d2\"}]},{\"uid\":123,\"timestamp\":\"2019-11-01T00:00:00Z\"," +
                        "\"type\":\"LOYALTY\",\"ruleTriggerEvents\":[{\"ruleName\":\"r1\",\"description\":\"d1\"}," +
                        "{\"ruleName\":\"r2\",\"description\":\"d2\"}]}]}\n";
        mockMvc.perform(
                get("/crm/buyer/blockings")
                        .param("uid", "123")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("type", "LOYALTY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(infoResponseJson));
    }


    @Test
    public void getBlockingGroups() throws Exception {
        Instant ts1 = LocalDate.of(2019, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant ts2 = LocalDate.of(2019, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC)
                .plus(1L, ChronoUnit.HOURS);
        when(buyerDataService.getEventGroupsForUser(anyLong(), any(), anyLong(), anyLong()))
                .thenReturn(List.of(
                        BlockingEventGroup.builder()
                                .uid(123L)
                                .from(ts1)
                                .to(ts1)
                                .count(1)
                                .restrictions(Set.of(AntifraudAction.CANCEL_ORDER))
                                .blockings(List.of(new BlockingEvent(123L, ts1, BlockingType.ORDER, List.of(
                                        new RuleTriggerEvent("r1", "d1"),
                                        new RuleTriggerEvent("r2", "d2")
                                ))))
                                .build(),
                        BlockingEventGroup.builder()
                                .uid(123L)
                                .from(ts2)
                                .to(ts2)
                                .count(1)
                                .restrictions(Set.of(AntifraudAction.CANCEL_ORDER))
                                .blockings(List.of(new BlockingEvent(123L, ts2, BlockingType.ORDER, List.of(
                                        new RuleTriggerEvent("r1", "d1"),
                                        new RuleTriggerEvent("r2", "d2")
                                )))).build()
                ));
        String infoResponseJson =
                "{\"uid\":123,\"total\":2,\"page\":0,\"pageSize\":10,\"blockingType\":\"ORDER\"," +
                        "\"blockingGroups\":[{\"uid\":123,\"from\":\"2019-11-02T01:00:00Z\"," +
                        "\"to\":\"2019-11-02T01:00:00Z\",\"count\":1,\"restrictions\":[\"CANCEL_ORDER\"]," +
                        "\"blockings\":[{\"uid\":123,\"timestamp\":\"2019-11-02T01:00:00Z\",\"type\":\"ORDER\"," +
                        "\"ruleTriggerEvents\":[{\"ruleName\":\"r1\",\"description\":\"d1\"},{\"ruleName\":\"r2\"," +
                        "\"description\":\"d2\"}]}]},{\"uid\":123,\"from\":\"2019-11-02T00:00:00Z\"," +
                        "\"to\":\"2019-11-02T00:00:00Z\",\"count\":1,\"restrictions\":[\"CANCEL_ORDER\"]," +
                        "\"blockings\":[{\"uid\":123,\"timestamp\":\"2019-11-02T00:00:00Z\",\"type\":\"ORDER\"," +
                        "\"ruleTriggerEvents\":[{\"ruleName\":\"r1\",\"description\":\"d1\"},{\"ruleName\":\"r2\"," +
                        "\"description\":\"d2\"}]}]}]}\n";
        mockMvc.perform(
                get("/crm/buyer/blockings/grouped")
                        .param("uid", "123")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("type", "ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(infoResponseJson));
    }

    @Test
    public void blockCustomer() throws Exception {
        when(buyerDataService.postBlacklistRule(234, 123, "OCRM_567"))
                .thenReturn(completedFuture(null));
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        post("/crm/buyer/blockings")
                                .content(AntifraudJsonUtil.toJson(crmRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().isOk());
    }

    @Test
    public void blockCustomers() throws Exception {
        var request = BuyerBlockBatchRequestDto.builder()
                .customerUids(List.of(1L, 2L))
                .authorUid(1001L)
                .authorLogin("author")
                .reason("AFM-1")
                .build();
        when(buyerDataService.postBlacklistRules(request)).thenReturn(completedFuture(null));
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        post("/crm/buyer/blockings/many")
                                .content(AntifraudJsonUtil.toJson(request))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().isOk());
    }

    @Test
    public void unblockCustomer() throws Exception {
        when(buyerDataService.clearBuyerBlacklist(234))
                .thenReturn(completedFuture(null));
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        delete("/crm/buyer/blockings")
                                .content(AntifraudJsonUtil.toJson(crmRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().isOk());
    }

    @Test
    public void whitelistCustomer() throws Exception {
        mockMvc.perform(
                put("/crm/buyer/whitelist")
                        .content(AntifraudJsonUtil.toJson(crmRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(roleService).addUserToRole("234", BuyerIdType.UID, "OCRM_567", "whitelist");
    }

    @Test
    public void removeFromWhiteList() throws Exception {
        mockMvc.perform(
                delete("/crm/buyer/whitelist")
                        .content(AntifraudJsonUtil.toJson(crmRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(roleService).removeUserFromRole("234", BuyerIdType.UID, "whitelist");
    }

    @Test
    public void unglueNode() throws Exception {
        when(unglueService.addNode(argThat(dto -> dto.getDescription().equals("OCRM_567"))))
                .thenReturn(completedFuture(null));
        var request = UnglueNodeRequestDto.builder()
            .nodeType(AntifraudNodeType.card)
                .nodeValue("card")
                .authorUid(123)
                .authorLogin("zzz")
                .reason("OCRM_567")
                .build();
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        post("/crm/unglue/node")
                                .content(AntifraudJsonUtil.toJson(request))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().isOk());
    }

    @Test
    public void unglueEdge() throws Exception {
        when(unglueService.addEdge(any()))
            .thenReturn(completedFuture(null));
        var request = UnglueEdgeRequestDto.builder()
            .node1Type(AntifraudNodeType.card)
            .node1Value("card")
            .node2Type(AntifraudNodeType.yandexuid)
                .node2Value("arcadiy")
                .authorUid(123)
                .authorLogin("zzz")
                .reason("OCRM_567")
                .build();
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        post("/crm/unglue/edge")
                                .content(AntifraudJsonUtil.toJson(request))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andReturn()))
                .andExpect(status().isOk());
        verify(unglueService)
            .addEdge(argThat(dto -> dto.getDescription().equals("OCRM_567")
                && dto.getNode1Value().equals("card")
                && dto.getNode2Value().equals("arcadiy")));
    }

    @Test
    public void addItemLimitRules() throws Exception {
        var request = ItemLimitRulesRequestDto.builder()
            .rules(List.of(
                AntifraudItemLimitRule.builder()
                    .msku(43L)
                    .maxCountPerOrder(1000L)
                    .build(),
                AntifraudItemLimitRule.builder()
                    .modelId(654L)
                    .maxCountPerUser(2000L)
                    .build()
            ))
            .authorUid(123)
            .authorLogin("zzz")
            .reason("OCRM_567")
            .periodFrom(LocalDate.of(2022, 6, 1))
            .periodTo(LocalDate.of(2022, 6, 2))
            .build();
        mockMvc.perform(
                post("/crm/item-limit-rules")
                        .content(AntifraudJsonUtil.toJson(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(itemLimitRulesService).saveValidatedRules(argThat(
            c -> c.getResult().stream()
                .allMatch(r -> r.getTag().equals("OCRM_567") &&
                    r.getPeriodFrom().equals(LocalDate.of(2022, 6, 1)) &&
                    r.getPeriodTo().equals(LocalDate.of(2022, 6, 2)))
        ));
    }

    @Test
    public void addPersonalRestrictions() throws Exception {
        var request = PersonalRestrictionsRequestDto.builder()
                .restrictions(List.of(
                        PersonalRestrictionsPojo.builder()
                                .uid("1234")
                                .actions(new AntifraudAction[]{AntifraudAction.PREPAID_ONLY})
                                .build(),
                        PersonalRestrictionsPojo.builder()
                                .uid("5678")
                                .actions(new AntifraudAction[]{AntifraudAction.ROBOCALL})
                                .build()
                ))
                .authorUid(123)
                .authorLogin("zzz")
                .reason("OCRM_567")
                .build();
        mockMvc.perform(
                put("/crm/personal-restrictions")
                        .content(AntifraudJsonUtil.toJson(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(personalRestrictionsService).saveRestrictions(argThat(
                c -> c.getRestrictions().size() == 2
        ));
    }

    @Test
    public void addPersonalRestrictionsManyUids() throws Exception {
        var request = PersonalRestrictionsRequestDto.builder()
                .restrictions(List.of(
                        PersonalRestrictionsUidsPojo.builder()
                                .uids(List.of(1L, 2L))
                                .action(AntifraudAction.PREPAID_ONLY)
                                .build(),
                        PersonalRestrictionsUidsPojo.builder()
                                .uids(List.of(3L, 4L))
                                .action(AntifraudAction.ROBOCALL)
                                .build()
                ))
                .authorUid(123)
                .authorLogin("zzz")
                .reason("OCRM_567")
                .build();
        mockMvc.perform(
                        put("/crm/personal-restrictions-uids")
                                .content(AntifraudJsonUtil.toJson(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(personalRestrictionsService).saveRestrictionsForManyUids(argThat(
                c -> c.getRestrictions().size() == 2
        ));
    }

    @Test
    public void deletePersonalRestrictions() throws Exception {
        var request = PersonalRestrictionsRequestDto.builder()
                .restrictions(List.of(
                        PersonalRestrictionsPojo.builder()
                                .uid("1")
                                .actions(new AntifraudAction[]{AntifraudAction.PREPAID_ONLY})
                                .build()
                ))
                .authorUid(123)
                .authorLogin("zzz")
                .reason("OCRM_567")
                .build();
        mockMvc.perform(
                        delete("/crm/personal-restrictions")
                                .content(AntifraudJsonUtil.toJson(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(personalRestrictionsService).deleteRestrictions(argThat(
                c -> c.getRestrictions().size() == 1
        ));
    }
}
