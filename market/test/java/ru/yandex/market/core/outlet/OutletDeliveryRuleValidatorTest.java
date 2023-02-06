package ru.yandex.market.core.outlet;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.common.framework.core.ErrorInfo;
import ru.yandex.common.framework.core.SimpleErrorInfo;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link OutletDeliveryRuleValidator валидацию правил доставки}.
 *
 * @author fbokovikov
 */
@RunWith(MockitoJUnitRunner.class)
public class OutletDeliveryRuleValidatorTest {

    private static final long SHOP_ID = 774L;
    private static final long REGION_ID = 1L;
    private static final long LOCAL_REGION_ID = 213L;

    @Mock
    private ParamService paramService;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private OutletDeliveryRuleValidator deliveryRuleValidator;

    @Before
    public void prepareMocks() {
        when(paramService.getParamNumberValue(ParamType.LOCAL_DELIVERY_REGION, SHOP_ID))
                .thenReturn(new BigDecimal(LOCAL_REGION_ID));
    }

    /**
     * Валидация для non-global кейса (maxDays - minDays < 18). Слишком большая разница между maxDays и minDays
     */
    @Test
    public void testNonGlobalRuleTooBigDateDiff() {
        DeliveryRule rule = prepareDeliveryRule();
        rule.setMaxDeliveryDays(30);
        rule.setMinDeliveryDays(17);
        List<ErrorInfo> errorInfos = deliveryRuleValidator.validateOutletDeliveryRules(rule, SHOP_ID, REGION_ID);
        ReflectionAssert.assertReflectionEquals(
                Collections.singletonList(new SimpleErrorInfo("datediff-is-to-big-remote")),
                errorInfos
        );
    }

    /**
     * Валидация для global кейса (maxDays - minDays >= 18). Слишком большая разница между maxDays и minDays
     */
    @Test
    public void testGlobalRuleTooBigDateDiff() {
        DeliveryRule rule = prepareDeliveryRule();
        rule.setMaxDeliveryDays(40);
        rule.setMinDeliveryDays(19);
        List<ErrorInfo> errorInfos = deliveryRuleValidator.validateOutletDeliveryRules(rule, SHOP_ID, REGION_ID);
        ReflectionAssert.assertReflectionEquals(
                Collections.singletonList(new SimpleErrorInfo("datediff-is-to-big-long-period")),
                errorInfos
        );
    }

    /**
     * Валидация для global кейса (maxDays - minDays >= 18).
     */
    @Test
    public void testGlobalRuleOk() {
        DeliveryRule rule = prepareDeliveryRule();
        rule.setMaxDeliveryDays(30);
        rule.setMinDeliveryDays(18);
        List<ErrorInfo> errorInfos = deliveryRuleValidator.validateOutletDeliveryRules(rule, SHOP_ID, REGION_ID);
        assertTrue(errorInfos.isEmpty());
    }

    /**
     * Проверяем, что в приоритетном регионе доставки не получится установить 3+ дней
     */
    @Test
    public void testPriorityRegionRule() {
        DeliveryRule rule = prepareDeliveryRule();
        rule.setMaxDeliveryDays(21);
        rule.setMinDeliveryDays(18);
        List<ErrorInfo> errorInfos = deliveryRuleValidator.validateOutletDeliveryRules(rule, SHOP_ID, LOCAL_REGION_ID);
        ReflectionAssert.assertReflectionEquals(
                Collections.singletonList(new SimpleErrorInfo("datediff-is-to-big-local")),
                errorInfos
        );
    }

    /**
     * Проверяем, что нельзя выставить отрицательные значения
     */
    @Test
    public void testNegativeMinDeliveryDays() {
        DeliveryRule rule = prepareDeliveryRule();
        rule.setMaxDeliveryDays(7);
        rule.setMinDeliveryDays(-1);
        List<ErrorInfo> errorInfos = deliveryRuleValidator.validateOutletDeliveryRules(rule, SHOP_ID, LOCAL_REGION_ID);
        ReflectionAssert.assertReflectionEquals(
                Collections.singletonList(new SimpleErrorInfo("min-delivery-days-cannot-be-negative")),
                errorInfos
        );
    }

    private DeliveryRule prepareDeliveryRule() {
        DeliveryRule rule = new DeliveryRule();
        rule.setCost(BigDecimal.TEN);
        return rule;
    }
}
