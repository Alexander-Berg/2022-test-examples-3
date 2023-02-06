package ru.yandex.market.antifraud.orders.web.controller;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.BuyerDataService;
import ru.yandex.market.antifraud.orders.service.GluesService;
import ru.yandex.market.antifraud.orders.service.ItemLimitRulesService;
import ru.yandex.market.antifraud.orders.service.RoleService;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyAntifraudService;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdRole;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdType;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.PHONE;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.UID;

/**
 * @author dzvyagin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(MetadataController.class)
public class MetadataControllerTest {

    private static final String CANCEL_ORDER_ACTION = Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlacklistService blackListService;
    @MockBean
    private ItemLimitRulesService itemLimitRulesService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private BuyerDataService buyerDataService;
    @MockBean
    private GluesService gluesService;

    @MockBean
    private LoyaltyAntifraudService loyaltyAntifraudService;

    @Test
    public void getBlacklistRules() throws Exception {
        List<AntifraudBlacklistRule> blacklistRules = getTestBlacklistRules();
        when(blackListService.getBlacklistRules(anyList(), isNull(), eq(CANCEL_ORDER_ACTION), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(blacklistRules);
        List<String> expectedStrings = getTestBlackListRulesAsJsonStrings();
        String response = mockMvc.perform(get("/metadata/blacklist")).andReturn().getResponse().getContentAsString();
        System.err.println(response);
        Assert.assertTrue(expectedStrings.stream().allMatch(response::contains));
    }

    @Test
    public void getBlacklistRulesPage() throws Exception {
        List<AntifraudBlacklistRule> blacklistRules = getTestBlacklistRules();
        List<String> expectedStrings = getTestBlackListRulesAsJsonStrings();
        when(blackListService.getBlacklistRules(
                        eq(List.of(PHONE, UID)), eq("value"), eq(CANCEL_ORDER_ACTION), eq("reason"), eq(-1L), eq(1), eq(1)))
                .thenReturn(Collections.singletonList(blacklistRules.iterator().next()));
        String url = "/metadata/blacklist?page=1&size=1&value=value&reason=reason&uid=-1&type=PHONE&type=UID";
        String response = mockMvc.perform(get(url)).andReturn().getResponse().getContentAsString();
        Assert.assertEquals("[" + expectedStrings.iterator().next() + "]", response);
    }

    @Test
    public void postBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                CANCEL_ORDER_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blackListService.saveBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                post("/metadata/blacklist")
                        .content(newRule)
                        .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void removeBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String newRule =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        String response =
                "{\"type\":\"EMAIL\",\"value\":\"test@test.com\",\"reason\":\"some_reason_3\"," +
                        "\"expiryAt\":\"30-05-2050 00:00:00\"}";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                AntifraudBlacklistRuleType.EMAIL,
                "test@test.com",
                CANCEL_ORDER_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blackListService.removeBlacklistRule(any())).thenReturn(rule);
        mockMvc.perform(
                delete("/metadata/blacklist")
                        .content(newRule)
                        .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void removeBlacklistRuleBatch() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        String response = "[{\"type\":\"UID\",\"value\":\"1\",\"reason\":\"some_reason_3\"," +
                "\"expiryAt\":\"30-05-2050 00:00:00\"}, {\"type\":\"UID\",\"value\":\"1\"," +
                "\"reason\":\"some_reason_3\",\"expiryAt\":\"30-05-2050 00:00:00\"}]";
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(
                UID,
                "1",
                CANCEL_ORDER_ACTION,
                "some_reason_3",
                ft.parse("30-05-2050 00:00:00 +0300"),
                555555555L
        );
        when(blackListService.removeBlacklistRule(any())).thenReturn(rule);
        when(blackListService.getRules(any(), any(), anyString())).thenReturn(List.of(rule, rule));

        mockMvc.perform(
                delete("/metadata/blacklist/batch?uids=1,2")
                        .header("Content-Type", "application/json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void postItemLimitRulesOk() throws Exception {
        String rules = "[{\"tag\":\"MARKETCHECKOUT-7719\",\"categoryId\":13360751,\"maxCountPerUser\":60," +
                "\"supplierId\":465852,\"historyPeriod\":1},\n" +
                "{\"tag\":\"DiS_first\",\"msku\":102669011,\"maxCountPerOrder\":2,\"supplierId\":465852}]";

        String response = "Parsed 2 rules, saving them";
        when(itemLimitRulesService.saveValidatedRules(any())).thenReturn(response);

        mockMvc.perform(
                post("/metadata/item-limit-rules")
                        .content(rules)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(response));

    }

    @Test
    public void postItemLimitRulesWithZeroes() throws Exception {
        String rules = "[{\"tag\":\"MARKETCHECKOUT-7719\",\"categoryId\":13360751,\"maxCountPerUser\":0," +
                "\"supplierId\":465852,\"historyPeriod\":1},\n" +
                "{\"tag\":\"DiS_first\",\"msku\":102669011,\"maxCountPerOrder\":2,\"supplierId\":465852}]";

        String responsePattern = "Parsed 1 rules, saving them";
        when(itemLimitRulesService.saveValidatedRules(any())).thenReturn(responsePattern);

        mockMvc.perform(
                post("/metadata/item-limit-rules")
                        .content(rules)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(Matchers.matchesPattern(responsePattern)));

    }

    @Test
    public void getRoles() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of(BuyerRole.builder()
                .id(1L)
                .name("test_role")
                .description("test")
                .detectorConfigurations(Map.of())
                .build()));
        String response = "[{\"id\":1,\"name\":\"test_role\",\"description\":\"test\",\"vip\":false,\"buyerType\":\"B2C\",\"detectorConfigurations\":{}}]";
        mockMvc.perform(
                get("/metadata/roles"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(response));
    }

    @Test
    public void getUsersByRole() throws Exception {
        when(roleService.getUsersByRole(eq("test_role"))).thenReturn(List.of(BuyerIdRole.builder()
                .id(1L)
                .buyerId("123")
                .buyerIdType(BuyerIdType.UID)
                .roleId(2L)
                .build()));
        String response = "[{\"id\":1,\"buyerId\":\"123\",\"buyerIdType\":\"UID\",\"roleId\":2}]";
        mockMvc.perform(
                get("/metadata/roles/test_role/users"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(response));
    }

    @Test
    public void getGraph() throws Exception {
        Node n1 = new Node("123", IdType.PUID);
        Node n2 = new Node("y123", IdType.YANDEXUID);
        Node n3 = new Node("234", IdType.PUID);
        when(gluesService.getEdges(any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        Map.of(
                                n1, Set.of(n2),
                                n2, Set.of(n1, n3),
                                n3, Set.of(n2)
                        )
                ));
        when(blackListService.getRules(any(), anySet(), anyString()))
                .thenReturn(List.of(new AntifraudBlacklistRule(UID, "123", CANCEL_ORDER_ACTION, "test", new Date(100L), 1L)));
        String response = "{\"adjacencyList\":[{\"node\":{\"id\":\"234\",\"idType\":\"PUID\"},\"adjacentNodes\":[{\"id\":\"y123\",\"idType\":\"YANDEXUID\"}]},{\"node\":{\"id\":\"y123\",\"idType\":\"YANDEXUID\"},\"adjacentNodes\":[{\"id\":\"123\",\"idType\":\"PUID\"},{\"id\":\"234\",\"idType\":\"PUID\"}]},{\"node\":{\"id\":\"123\",\"idType\":\"PUID\"},\"adjacentNodes\":[{\"id\":\"y123\",\"idType\":\"YANDEXUID\"}]}],\"blacklistedPuids\":[123]}";
        mockMvc.perform(
                get("/metadata/graph?id=123&idType=PUID&depth=3")
                        .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    public List<AntifraudBlacklistRule> getTestBlacklistRules() throws ParseException {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        return Arrays.asList(
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.UID, "123", CANCEL_ORDER_ACTION, "some_reason_1",
                        ft.parse("30-05-2045 00:00:00 +0300"), 555555555L),
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.PHONE, "+7111111111", CANCEL_ORDER_ACTION, "some_reason_2",
                        ft.parse("30-05-2049 00:00:00 +0300"), 555555555L)
        );
    }

    public List<String> getTestBlackListRulesAsJsonStrings() {
        return Arrays.asList(
                "{\"type\":\"UID\",\"value\":\"123\",\"action\":\"AntifraudAction_CANCEL_ORDER\",\"reason\":\"some_reason_1\"," +
                        "\"expiryAt\":\"30-05-2045 00:00:00\",\"authorUid\":555555555}",
                "{\"type\":\"PHONE\",\"value\":\"+7111111111\",\"action\":\"AntifraudAction_CANCEL_ORDER\",\"reason\":\"some_reason_2\"," +
                        "\"expiryAt\":\"30-05-2049 00:00:00\",\"authorUid\":555555555}"
        );
    }
}
