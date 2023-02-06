package ru.yandex.autotests.market.billing.backend.vendorDataExecutor;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.vendor.VendorData;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.vendor.VendorDataField;
import ru.yandex.autotests.market.billing.backend.steps.VendorParametersSteps;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.ArrayList;
import java.util.Collection;

import static ru.yandex.autotests.market.billing.backend.core.dao.entities.vendor.VendorDataField.HAS_OPEN_CUTOFF_CPA;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.vendor.VendorDataField.HAS_OPEN_CUTOFF_CPC;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.vendor.VendorDataField.SHOP_NAME;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.vendor.VendorDataField.SHOP_URL;
import static ru.yandex.autotests.market.billing.backend.steps.VendorParametersSteps.hasOpenCutoffCpaMatcher;
import static ru.yandex.autotests.market.billing.backend.steps.VendorParametersSteps.hasOpenCutoffCpcMatcher;
import static ru.yandex.autotests.market.billing.backend.steps.VendorParametersSteps.shopNameMatcher;
import static ru.yandex.autotests.market.billing.backend.steps.VendorParametersSteps.shopUrlMatcher;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@Aqua.Test(title = "Тест параметров выгрузки vendorDataExecutor")
@Feature("vendorData")
@Description("")
@RunWith(Parameterized.class)
public class VendorDataParametersTest {

    private VendorParametersSteps vendorParametersSteps = VendorParametersSteps.getInstance();

    public static VendorParametersSteps.VendorFileDao dao;

    @Parameterized.Parameter
    public static VendorDataField field;

    @Parameterized.Parameter(1)
    public static Matcher<VendorData> matcher;

    @Parameterized.Parameters(name= "{index}: проверка параметра {1}")
    public static Collection<Object[]> data() throws Exception {
        dao = VendorParametersSteps.getVendorFileDao();
        return new ArrayList<Object[]>() {{
            add(new Object[]{SHOP_NAME, shopNameMatcher()});
            add(new Object[]{SHOP_URL, shopUrlMatcher()});
            add(new Object[]{HAS_OPEN_CUTOFF_CPC, hasOpenCutoffCpcMatcher()});
            add(new Object[]{HAS_OPEN_CUTOFF_CPA, hasOpenCutoffCpaMatcher()});
        }};
    }

    @Test
    public void testParameter() {
        vendorParametersSteps.checkParameterForShops(field, dao, matcher);
    }
}

