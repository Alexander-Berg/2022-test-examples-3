package ru.yandex.autotests.market.billing.backend.getClicksExecutor;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 10/31/14
 */

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole;
import ru.yandex.autotests.market.billing.backend.steps.TmsSteps;
import ru.yandex.autotests.market.stat.beans.tables.Table;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.ArrayList;
import java.util.Collection;

import static ru.yandex.autotests.market.billing.backend.utils.mapper.map.marketstat.MstToBillingMappers.createBillingClickToMstClickMapping;
import static ru.yandex.autotests.market.billing.backend.utils.mapper.map.marketstat.MstToBillingMappers.createMstClickItselfMapping;

@Aqua.Test(title = "Проверка соответствия данных api маркетстата и базы билинга для кликов")
@Feature("clicks")
@Description("Подробное описание теста https://st.yandex-team.ru/TESTMARKET-1604")
@RunWith(Parameterized.class)
public class ClicksCaptureTest extends AbstractClicksCaptureTest {

    private static TmsSteps tmsSteps = new TmsSteps();

    @Before
    public void setUp() {
        tmsSteps.checkLastJobSuccess(MarketBillingConsole.GET_CLICKS_EXECUTOR);
        tmsSteps.checkLastJobSuccess(MarketBillingConsole.GET_ROLLBACKS_EXECUTOR);
    }

    @Parameterized.Parameters(name = "Проверка соотвествия для таблицы {0}")
    public static Collection<Object[]> testData() {
        return new ArrayList<Object[]>() {{
            add(new Object[]{Table.Raw.CLICKS, createBillingClickToMstClickMapping(false), createMstClickItselfMapping(false)});
            add(new Object[]{Table.Raw.CLICKS_ROLLBACKS, createBillingClickToMstClickMapping(true), createMstClickItselfMapping(true)});
        }};
    }


}
