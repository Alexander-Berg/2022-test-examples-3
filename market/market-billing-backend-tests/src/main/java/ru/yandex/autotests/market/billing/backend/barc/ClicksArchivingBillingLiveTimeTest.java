package ru.yandex.autotests.market.billing.backend.barc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.steps.archiver.ArchivingSteps;
import ru.yandex.autotests.market.billing.backend.utils.mapper.map.barc.BarcClickToBillingMap;
import ru.yandex.qatools.allure.annotations.Description;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 5/8/15
 */
@Aqua.Test(title = "Тест на архивацию кликов и откатов в hadoop. Проверка срока хранения.")
@Feature("barc")
@Description("Описание теста тут https://st.yandex-team.ru/AUTOTESTMARKET-744")
@RunWith(Parameterized.class)
public class ClicksArchivingBillingLiveTimeTest {

    public static final int BILLING_LIFETIME_IN_MONTH = 1;

    //время, после которого все клики должны быть заархивированы и удалены из билинга
    public static final long DT = 150222L;

    @Parameterized.Parameters(name = "Проверка для {0}")
    public static Collection<Object[]> data() {
        return new ArrayList<Object[]>() {{
            add(new Object[]{BarcClickToBillingMap.LOG_CLICK});
            add(new Object[]{BarcClickToBillingMap.CLICK_ROLLBACK});
        }};
    }

    @Parameterized.Parameter
    public BarcClickToBillingMap table;

    private ArchivingSteps archivingSteps = ArchivingSteps.getInstance();

    //TODO unignore when implemented without p_clicks
    @Test
    public void checkBillingDataRetentionTest() throws IOException {
        archivingSteps.checkBillingDataRetention(BILLING_LIFETIME_IN_MONTH, DT, table);
    }
}
