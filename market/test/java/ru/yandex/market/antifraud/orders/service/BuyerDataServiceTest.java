package ru.yandex.market.antifraud.orders.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.detector.AntifraudBlacklistDetector;
import ru.yandex.market.antifraud.orders.detector.AntifraudBlacklistGluedDetector;
import ru.yandex.market.antifraud.orders.detector.PgItemLimitRuleDetector;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.UserMarkers;
import ru.yandex.market.antifraud.orders.external.crm.HttpLiluCrmClient;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlacklistType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEvent;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEventGroup;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.RuleTriggerEvent;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.AntifraudBlockingEvent;
import ru.yandex.market.crm.platform.profiles.Facts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class BuyerDataServiceTest {

    private static final String CANCEL_ORDER_ACTION = Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER);

    @Mock
    private HttpLiluCrmClient httpLiluCrmClient;
    @Mock
    private RoleService roleService;
    @Mock
    private AntifraudDao antifraudDao;
    @Mock
    private MarketUserIdDao marketUserIdDao;
    @Mock
    private GluesService gluesService;
    @Mock
    private BlacklistService blacklistService;

    private BuyerDataService buyerDataService;

    @Before
    public void init() {
        buyerDataService = new BuyerDataService(httpLiluCrmClient,
                roleService,
                antifraudDao,
                marketUserIdDao,
                List.of(new AntifraudBlacklistDetector(blacklistService),
                        new PgItemLimitRuleDetector(null),
                        new AntifraudBlacklistGluedDetector(null, null)),
                gluesService,
                new UserMarkerResolver(mock(ConfigurationService.class)),
                blacklistService);
    }

    @SneakyThrows
    @Test
    public void getBuyerInfo() {
        BuyerRole role = BuyerRole.builder()
                .name("test_role")
                .description("test_role_description")
                .detectorConfigurations(Collections.emptyMap())
                .build();
        when(roleService.getRoleByUidOrDefault(anyString())).thenReturn(role);
        when(roleService.getDefaultRole()).thenReturn(role);
        when(blacklistService.getAllRules(eq(AntifraudBlacklistRuleType.UID), anyCollection()))
            .thenReturn(List.of(
                AntifraudBlacklistRule.builder().action(CANCEL_ORDER_ACTION).build(),
                AntifraudBlacklistRule.builder().action(Utils.getBlacklistAction(LoyaltyVerdictType.BLACKLIST)).build()
            ));
        when(gluesService.getGluedIds(any()))
                .thenReturn(CompletableFuture.completedFuture(Set.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L),
                        new MarketUserId(null, "uid", "ds", null))));
        when(marketUserIdDao.getAllMarkersForPuid(anyLong()))
                .thenReturn(Optional.of(UserMarkers.builder()
                        .puid(123L)
                        .glueId(1L)
                        .markers(Set.of("bad_acc", "verdict_no_review"))
                        .build()));
        BuyerInfoDto infoDto = buyerDataService.getBuyerInfo(123L).get();
        assertThat(infoDto.getBlacklist()).isTrue();
        assertThat(infoDto.getBlacklistType()).isEqualTo(BlacklistType.GLUE);
        assertThat(infoDto.getVip()).isFalse();
        assertThat(infoDto.getUid()).isEqualTo(123L);
        assertThat(infoDto.getGluedUids()).containsExactly(123L, 124L);
        assertThat(infoDto.getRoleName()).isEqualTo("test_role");
        assertThat(infoDto.getUserMarkers()).containsExactlyInAnyOrder(
            UserMarkerResolver.BAD_ACC_MARKER,
            UserMarkerResolver.VERDICT_NO_REVIEW_MARKER,
            UserMarkerResolver.LOYALTY_BLACKLIST_MARKER
        );
        assertThat(infoDto.getRoleDescription()).isEqualTo("test_role_description");
    }

    @SneakyThrows
    @Test
    public void checkBlacklistedWhitelist() {
        BuyerRole role = BuyerRole.builder()
                .name("test_role")
                .description("test_role_description")
                .detectorConfigurations(Collections.emptyMap())
                .build();
        when(roleService.getRoleByUidOrDefault(anyString())).thenReturn(role);
        when(roleService.getDefaultRole()).thenReturn(BuyerRole.builder()
                .name("default_test_role")
                .description("test_role_description")
                .detectorConfigurations(Collections.emptyMap())
                .build());
        when(blacklistService.getAllRules(eq(AntifraudBlacklistRuleType.UID), anyCollection()))
                .thenReturn(List.of(AntifraudBlacklistRule.builder().action(CANCEL_ORDER_ACTION).build()));
        when(gluesService.getGluedIds(any()))
                .thenReturn(CompletableFuture.completedFuture(Set.of(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUid(124L),
                        new MarketUserId(null, "uid", "ds", null))));

        BuyerInfoDto infoDto = buyerDataService.getBuyerInfo(123L).get();
        assertThat(infoDto.getBlacklist()).isFalse();
    }


    @SneakyThrows
    @Test
    public void checkSearchUsesUid() {
        BuyerRole role = BuyerRole.builder()
                .name("test_role")
                .description("test_role_description")
                .detectorConfigurations(Collections.emptyMap())
                .build();
        when(roleService.getRoleByUidOrDefault(anyString())).thenReturn(role);
        when(roleService.getDefaultRole()).thenReturn(BuyerRole.builder()
                .name("default_test_role")
                .description("test_role_description")
                .detectorConfigurations(Collections.emptyMap())
                .build());
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        when(blacklistService.getAllRules(eq(AntifraudBlacklistRuleType.UID), captor.capture()))
                .thenReturn(List.of(AntifraudBlacklistRule.builder().action(CANCEL_ORDER_ACTION).build()));
        when(gluesService.getGluedIds(any()))
                .thenReturn(CompletableFuture.completedFuture(Set.of()));
        BuyerInfoDto infoDto = buyerDataService.getBuyerInfo(123L).get();
        Collection<String> collection = captor.getValue();
        assertThat(collection).contains("123");
    }

    @Test
    public void saveBlockingEvent() {
        BlockingEvent blockingEvent = new BlockingEvent(
                123L,
                Instant.now(),
                BlockingType.LOYALTY,
                List.of(new RuleTriggerEvent("r1", "d1"),
                        new RuleTriggerEvent("r2", "d2"))
        );
        buyerDataService.saveBlockingEvent(blockingEvent);
        ArgumentCaptor<AntifraudBlockingEvent> abeCaptor = ArgumentCaptor.forClass(AntifraudBlockingEvent.class);
        verify(httpLiluCrmClient).saveFact(anyString(), abeCaptor.capture());
        AntifraudBlockingEvent abe = abeCaptor.getValue();
        assertThat(abe.getBlockingType()).isEqualTo("LOYALTY");
        assertThat(abe.getTriggeredRulesCount()).isEqualTo(2);
        assertThat(abe.getKeyUid().getStringValue()).isEqualTo("123");
    }

    @Test
    public void getEventsForUser() {
        AntifraudBlockingEvent abe1 = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setStringValue("311870044").build())
                .setTimestamp(1575633900041L)
                .setBlockingType("ORDER")
                .build();
        AntifraudBlockingEvent abe2 = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setStringValue("311870044").build())
                .setTimestamp(1575636170807L)
                .setBlockingType("LOYALTY")
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("r1")
                        .setDescription("d1")
                        .build()
                )
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("r2")
                        .setDescription("d2")
                        .build()
                )
                .build();
        Facts facts = Facts.newBuilder()
                .addAntifraudBlockingEvent(abe1)
                .addAntifraudBlockingEvent(abe2)
                .build();

        when(httpLiluCrmClient.getFacts(eq("AntifraudBlockingEvent"), anyLong(), anyLong(), anyLong()))
                .thenReturn(facts);
        List<BlockingEvent> blockingEvents = buyerDataService.getEventsForUser(311870044L, BlockingType.LOYALTY);
        assertThat(blockingEvents).hasSize(1);
    }

    @Test
    public void getGroupedEventsForUser() {
        AntifraudBlockingEvent abe1 = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setStringValue("311870044").build())
                .setTimestamp(Instant.now().minus(3L, ChronoUnit.HOURS).toEpochMilli())
                .setBlockingType("ORDER")
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("ORDER_AntifraudBlacklistGluedDetector")
                        .setDescription("d2")
                        .build()
                )
                .build();
        AntifraudBlockingEvent abe2 = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setStringValue("311870044").build())
                .setTimestamp(Instant.now().minus(10L, ChronoUnit.MINUTES).toEpochMilli())
                .setBlockingType("ORDER")
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("ORDER_AntifraudBlacklistDetector")
                        .setDescription("d1")
                        .build()
                )
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("r2")
                        .setDescription("d2")
                        .build()
                )
                .build();
        AntifraudBlockingEvent abe3 = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder().setType(UidType.PUID).setStringValue("311870044").build())
                .setTimestamp(Instant.now().toEpochMilli())
                .setBlockingType("ORDER")
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("ORDER_PgItemLimitRuleDetector")
                        .setDescription("d1")
                        .build()
                )
                .addTriggeredRules(AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                        .setRuleName("r2")
                        .setDescription("d2")
                        .build()
                )
                .build();
        Facts facts = Facts.newBuilder()
                .addAntifraudBlockingEvent(abe1)
                .addAntifraudBlockingEvent(abe2)
                .addAntifraudBlockingEvent(abe3)
                .build();
        when(httpLiluCrmClient.getFacts(eq("AntifraudBlockingEvent"), anyLong(), anyLong(), anyLong()))
                .thenReturn(facts);
        List<BlockingEventGroup> groups = buyerDataService.getEventGroupsForUser(311870044L, BlockingType.ALL, -1L, -1L);

        assertThat(groups).hasSize(2);

        assertThat(groups.get(0).getCount()).isEqualTo(1);
        assertThat(groups.get(0).getRestrictions()).containsExactlyInAnyOrder(
                AntifraudAction.CANCEL_ORDER,
                AntifraudAction.ORDER_ITEM_CHANGE,
                AntifraudAction.PREPAID_ONLY,
                AntifraudAction.ROBOCALL,
                AntifraudAction.CANCEL_PROMO_CODE);

        assertThat(groups.get(1).getCount()).isEqualTo(2);
        assertThat(groups.get(1).getRestrictions()).containsExactlyInAnyOrder(
                AntifraudAction.CANCEL_ORDER,
                AntifraudAction.ORDER_ITEM_CHANGE,
                AntifraudAction.PREPAID_ONLY,
                AntifraudAction.ROBOCALL,
                AntifraudAction.CANCEL_PROMO_CODE);
    }

}
