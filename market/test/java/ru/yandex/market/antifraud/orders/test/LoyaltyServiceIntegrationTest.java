package ru.yandex.market.antifraud.orders.test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyCoin;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyPromo;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.storage.dao.RoleDao;
import ru.yandex.market.antifraud.orders.storage.dao.loyalty.LoyaltyDao;
import ru.yandex.market.antifraud.orders.storage.entity.antifraud.YtWalletTransaction;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.BaseDetectorConfiguration;
import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;
import ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestFiltersDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseItemDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dzvyagin
 */
@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class LoyaltyServiceIntegrationTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LoyaltyDao loyaltyDao;
    @Autowired
    private MarketUserIdDao userIdDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private YtClient ytClient;

    @Before
    public void init() {
        configurationService.save(ConfigurationEntity.builder()
            .config(AntifraudJsonUtil.toJsonTree(new String[0]))
            .parameter(ConfigEnum.FORCE_TVM_FOR_CLIENTS)
            .updatedAt(Instant.now())
            .build());
        configurationService.save(ConfigurationEntity.builder()
            .parameter(ConfigEnum.VOLVA_CHECK)
                .config(BooleanNode.FALSE)
                .build());
    }

    @Test
    public void testCoinRestriction() throws Exception {
        LoyaltyPromo promo = testPromo(2222L);
        loyaltyDao.savePromo(promo);
        userIdDao.save(MarketUserId.fromUid(8141L, 2323L));
        userIdDao.save(MarketUserId.fromUid(8142L, 2323L));
        userIdDao.save(MarketUserId.fromUid(8143L, 2323L));
        userIdDao.save(MarketUserId.fromUid(8144L, 2323L));
        userIdDao.save(MarketUserId.fromUid(8145L, 2323L));
        userIdDao.save(MarketUserId.fromUid(8146L, 2323L));

        loyaltyDao.saveCoin(LoyaltyCoin.builder()
                .uid(8142L)
                .coinId(32221L)
                .promoId(promo.getPromoId())
                .status("USED")
                .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .build());

        LoyaltyVerdictRequestDto requestDto = LoyaltyVerdictRequestDto.builder()
                .orderIds(Collections.singletonList(333L))
                .uid(8141L)
                .coins(Collections.singletonList(new CoinDto(112223L, promo.getPromoId())))
                .reason("SPEND")
                .userParams(OrderBuyerRequestDto.builder().build())
                .build();
        //language=json
        String verdictJson = "{\"verdict\":\"OTHER\",\"uids\":[8144,8145,8146,8141,8142,8143]," +
                "\"promos\":[{\"coinId\":112223,\"promoId\":2222,\"verdict\":\"USED\"}],\"firstOrder\":true}";
        mockMvc.perform(
                post("/antifraud/loyalty/detect")
                        .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(verdictJson));
    }

    @Test
    public void userRestrictions() throws Exception {
        BuyerRole role = BuyerRole.builder()
                .name("loyalty_restrictions_test")
                .detectorConfigurations(ImmutableMap.of(
                        "LOYALTY_PromoRestrictedDetector", new BaseDetectorConfiguration(true)
                ))
                .build();
        role = roleDao.saveRole(role);
        Long UID = 8145L;
        roleDao.addUidToRole(String.valueOf(UID), role);

        LoyaltyBuyerRestrictionsRequestDto request = new LoyaltyBuyerRestrictionsRequestDto(UID, null);
        //language=json
        String verdictJson = "{\"restriction\":\"PROHIBITED\",\"orderStats\":{" +
                "\"lastDayUserOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastWeekUserOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastDayGlueOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}," +
                "\"lastWeekGlueOrderStat\":{\"active\":0,\"delivered\":0,\"cancelled\":0,\"total\":0}}}";

        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                        .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(verdictJson));
    }

    @Test
    public void whiteList() throws Exception {
        LoyaltyPromo promo = testPromo(3333L);
        loyaltyDao.savePromo(promo);
        userIdDao.save(MarketUserId.fromUid(9141L, 3323L));
        userIdDao.save(MarketUserId.fromUid(9142L, 3323L));
        loyaltyDao.saveCoin(LoyaltyCoin.builder()
                .uid(9142L)
                .coinId(42221L)
                .promoId(promo.getPromoId())
                .status("USED")
                .startDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .build());
        BuyerRole role = roleDao.saveRole(BuyerRole.builder()
                .name("test-whitelist")
                .detectorConfigurations(ImmutableMap.of(
                        "LOYALTY_BlackListDetector", new BaseDetectorConfiguration(true),
                        "LOYALTY_UsedCoinsDetector", new BaseDetectorConfiguration(false)
                ))
                .build());
        roleDao.addUidToRole(String.valueOf(9141L), role);

        LoyaltyVerdictRequestDto requestDto = LoyaltyVerdictRequestDto.builder()
                .orderIds(Collections.singletonList(334L))
                .uid(9141L)
                .coins(Collections.singletonList(new CoinDto(442223L, promo.getPromoId())))
                .reason("SPEND")
                .userParams(OrderBuyerRequestDto.builder().build())
                .build();
        //language=json
        String verdictJson = "{\"verdict\":\"OK\",\"uids\":[9141,9142]," +
            "\"promos\":[{\"coinId\":442223,\"promoId\":3333,\"verdict\":\"OK\"}],\"firstOrder\":true}";
        mockMvc.perform(
                post("/antifraud/loyalty/detect")
                    .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(requestDto))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(verdictJson));
    }

    @SneakyThrows
    @Test
    public void walletTransactionsCount() {
        userIdDao.insertNewGlues(List.of(MarketUserId.fromUid(1123L), MarketUserId.fromUid(1124L)));
        when(ytClient.selectRows(any(SelectRowsRequest.class), any()))
            .thenReturn(CompletableFuture.completedFuture(List.of(
                YtWalletTransaction.builder()
                    .uid(1123)
                    .promoKey("promo_1")
                    .orderId(12L)
                    .timestamp(1636560172747L)
                    .amount(15_000)
                    .build(),
                YtWalletTransaction.builder()
                    .uid(1124)
                    .promoKey("promo_1")
                    .orderId(13L)
                    .timestamp(1636641290023L)
                    .amount(5_000)
                    .build(),
                YtWalletTransaction.builder()
                    .uid(1124)
                    .promoKey("promo_1")
                    .orderId(14L)
                    .timestamp(1636560180648L)
                    .amount(10_000)
                    .build()
            )));

        var requestDto = OrderCountRequestDtoV2.builder()
            .puid(1123)
            .requestItem("count", OrderCountRequestFiltersDto.builder()
                .promoFilters("promo_1")
                .build())
            .build();
        var verdictDto = OrderCountResponseDtoV2.builder()
            .puid(1123)
            .glueSize(2)
            .responseItem("count", OrderCountResponseItemDto.builder()
                .from(Instant.parse("2021-11-10T16:02:52.747Z"))
                .to(Instant.parse("2021-11-11T14:34:50.023Z"))
                .userOrderCount(new OrderCountDto(0, 0, 0, 1))
                .glueOrderCount(new OrderCountDto(0, 0, 0, 3))
                .build())
            .build();

        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        post("/antifraud/loyalty/orders-count/wallet-transactions")
                            .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(requestDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()))
            .andExpect(status().isOk())
            .andExpect(content().json(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(verdictDto)));

        verify(ytClient)
            .selectRows(AntifraudTestUtils.ytQueryThat(matchesPattern(
                " \\* FROM \\[.*] WHERE uid IN \\(1123, 1124\\) AND promo_key IN \\('promo_1'\\)"
            )), any());
    }

    private LoyaltyPromo testPromo(Long promoId) {
        return LoyaltyPromo.builder()
            .promoName("test_promo")
            .actionOnceRestrictionType("CHECK_USER")
            .bindOnlyOnce(true)
            .promoId(promoId)
            .build();
    }
}
