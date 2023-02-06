package ru.yandex.market.core.program.partner.calculator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.program.partner.model.Substatus;

import static ru.yandex.market.core.program.partner.calculator.Substatus2FeatureCutoffTypeMapper.SUBSTATUS_CUTOFF_MAPPER;

/**
 * Тест проверяет, чтобы не забыли добавить маппинг подстатуса на отключение при
 * добавлении нового подстатуса. Если этого не сделать, то в партнерке не отобразится уведомление на
 * соответствующее отключение. Может быть такое что добавлять маппинг не нужно, тогда нужно добавить его
 * в исключения.
 */
public class Substatus2FeatureCutoffTypeMapperTest {

    private Set<Substatus> excludedSubstatuses = Set.of(
            Substatus.SUSPENDED_BY_MANAGER,
            Substatus.FEED_FAILED,
            Substatus.FEED_DROPPED,
            Substatus.MISSING_PARAMS,
            Substatus.SITE_DISABLED,
            Substatus.QUALITY_FAILED,
            Substatus.PREMODERATION_NEEDED,
            Substatus.NEED_MONEY,
            Substatus.SUSPENDED_BY_SCHEDULE,
            Substatus.SUSPENDED_BY_PLACEMENT,
            Substatus.SUSPENDED_BY_PROGRAM,
            Substatus.TESTING_DROPPED,
            Substatus.MISSING_DATAFEED,
            Substatus.DAILY_FINANCE_LIMIT,
            Substatus.CONFIRMATION_REQUIRED,
            Substatus.NEED_INFO,
            Substatus.LEGAL_INFO,
            Substatus.FEATURE_CUTOFF_TYPES,
            Substatus.SANDBOX,
            Substatus.STOCKS,
            Substatus.FILL_APPLICATION,
            Substatus.MARKETPLACE_PLACEMENT,
            Substatus.CLOSED,
            Substatus.ORDERS_RESTRICTED,
            Substatus.ON_MODERATION,
            Substatus.SORT_CENTER_NOT_CONFIGURED,
            Substatus.WORK_MODE,
            Substatus.NO_LOADED_OFFERS,
            Substatus.DAILY_ORDER_LIMIT,
            Substatus.ORDER_FINAL_STATUS_NOT_SET,
            Substatus.DELIVERY_NOT_CONFIGURED,
            Substatus.LOW_RATING,
            Substatus.NPD_UNAVAILABLE,
            Substatus.SELFCHECK_REQUIRED
    );

    @Test
    public void testMissingSubstatusMapping() {
        List<Substatus> missing = Arrays.stream(Substatus.values())
                .filter(s -> SUBSTATUS_CUTOFF_MAPPER.get(s.getId()) == null)
                .filter(s -> !excludedSubstatuses.contains(s))
                .collect(Collectors.toList());

        Assertions.assertTrue(missing.isEmpty(), "Missing mappings for substatuses: " + missing);
    }
}
