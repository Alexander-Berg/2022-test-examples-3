package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.util.Collection;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.DataVersionDao;
import ru.yandex.market.loyalty.core.dao.MskuAttributesDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.core.model.MskuAttributesRecord;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.spring.utils.AbstractBatchedConsumer;
import ru.yandex.market.loyalty.test.TestFor;

import static java.math.BigDecimal.valueOf;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.model.CoreMarketPlatform.BLUE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.trigger.Trigger.CHANCE_EPSILON;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.coin.CoinPromoConversionCalculator.DEFAULT_CONVERSION;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_ORDER_STATUS_PREDICATE;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;

@TestFor(TriggerChangesUpdateProcessor.class)
public class TriggerChangesUpdateProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final long HID_1 = 16089018;
    private static final long HID_2 = 12312131;
    private static final long HID_3 = 879465459;


    @Autowired
    private TriggerChangesUpdateProcessor triggerChangesUpdateProcessor;
    @Autowired
    private MskuAttributesDao mskuAttributesDao;
    @Autowired
    private DataVersionDao dataVersionDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerDao triggerDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    @YtHahn
    private JdbcTemplate jdbcTemplate;

    private static final BigDecimal MED_PRICE = valueOf(2500);
    private static final BigDecimal AVG_PRICE = valueOf(2500);
    private static final ImmutableSet<MskuAttributesRecord> MSKU_ATTRIBUTES_RECORDS_INIT_SET = ImmutableSet.of(
            record(HID_1, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_1 + "_1"),
            record(HID_1, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_1 + "_2"),
            record(HID_1, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_1 + "_3"),
            record(HID_2, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_2 + "_1"),
            record(HID_2, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_2 + "_2"),
            record(HID_2, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_2 + "_3"),
            record(HID_3, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_3 + "_1"),
            record(HID_3, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_3 + "_2"),
            record(HID_3, 0L, BLUE, 1L, MED_PRICE, AVG_PRICE, HID_3 + "_3")
    );

    @Test
    public void shouldUpdateFreeDeliveryCoinPromoTriggerChance() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFreeDelivery().setExpiration(ExpirationPolicy.expireByDays(31)));
        Trigger<OrderStatusUpdatedEvent> t = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));
        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        populateMskuAttributesTable();
        triggerChangesUpdateProcessor.triggerChancesUpdate();

        assertEquals(0.1012841949117791, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);
    }

    @Test
    public void shouldUpdateFixedCoinPromoTriggerChance() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed(valueOf(500))
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (int) HID_2));
        Trigger<OrderStatusUpdatedEvent> t = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));
        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        populateMskuAttributesTable();
        triggerChangesUpdateProcessor.triggerChancesUpdate();

        assertEquals(0.3050861480678873, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);
    }

    @Test
    public void shouldUpdatePercentCoinPromoTriggerChance() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultPercent(valueOf(15))
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (int) HID_2));
        Trigger<OrderStatusUpdatedEvent> t = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));
        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        populateMskuAttributesTable();
        triggerChangesUpdateProcessor.triggerChancesUpdate();

        assertEquals(0.020720663401691763, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);
    }

    @Test
    public void shouldSetPercentCoinPromoTriggerChanceOnPromoCreate() {
        populateMskuAttributesTable();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed(valueOf(500))
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (int) HID_2));
        Trigger<OrderStatusUpdatedEvent> t = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));

        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        assertEquals(0.3050861480678873, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);
    }

    @Test
    public void shouldSetPercentCoinPromoTriggerChanceOnPromoUpdate() {
        populateMskuAttributesTable();

        SmartShoppingPromoBuilder builder = PromoUtils.SmartShopping
                .defaultFixed(valueOf(500));

        Promo promo = promoManager.createSmartShoppingPromo(builder
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (int) HID_2));

        Trigger<OrderStatusUpdatedEvent> t = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));

        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        assertEquals(0.3050861480678873, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);

        promoManager.updateCoinPromo(builder.replaceCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID,
                ImmutableSet.of((int) HID_2, (int) HID_1)));

        assertEquals(0.3484961306148365, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);

    }

    @Test
    public void shouldSetPercentCoinPromoTriggerChanceOnPromoCreateAndMskuAttributesTableNotPopulated() {
        SmartShoppingPromoBuilder builder = PromoUtils.SmartShopping
                .defaultFixed(valueOf(500));

        Promo promo = promoManager.createSmartShoppingPromo(builder
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, (int) HID_2));

        Trigger<OrderStatusUpdatedEvent> t = triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE)
        );
        promoService.updateStatus(promo, PromoStatus.ACTIVE);

        assertEquals(DEFAULT_CONVERSION, triggerDao.getTriggerById(t.getId()).getChance(), CHANCE_EPSILON);
    }

    @NotNull
    private static Answer records(ImmutableSet<MskuAttributesRecord> records) {
        return invocation -> {
            @SuppressWarnings({"unchecked", "resource"})
            AbstractBatchedConsumer<MskuAttributesRecord, Collection<?>> consumer =
                    (AbstractBatchedConsumer<MskuAttributesRecord, Collection<?>>) invocation.getArgument(1,
                            AbstractBatchedConsumer.class);
            records.forEach(consumer::processRow);
            return null;
        };
    }


    private void populateMskuAttributesTable() {
        long dataVersionNum = dataVersionDao.createDataVersionNum();
        dataVersionDao.saveDataVersion(DataVersion.MSKU_ATTRIBUTES, dataVersionNum);
        mskuAttributesDao.saveMskuAttributesRecords(
                dataVersionNum,
                MSKU_ATTRIBUTES_RECORDS_INIT_SET
        );
    }

    private static MskuAttributesRecord record(
            long hid, long vendorId, CoreMarketPlatform marketPlatform, long offers, BigDecimal medPrice,
            BigDecimal avgPrice,
            String msku
    ) {
        return new MskuAttributesRecord(
                hid, vendorId, marketPlatform, offers, avgPrice, medPrice, msku
        );
    }
}
