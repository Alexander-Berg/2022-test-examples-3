package ru.yandex.autotests.market.checkouter.api.multitesting;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.checkouter.api.rule.ShopIdsProvider;
import ru.yandex.autotests.market.checkouter.api.steps.ShopsSteps;
import ru.yandex.autotests.market.checkouter.beans.shops.Shops;
import ru.yandex.autotests.market.checkouter.utils.CheckouterUtilsConfig;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.market.pushapi.beans.PushApiDataUtils.getEnclosingClassName;
import static ru.yandex.autotests.market.pushapi.utils.ApiDateFormatter.getNowSimpleTime;

/**
 * Created by strangelet on 02.11.15.
 */
@Aqua.Test(title = "Копировать настройки/shops магазинов из списка на вики  на ноду мультитестинга !!!НЕ для регресса!!!")
@Features("Shops")
@RunWith(Parameterized.class)
@Issues({
        @Issue("AUTOTESTMARKET-1318"),
})
public class ShopsDataCopyUtils {

    @Parameterized.Parameter(0)
    public Long testShopId;
    ShopsSteps shopsSteps = new ShopsSteps();

    @Parameterized.Parameters(name = "Copy  /shops/{0}")
    public static List<Long[]> data() {
        return ShopIdsProvider.getTestShopIds();
    }


    @Test
    public void update() {
        CheckouterUtilsConfig config = new CheckouterUtilsConfig();
        String baseUrl = config.getGravicapaUrl();
        String nodeUrl = config.getBaseUrl();
        Shops getShops = shopsSteps.getShopData(testShopId, baseUrl);
        String requestId = "Change from Checkouter  " + getEnclosingClassName(2) + "  "
                + "for nodeUrl " + nodeUrl + "  " + getNowSimpleTime();

        Shops shops1 = shopsSteps.putShopsData(testShopId, getShops, "copy settings from BT");
        assertThat("Error during PUT shops data" + shops1, shops1.getCode(), nullValue());
    }

}