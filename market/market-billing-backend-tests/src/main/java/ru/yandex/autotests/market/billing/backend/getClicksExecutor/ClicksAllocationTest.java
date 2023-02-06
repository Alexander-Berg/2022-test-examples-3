package ru.yandex.autotests.market.billing.backend.getClicksExecutor;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 11/21/14
 */

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole;
import ru.yandex.autotests.market.billing.backend.steps.GetClicksExecutorSteps;
import ru.yandex.autotests.market.billing.backend.steps.TmsSteps;
import ru.yandex.autotests.market.stat.beans.tables.MstGetterTable;
import ru.yandex.autotests.market.stat.beans.tables.Table;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.ArrayList;
import java.util.Collection;

@Aqua.Test(title = "Проверка раскладки сырых кликов clicks, clicks_detailed, png_clicks_2")
@Feature("clicks")
@Description("Подробное описание теста https://st.yandex-team.ru/TESTMARKET-1625")
@RunWith(Parameterized.class)
public class ClicksAllocationTest {

    private static TmsSteps tmsSteps = new TmsSteps();

    @Parameterized.Parameters(name = "Проверка раскладки для таблицы {0}")
    public static Collection<Object[]> testData() {
        return new ArrayList<Object[]>() {{
            add(new Object[]{Table.Raw.CLICKS});
            add(new Object[]{Table.Raw.CLICKS_ROLLBACKS});
        }};
    }

    @Parameterized.Parameter
    public static MstGetterTable table;

    GetClicksExecutorSteps steps = new GetClicksExecutorSteps();

    @Before
    public void setUp() {
        tmsSteps.checkLastJobSuccess(MarketBillingConsole.GET_CLICKS_EXECUTOR);
        tmsSteps.checkLastJobSuccess(MarketBillingConsole.GET_ROLLBACKS_EXECUTOR);
    }

    @Test
    public void allocationClicksTest() {
        steps.checkRawClicksAllocationIntoClicks2Table(table);
    }

    @Test
    public void allocationClicksWithSbidTest() {
        steps.checkRawClicksWithSbidAllocationIntoClicks2Table(table);
    }

    @Test
    public void allocationClicksDetailedTest() {
        steps.checkRawClickAllocationIntoClicksDetailed2Table(table);
    }

    @Test
    public void allocationPNGClicksTest() {
        steps.checkRawClickAllocationIntoPNGClicks2Table(table);
    }
}
