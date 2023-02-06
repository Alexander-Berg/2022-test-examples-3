package ru.yandex.autotests.market.billing.backend.barc;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleFactory;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ArchivingSteps;
import ru.yandex.autotests.market.billing.backend.steps.BillingDaoSteps;
import ru.yandex.autotests.market.billing.backend.utils.mapper.map.barc.BarcToBillingMap;
import ru.yandex.qatools.allure.annotations.Description;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 9/2/15
 */
@Aqua.Test(title = "Тест tms задачи dropPartitionsExecutor ")
@Feature("barc")
@Description("" +
        "Этот тест является дополнением ко всем тестам на архивацию. " +
        "Запускать его нужно только после полного завершения тестов на архивацию. " +
        "Суть теста - в проверке тмс задачи dropPartitionsExecutor за один её вызов. Связано это с долгим выполнением" +
        "данной задачи. Для тестов на архивацию создаются тестовые данные (путём генерации через внение системы " +
        "или прямым копированием данных в пределах одной таблицы), они же архивируются тестами архивации. После этого " +
        "партиции с заархивированными данными попадают в очередь на удаление. От куда удаляются задачей DropPartitionsExecutor." +
        "" +
        "Поэтому в тест сначала собирает информацию о числе партиций в очереди, потом запускает однократно задачу на удаление, " +
        "и после проверяется очередь на пустоту")
@RunWith(Parameterized.class)
public class DropPartitionsExecutorTest {
    @Parameterized.Parameter
    public BarcToBillingMap table;

    @Parameterized.Parameter(1)
    public Long dt;

    @Parameterized.Parameter(2)
    public Integer partitionToDropCount;

    @Parameterized.Parameters(name = "Проверка для {0}")
    public static Collection<Object[]> data() {
        // TODO: test drop?
        return new ArrayList<>();
    }

    private static Object[] fillParameters(BarcToBillingMap table, Long dt) {
        Integer tableToDropCount = BillingDaoSteps.getInstance().getTableToDropCount(table, dt);
        return new Object[]{table, dt, tableToDropCount};
    }

    @BeforeClass
    public static void setUp() throws Exception {
        MarketBillingConsoleFactory.connectToArchiver().runDropPartitionsExecutor();
    }

    @Test
    public void deletePartitionTest() throws IOException {
        ArchivingSteps.getInstance().checkPartitionToDrop(table, dt, partitionToDropCount);
    }
}
