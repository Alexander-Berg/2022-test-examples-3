package ru.yandex.market.checkout.checkouter.feature;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.feature.repository.CheckouterFeatureDao;
import ru.yandex.market.checkout.checkouter.feature.type.NamedFeatureTypeRegister;
import ru.yandex.market.checkout.checkouter.feature.type.logging.LoggingBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentComplexFeatureType;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryOptionsPolicy;
import ru.yandex.market.checkout.common.util.ItemServiceDefaultTimeInterval;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;

import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.ENABLED_FETCHERS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.ITEM_SERVICE_DEFAULT_TIME_INTERVALS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.SPASIBO_EXCLUDED_VENDORS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.FISCAL_AGENT_TYPE_ENABLED;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.ORDER_ARCHIVING_START_PERIOD;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY;
import static ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType.MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION;
import static ru.yandex.market.checkout.checkouter.feature.type.common.StringFeatureType.DEFAULT_SERVICE_PROVIDER_INN;

public class CheckouterFeatureResolverTest extends AbstractServicesTestBase {
    @Autowired
    private CheckouterFeatureResolver checkouterFeatureResolver;
    @Autowired
    private CachedService cachedCheckouterFeatureService;
    @Autowired
    private CheckouterFeatureService checkouterFeatureService;
    @Autowired
    private CheckouterFeatureDao checkouterFeatureDao;

    @BeforeEach
    public void beforeEach() {
        cachedCheckouterFeatureService.invalidateAll();
    }

    @AfterEach
    public void afterEach() {
        cachedCheckouterFeatureService.invalidateAll();
    }

    @Test
    public void getIntegerStringBoolean() {
        var intValue = 123;
        var stringValue = "432asd432";
        var booleanValue = true;

        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, intValue);
        Assertions.assertEquals(intValue,
                checkouterFeatureResolver.getInteger(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT));

        checkouterFeatureResolver.writeValue(DEFAULT_SERVICE_PROVIDER_INN, stringValue);
        Assertions.assertEquals(stringValue, checkouterFeatureResolver.getString(DEFAULT_SERVICE_PROVIDER_INN));
    }

    @Test
    public void getSetListMap() {
        var longValue = 9223372036854775807L;
        var bigDecimal = new BigDecimal("123.10");
        var setOfStringValue1 = "asdf";
        var setOfStringValue2 = "фыва";

        checkouterFeatureResolver.writeValue(ENABLED_FETCHERS, Set.of(setOfStringValue1, setOfStringValue2));
        var setOfStrings = checkouterFeatureResolver.getSet(ENABLED_FETCHERS, String.class);
        Assertions.assertTrue(setOfStrings.contains(setOfStringValue1) && setOfStrings.contains(setOfStringValue2));

        checkouterFeatureResolver.writeValue(SPASIBO_EXCLUDED_VENDORS, List.of(longValue, longValue));
        var listOfLongs = checkouterFeatureResolver.getSet(SPASIBO_EXCLUDED_VENDORS, Long.class);
        Assertions.assertFalse(listOfLongs.isEmpty());
        listOfLongs.forEach(item -> Assertions.assertEquals(longValue, item));

        checkouterFeatureResolver.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(longValue, bigDecimal));
        Map<Long, BigDecimal> map = checkouterFeatureResolver.getMap(MULTI_CART_MIN_COSTS_BY_REGION, Long.class,
                BigDecimal.class);
        Assertions.assertEquals(bigDecimal, map.get(longValue));
    }

    @Test
    public void getComplexTypes() {
        checkouterFeatureResolver.writeValue(LoggingBooleanFeatureType.CHECKOUTER_FEATURE, true);
        var enumValue = YaLavkaDeliveryOptionsPolicy.CHECK;
        var localDateTimeValue = LocalDateTime.of(2018, 9, 10, 0, 0, 0, 0);
        var switchWithWhiteListValue = new SwitchWithWhitelist<>(true, Set.of(9223372036854775805L,
                9223372036854775806L, 9223372036854775807L));

        checkouterFeatureResolver.writeValue(YA_LAVKA_DELIVERY_OPTIONS_POLICY, enumValue);
        Assertions.assertEquals(enumValue, checkouterFeatureResolver.getAsTargetType(YA_LAVKA_DELIVERY_OPTIONS_POLICY,
                YaLavkaDeliveryOptionsPolicy.class));

        checkouterFeatureResolver.writeValue(ORDER_ARCHIVING_START_PERIOD, localDateTimeValue);
        Assertions.assertEquals(localDateTimeValue,
                checkouterFeatureResolver.getAsTargetType(ORDER_ARCHIVING_START_PERIOD, LocalDateTime.class));

        checkouterFeatureResolver.writeValue(FISCAL_AGENT_TYPE_ENABLED, switchWithWhiteListValue);
        Assertions.assertEquals(switchWithWhiteListValue,
                checkouterFeatureResolver.getAsTargetType(FISCAL_AGENT_TYPE_ENABLED, SwitchWithWhitelist.class));
    }

    @Test
    public void getHistory() {
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 1);
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 2);
        Assertions.assertEquals(2, checkouterFeatureResolver.getInteger(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT));
        checkouterFeatureResolver.writeValue(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT, 3);

        var histories =
                checkouterFeatureService.getFeatureHistory(MIN_RECEIPT_PAYLOAD_LENGTH_TO_REPAIR_RECEIPT.getName(),
                        null, null);
        Assertions.assertTrue(histories.size() >= 3);
    }

    @Test
    public void eachEnumRegisteredByMigration() {
        NamedFeatureTypeRegister.getAllowableTypes().values().forEach(type ->
                Assertions.assertTrue(checkouterFeatureDao.findFeature(type.getName()).isPresent(),
                        "There is no migration for " + type));
    }

    @Test
    public void workWithComplexCollection() {
        var interval1 = ItemServiceDefaultTimeInterval.of(8, 0, 14, 0);
        var interval2 = ItemServiceDefaultTimeInterval.of(14, 0, 20, 0);
        var result = checkouterFeatureResolver.getList(ITEM_SERVICE_DEFAULT_TIME_INTERVALS,
                ItemServiceDefaultTimeInterval.class);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.containsAll(List.of(interval1, interval2)));
    }

    @Test
    public void workWithBigDecimal() {
        var actual = checkouterFeatureResolver.getAsTargetType(PermanentComplexFeatureType.LIMIT_ORDER_MAX_AMOUNT,
                BigDecimal.class);
        Assertions.assertEquals(new BigDecimal("600000"), actual);
    }
}
