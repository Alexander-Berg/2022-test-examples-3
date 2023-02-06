package ru.yandex.autotests.market.billing.backend.shopDataExecutor;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.data.wiki.ShopsProvider;
import ru.yandex.autotests.market.billing.backend.steps.ShopPrepaymentSteps;
import ru.yandex.autotests.market.partner.backend.steps.CpaPaymentUpdateSteps;
import ru.yandex.autotests.mbi.api.steps.ParamSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.hazelcast.LockRule;

import static ru.yandex.autotests.market.billing.beans.paramType.ParamType.PREPAY_ENABLED;

/**
 * @author Dmitriy Polyanov <a href="mailto:neiwick@yandex-team.ru"></a>
 * @date 08.09.16
 */
@Aqua.Test(title = "Проверка прокидывания параметра включения/выключения предоплаты в shops.dat/dynamic")
@Feature("shopdata")
@Description("Последовательно обновляет состояние предоплаты магазина и на каждом этапе производит проверку"
        + " изменений в shops.dat и маркет-динамике")
@Issue("AUTOTESTMARKET-3337")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShopPrepaymentTest {
    public static final String PREPAY_DONT_WANT = "DONT_WANT";
    public static final String PREPAY_NEW = "NEW";
    public static final String PREPAY_SUCCESS = "SUCCESS";
    @Parameter("Магазин")
    private static final Long SHOP_ID = ShopsProvider.getShop("ShopPrepaymentTest");
    @Parameter("Пользователь")
    private static final Long USER_ID = 79668854L;
    private static final ShopPrepaymentSteps shopPrepaymentSteps = new ShopPrepaymentSteps();
    private static final ParamSteps paramSteps = new ParamSteps();
    @ClassRule
    public static LockRule lockRule = new LockRule("${mbi.billing.db.servicename} - shopdata_lock");

    @BeforeClass
    public static void setUp() {
        // подкручиваем магазин до начального состояния - DONT_WANT
        CpaPaymentUpdateSteps.updatePaymentCheck(SHOP_ID, USER_ID, PREPAY_DONT_WANT);
    }

    @Test
    public void testScenario1EnablePrepayment() throws Exception {
        shopPrepaymentSteps.checkPrepaymentStatus(SHOP_ID, PREPAY_DONT_WANT);

        // Update status and check, that correctly updated
        CpaPaymentUpdateSteps.updatePaymentCheck(SHOP_ID, USER_ID, PREPAY_NEW);
        shopPrepaymentSteps.checkPrepaymentStatus(SHOP_ID, PREPAY_NEW);

        paramSteps.setParam(SHOP_ID, PREPAY_ENABLED.value(), PREPAY_SUCCESS);

        shopPrepaymentSteps.checkShopsDatHasShopWithPrepayment(SHOP_ID);
        shopPrepaymentSteps.checkDynamicHasShopWithPrepayment(SHOP_ID);
    }

    @Test
    public void testScenario2DisablePrepayment() throws Exception {
        CpaPaymentUpdateSteps.updatePaymentCheck(SHOP_ID, USER_ID, PREPAY_DONT_WANT);

        shopPrepaymentSteps.checkShopsDatHasNoShopWithPrepayment(SHOP_ID);
        shopPrepaymentSteps.checkDynamicHasNoShopWithPrepayment(SHOP_ID);
    }
}
