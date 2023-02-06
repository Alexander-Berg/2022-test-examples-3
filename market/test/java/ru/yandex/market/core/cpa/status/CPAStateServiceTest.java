package ru.yandex.market.core.cpa.status;

import java.time.LocalDate;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.api.cpa.CPAPrepaymentType;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@DbUnitDataSet(before = "CPAStateServiceTest.before.csv")
public class CPAStateServiceTest extends FunctionalTest {

    @Autowired
    private CPAStateService cpaStateService;

    @Test
    public void testStateVirtualShop() {
        CPAState state = cpaStateService.getCpaState(1001L);
        ReflectionAssert.assertReflectionEquals(
                CPAState.builder()
                        .canParticipateCPA(true)
                        .cpaReal(CPA.REAL)
                        .cpa(CPA.REAL)
                        .cpaReady(true)
                        .cpaActivity(true)
                        .cpaPartner(false)
                        .cpaAlive(true)
                        .cpaPrepaymentType(new CPAPrepaymentType(PaymentClass.YANDEX, PaymentClass.YANDEX))
                        .lastActivity(DateUtil.asDate(LocalDate.of(2020, 11, 28)))
                        .canSwitchOnApi(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchForTesting(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchToOff(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchForPartner(CanSwitchCPAStatus.DONT_NEED)
                        .build(),
                state,
                ReflectionComparatorMode.LENIENT_DATES);
    }

    @Test
    public void testStateVirtualCPC() {
        CPAState state = cpaStateService.getCpaState(1002L);
        ReflectionAssert.assertReflectionEquals(CPAState.builder().canParticipateCPA(false).build(), state);
    }

    @Test
    public void testStateDsbsFull() {
        CPAState state = cpaStateService.getCpaState(1003L);
        ReflectionAssert.assertReflectionEquals(
                CPAState.builder()
                        .canParticipateCPA(true)
                        .cpaReal(CPA.REAL)
                        .cpa(CPA.REAL)
                        .cpaReady(true)
                        .cpaActivity(true)
                        .cpaPartner(true)
                        .cpaAlive(true)
                        .cpaPrepaymentType(new CPAPrepaymentType(PaymentClass.YANDEX, PaymentClass.YANDEX))
                        .lastActivity(new Date())
                        .canSwitchOnApi(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchForTesting(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchToOff(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchForPartner(CanSwitchCPAStatus.DONT_NEED)
                        .build(),
                state,
                ReflectionComparatorMode.LENIENT_DATES);
    }

    @Test
    public void testStateDsbsConfigure() {
        CPAState state = cpaStateService.getCpaState(1004L);
        ReflectionAssert.assertReflectionEquals(
                CPAState.builder()
                        .canParticipateCPA(true)
                        .cpaReal(CPA.REAL)
                        .cpa(CPA.REAL)
                        .cpaReady(false)
                        .cpaActivity(true)
                        .cpaPartner(false)
                        .cpaAlive(true)
                        .cpaPrepaymentType(new CPAPrepaymentType(PaymentClass.YANDEX, PaymentClass.YANDEX))
                        .lastActivity(new Date())
                        .canSwitchOnApi(CanSwitchCPAStatus.IMPOSSIBLE)
                        .canSwitchForTesting(CanSwitchCPAStatus.IMPOSSIBLE)
                        .canSwitchToOff(CanSwitchCPAStatus.DONT_NEED)
                        .canSwitchForPartner(CanSwitchCPAStatus.DONT_NEED)
                        .build(),
                state,
                ReflectionComparatorMode.LENIENT_DATES);
    }


}
