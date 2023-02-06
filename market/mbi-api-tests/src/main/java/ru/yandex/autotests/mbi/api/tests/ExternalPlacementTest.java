package ru.yandex.autotests.mbi.api.tests;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.steps.ClicksGenerationSteps;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.mbi.api.beans.ExternalPlacement;
import ru.yandex.autotests.market.stat.mstgetter.steps.mstgetter.ClicksGeneratorConfig;
import ru.yandex.autotests.mbi.api.steps.ExternalPlacementSteps;
import ru.yandex.autotests.mbi.api.utils.MbiApiRequestFactory;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThat;

/**
 * @author Vadim Lyalin
 */
@Aqua.Test(title = "Проверка ручки /external-placement")
@Feature("External placement")
@Stories("Проверка ручки /external-placement")
public class ExternalPlacementTest {
    /**
     * Ручка получает клики только для партнеров с кодом больше 1000.
     */
    private static final int MIN_CLID = 1000;
    private final ExternalPlacementSteps externalPlacementSteps = new ExternalPlacementSteps();

    private ClicksGeneratorConfig config = new ClicksGeneratorConfig();
    {
        config.setPp("1000");
        config.setShopId("243245");
        config.setPrice("22");
        config.setDistrType("1");
        config.setClid(randomClid());
    }
    private ExternalPlacement expectedExternalPlacement =
            new ExternalPlacement(
                    config.getClid(),
                    "0",
                    MbiApiRequestFactory.REQUEST_DATE_FORMAT.format(new Date()),
                    "1", // на стороне бекенда явно синхронизировано max(shows,clicks)
                    "1",
                    "0.22",
                    "22",
                    "0"
            );

    @Test
    @Title("Генерим клик и получаем его в выдаче /external-placement")
    public void testExternalPlacement() {
        Attacher.attach(config);
        ClicksGenerationSteps.getInstance().generateLbClicksAndWait(1, 0, config);
        Date today = new Date();
        List<ExternalPlacement> externalPlacements = externalPlacementSteps.getExternalPlacement(today, today, config.getDistrType(),
                config.getClid());
        assertThat(externalPlacements, hasSize(equalTo(1)));
        assertThat(externalPlacements.get(0), samePropertyValuesAs(expectedExternalPlacement));
    }

    private String randomClid() {
        return Integer.toString(new Random().nextInt(10000) + MIN_CLID);
    }
}
