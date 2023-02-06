package ru.yandex.market.antifraud.orders.test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.node.BooleanNode;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.market.antifraud.orders.detector.ItemAutoLimitDetector;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDaoImpl;
import ru.yandex.market.antifraud.orders.storage.dao.ItemLimitRulesDao;
import ru.yandex.market.antifraud.orders.storage.dao.RoleDao;
import ru.yandex.market.antifraud.orders.storage.dao.StatDao;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.storage.entity.limits.LimitRuleType;
import ru.yandex.market.antifraud.orders.storage.entity.limits.PgItemLimitRule;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.ItemAutoLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.stat.ItemPeriodicCountStat;
import ru.yandex.market.antifraud.orders.storage.entity.stat.PeriodStatValues;
import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;
import ru.yandex.market.antifraud.orders.test.providers.OrderDeliveryProvider;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderItem;
import ru.yandex.market.crm.platform.profiles.Facts;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnSortOrder;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dzvyagin
 */
@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AntifraudServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NamedParameterJdbcOperations pgaasJdbcOperations;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private StatDao statDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private ItemLimitRulesDao itemLimitRulesDao;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private YtClient ytClient;

    private AntifraudDao antifraudDao;

    @Before
    public void init() {
        antifraudDao = new AntifraudDaoImpl(pgaasJdbcOperations);
        configurationService.save(ConfigurationEntity.builder()
                .parameter(ConfigEnum.VOLVA_CHECK)
                .config(BooleanNode.FALSE)
                .build());
    }

    @Test
    public void testStationWhitelisted() throws Exception {
        insertInBlackList(917685447L);
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(917685447L)
                        .email("stationorders@yandex.ru")
                        .build())
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(Collections.singletonList(OrderItemRequestDto.builder()
                        .id(333L)
                        .categoryId(444)
                        .price(BigDecimal.valueOf(999))
                        .count(3)
                        .supplierId(1L)
                        .build()))
                .build();
        OrderVerdict response = OrderVerdict.EMPTY;
        check(request, response);
    }

    @Test
    public void testYandexoidWhitelisted() throws Exception {
        insertInBlackList(374670152L);
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(374670152L)
                        .email("yandexoid@yandex.ru")
                        .build())
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(Collections.singletonList(OrderItemRequestDto.builder()
                        .id(333L)
                        .price(BigDecimal.valueOf(999))
                        .count(3)
                        .categoryId(444)
                        .supplierId(1L)
                        .build()))
                .build();
        OrderVerdict response = OrderVerdict.EMPTY;
        check(request, response);
    }

    @Test
    public void testCategoryRules() throws Exception {
        itemLimitRulesDao.saveRule(PgItemLimitRule.builder()
                .msku(100639316533L)
                .periodDays(7)
                .maxCount(1L)
                .ruleType(LimitRuleType.GLUE)
                .build());
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(311870046L)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .id(333L)
                        .msku(100639316533L)
                        .price(BigDecimal.valueOf(1488))
                        .count(3)
                        .modelId(123L)
                        .categoryId(444)
                        .supplierId(1L)
                        .build()))
                .build();
        OrderVerdict response = OrderVerdict.builder()
                .checkResults(Set.of(
                        new AntifraudCheckResult(
                                AntifraudAction.ORDER_ITEM_CHANGE,
                                "Состав корзины был изменен в связи с категорийными ограничениями",
                                "Состав корзины был изменен в связи с категорийными ограничениями"
                        )
                ))
                .build();
        check(request, response);
    }

    @Test
    public void testAutoCategoryRule() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(311870044L)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .id(333L)
                        .msku(123123124L)
                        .price(BigDecimal.valueOf(1488))
                        .count(123)
                        .modelId(123L)
                        .categoryId(444)
                        .supplierId(123L)
                        .build()))
                .build();
        statDao.saveItemPeriodicCountStat(ItemPeriodicCountStat.builder()
                .msku(123123124L)
                .modelId(123L)
                .categoryId(444)
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(6))
                        .countAvgUser(BigDecimal.valueOf(3))
                        .countSigmaGlue(BigDecimal.valueOf(12))
                        .build())
                .build());
        OrderVerdict response = OrderVerdict.builder()
                .checkResults(Set.of(
                        new AntifraudCheckResult(
                                AntifraudAction.ORDER_ITEM_CHANGE,
                                "Состав корзины был изменен в связи с автоматическими категорийными ограничениями",
                                "Состав корзины был изменен в связи с автоматическими категорийными ограничениями"
                        )
                ))
                .build();
        check(request, response);
    }

    @Test
    public void testAutoCategoryRulePrepay() throws Exception {
        ItemAutoLimitDetector stub = new ItemAutoLimitDetector(null, null, null);
        ItemAutoLimitDetectorConfiguration conf = stub.defaultConfiguration();
        String accountStateQuery =
                "uid, karma, created_rules FROM [//home/market/production/mstat/antifraud/accounts/accounts_state] " +
                "WHERE uid=311870049";
        String passportFeaturesQuery =
                "uid, karma, account_created, rules, is_hosting_ip, is_suggested_login, age, is_proxy_ip, is_mobile_ip, is_vpn_ip " +
                "FROM [//home/market/production/mstat/antifraud/accounts/passport_features_fast] WHERE uid=311870049";
        when(ytClient.selectRows(accountStateQuery)).thenReturn(CompletableFuture.completedFuture(buildAccountStateRowset()));
        when(ytClient.selectRows(passportFeaturesQuery)).thenReturn(CompletableFuture.completedFuture(buildPassportFeaturesRowset()));
        BuyerRole role = roleDao.saveRole(BuyerRole.builder()
                .description("test prepay role")
                .name("test-prepay")
                .detectorConfigurations(Map.of(
                        stub.getUniqName(),
                        conf
                ))
                .build());
        roleDao.addUidToRole("311870049", role);
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(311870049L)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .id(333L)
                        .msku(123123123L)
                        .price(BigDecimal.valueOf(1488))
                        .count(123)
                        .modelId(123L)
                        .categoryId(444)
                        .supplierId(123L)
                        .build()))
                .build();
        statDao.saveItemPeriodicCountStat(ItemPeriodicCountStat.builder()
                .msku(123123123L)
                .modelId(123L)
                .categoryId(444)
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(6))
                        .countAvgUser(BigDecimal.valueOf(3))
                        .countSigmaGlue(BigDecimal.valueOf(12))
                        .build())
                .build());
        OrderVerdict response = OrderVerdict.builder()
                .checkResults(Set.of(
                        new AntifraudCheckResult(
                                AntifraudAction.PREPAID_ONLY,
                                "Принудительная предоплата в связи с автоматическими категорийными ограничениями",
                                "Принудительная предоплата в связи с автоматическими категорийными ограничениями"
                        )
                ))
                .build();
        check(request, response);
    }

    @Test
    public void testCategoryPgRuleGoodAcc() throws Exception {
        long msku = 123123125L;
        ItemAutoLimitDetector stub = new ItemAutoLimitDetector(null, null, null);
        ItemAutoLimitDetectorConfiguration conf = stub.defaultConfiguration();
        String accountStateQuery =
                "uid, karma, created_rules FROM [//home/market/production/mstat/antifraud/accounts/accounts_state] " +
                        "WHERE uid=311870049";
        String passportFeaturesQuery =
                "uid, karma, account_created, rules, is_hosting_ip, is_suggested_login, age, is_proxy_ip, is_mobile_ip, is_vpn_ip " +
                        "FROM [//home/market/production/mstat/antifraud/accounts/passport_features_fast] WHERE uid=311870049";
        when(ytClient.selectRows(accountStateQuery)).thenReturn(CompletableFuture.completedFuture(buildAccountStateRowset()));
        when(ytClient.selectRows(passportFeaturesQuery)).thenReturn(CompletableFuture.completedFuture(buildPassportFeaturesRowset()));
        BuyerRole role = roleDao.saveRole(BuyerRole.builder()
                .description("test prepay role")
                .name("test-prepay")
                .detectorConfigurations(Map.of(
                        stub.getUniqName(),
                        conf
                ))
                .build());
        roleDao.addUidToRole("311870049", role);
        itemLimitRulesDao.saveRule(PgItemLimitRule.builder()
                .msku(msku)
                .periodDays(7)
                .maxCount(1L)
                .ruleType(LimitRuleType.USER)
                .build());
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(311870049L)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .id(333L)
                        .msku(msku)
                        .price(BigDecimal.valueOf(1488))
                        .count(12)
                        .modelId(123L)
                        .categoryId(444)
                        .supplierId(123L)
                        .build()))
                .build();
        statDao.saveItemPeriodicCountStat(ItemPeriodicCountStat.builder()
                .msku(msku)
                .modelId(123L)
                .categoryId(444)
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(6))
                        .countAvgUser(BigDecimal.valueOf(3))
                        .countSigmaGlue(BigDecimal.valueOf(12))
                        .countSigmaUser(BigDecimal.valueOf(1))
                        .build())
                .build());
        OrderVerdict response = OrderVerdict.builder()
                .checkResults(Set.of(
                        new AntifraudCheckResult(
                                AntifraudAction.ORDER_ITEM_CHANGE,
                                "Состав корзины был изменен в связи с категорийными ограничениями",
                                "Состав корзины был изменен в связи с категорийными ограничениями"
                        )
                ))
                .build();
        check(request, response);
    }

    @Test
    public void testCategoryGoodAccAboveMultipliedLimit() throws Exception {
        long msku = 123123126L;
        ItemAutoLimitDetector stub = new ItemAutoLimitDetector(null, null, null);
        ItemAutoLimitDetectorConfiguration conf = stub.defaultConfiguration();
        String accountStateQuery =
                "uid, karma, created_rules FROM [//home/market/production/mstat/antifraud/accounts/accounts_state] " +
                        "WHERE uid=311870049";
        String passportFeaturesQuery =
                "uid, karma, account_created, rules, is_hosting_ip, is_suggested_login, age, is_proxy_ip, is_mobile_ip, is_vpn_ip " +
                        "FROM [//home/market/production/mstat/antifraud/accounts/passport_features_fast] WHERE uid=311870049";
        when(ytClient.selectRows(accountStateQuery)).thenReturn(CompletableFuture.completedFuture(buildAccountStateRowset()));
        when(ytClient.selectRows(passportFeaturesQuery)).thenReturn(CompletableFuture.completedFuture(buildPassportFeaturesRowset()));
        BuyerRole role = roleDao.saveRole(BuyerRole.builder()
                .description("test prepay role")
                .name("test-prepay")
                .detectorConfigurations(Map.of(
                        stub.getUniqName(),
                        conf
                ))
                .build());
        roleDao.addUidToRole("311870049", role);
        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(311870049L)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .id(333L)
                        .msku(msku)
                        .price(BigDecimal.valueOf(1488))
                        .count(123)
                        .modelId(123L)
                        .categoryId(444)
                        .supplierId(123L)
                        .build()))
                .build();
        statDao.saveItemPeriodicCountStat(ItemPeriodicCountStat.builder()
                .msku(msku)
                .modelId(123L)
                .categoryId(444)
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(6))
                        .countAvgUser(BigDecimal.valueOf(3))
                        .countSigmaGlue(BigDecimal.valueOf(12))
                        .countSigmaUser(BigDecimal.valueOf(1))
                        .build())
                .build());
        OrderVerdict response = OrderVerdict.builder()
                .checkResults(Set.of(
                        new AntifraudCheckResult(
                                AntifraudAction.ORDER_ITEM_CHANGE,
                                "Принудительная предоплата и изменение состава корзины в связи с автоматическими категорийными ограничениями",
                                "Принудительная предоплата и изменение состава корзины в связи с автоматическими категорийными ограничениями"
                        ),
                        new AntifraudCheckResult(
                                AntifraudAction.PREPAID_ONLY,
                                "Принудительная предоплата и изменение состава корзины в связи с автоматическими категорийными ограничениями",
                                "Принудительная предоплата и изменение состава корзины в связи с автоматическими категорийными ограничениями"
                        )
                ))
                .build();
        check(request, response);
    }

    @Test
    @Ignore // по непоняттным причинам не работает в аркадии О_о
    public void testYaKolonkaFixed() throws Exception {
        itemLimitRulesDao.saveRule(PgItemLimitRule.builder()
                .modelId(1971204201L)
                .periodDays(180)
                .maxCount(5L)
                .ruleType(LimitRuleType.GLUE)
                .tag("testYaKolonkaFixed")
                .build());
        final Long UID = 321870092L;
        Cache cache = cacheManager.getCache("orders_cache");
        cache.put(UID, Facts.newBuilder()
                .addOrder(Order.newBuilder()
                        .setKeyUid(Uid.newBuilder()
                                .setType(UidType.PUID)
                                .setIntValue(UID)
                                .build())
                        .addItems(OrderItem.newBuilder()
                                .setSku("1971204201")
                                .setModelId(1971204201L)
                                .setPrice(1234567L)
                                .setCount(3)
                                .build())
                        .setRgb(RGBType.BLUE)
                        .setCreationDate(Instant.now().minus(3, ChronoUnit.DAYS).toEpochMilli())
                        .setStatus("DELIVERED")
                        .build())
                .build());

        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(UID)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .msku(1234L)
                        .price(BigDecimal.valueOf(1488))
                        .count(3)
                        .modelId(1971204201L)
                        .categoryId(444)
                        .supplierId(1L)
                        .build()))
                .build();
        OrderVerdict response = OrderVerdict.builder()
                .checkResults(Set.of(
                        new AntifraudCheckResult(
                                AntifraudAction.ORDER_ITEM_CHANGE,
                                "Состав корзины был изменен в связи с категорийными ограничениями",
                                "Состав корзины был изменен в связи с категорийными ограничениями"
                        )
                ))
                .build();
        check(request, response);
    }

    @Test
    public void testYaKolonkaNotFixed() throws Exception {
        final Long UID = 311870045L;
        Cache cache = cacheManager.getCache("orders_cache");
        cache.put(UID, Facts.newBuilder()
                .addOrder(Order.newBuilder()
                        .setKeyUid(Uid.newBuilder()
                                .setType(UidType.PUID)
                                .setIntValue(UID)
                                .build())
                        .addItems(OrderItem.newBuilder()
                                .setSku("1234")
                                .setModelId(1971204201L)
                                .setPrice(1234567L)
                                .setCount(3)
                                .build())
                        .setRgb(RGBType.BLUE)
                        .setCreationDate(Instant.now().minus(3, ChronoUnit.DAYS).toEpochMilli())
                        .setStatus("CANCELLED")
                        .build())
                .build());

        OrderRequestDto request = OrderRequestDto.builder()
                .buyer(OrderBuyerRequestDto.builder()
                        .uid(UID)
                        .email("docdvz@yandex.ru")
                        .build())
                .fulfilment(true)
                .delivery(OrderDeliveryProvider.getOrderDeliveryRequest())
                .items(List.of(OrderItemRequestDto.builder()
                        .msku(1234L)
                        .price(BigDecimal.valueOf(1488))
                        .count(3)
                        .modelId(1971204201L)
                        .categoryId(444)
                        .supplierId(1L)
                        .build()))
                .build();
        OrderVerdict response = OrderVerdict.EMPTY;
        check(request, response);
    }

    private void check(OrderRequestDto request, OrderVerdict response) throws Exception {
        mockMvc.perform(
                post("/antifraud/detect")
                        .content(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(response)));
    }

    @SneakyThrows
    private void insertInBlackList(Long uid) {
        antifraudDao.saveBlacklistRule(
                AntifraudBlacklistRule.builder()
                        .type(AntifraudBlacklistRuleType.UID)
                        .value(uid.toString())
                        .action(Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER))
                        .reason("ban yandex station lol")
                        .expiryAt(AntifraudDao.DEFAULT_EXPIRE_DATE)
                        .build()
        );
    }

    private UnversionedRowset buildAccountStateRowset() {
        TableSchema tableSchema = new TableSchema.Builder()
                .addAll(List.of(
                        new ColumnSchema("uid", ColumnValueType.UINT64, ColumnSortOrder.ASCENDING),
                        new ColumnSchema("karma", ColumnValueType.UINT64),
                        new ColumnSchema("created_rules", ColumnValueType.ANY)
                ))
                .setStrict(true)
                .setUniqueKeys(true)
                .build();

        List<UnversionedRow> unversionedRows = List.of(
                new UnversionedRow(List.of(
                        new UnversionedValue(0, ColumnValueType.UINT64, false, 311870049L),
                        new UnversionedValue(1, ColumnValueType.UINT64, false, 6000L),
                        new UnversionedValue(2, ColumnValueType.ANY, false,
                                UnversionedValue.convertValueTo(new YTreeListNodeImpl(new EmptyMap<>()), ColumnValueType.ANY))
                )));
        return new UnversionedRowset(tableSchema, unversionedRows);
    }

    private UnversionedRowset buildPassportFeaturesRowset() {
        TableSchema tableSchema = new TableSchema.Builder()
                .addAll(List.of(
                        new ColumnSchema("uid", ColumnValueType.UINT64, ColumnSortOrder.ASCENDING),
                        new ColumnSchema("karma", ColumnValueType.STRING),
                        new ColumnSchema("account_created", ColumnValueType.BOOLEAN),
                        new ColumnSchema("rules", ColumnValueType.ANY)

                ))
                .setStrict(true)
                .setUniqueKeys(true)
                .build();

        List<UnversionedRow> unversionedRows = List.of(new UnversionedRow(List.of(
                        new UnversionedValue(0, ColumnValueType.UINT64, false, 311870049L),
                        new UnversionedValue(1, ColumnValueType.STRING, false, "6000".getBytes()),
                        new UnversionedValue(2, ColumnValueType.BOOLEAN, false, false),
                        new UnversionedValue(3, ColumnValueType.ANY, false,
                                UnversionedValue.convertValueTo(new YTreeListNodeImpl(new EmptyMap<>()), ColumnValueType.ANY))
                ))
        );
        return new UnversionedRowset(tableSchema, unversionedRows);
    }
}
