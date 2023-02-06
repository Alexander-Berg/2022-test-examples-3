package ru.yandex.autotests.market.checkouter.api.multitesting;

import com.jayway.restassured.response.Response;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.common.request.log.AllureRestAssuredLogger;
import ru.yandex.autotests.common.request.log.DefaultRestAssuredLogger;
import ru.yandex.autotests.common.request.rest.HttpRequestSteps;
import ru.yandex.autotests.market.pushapi.ProjectConfig;
import ru.yandex.autotests.market.push.api.beans.request.settings.Settings;
import ru.yandex.autotests.market.pushapi.data.SettingsRequestData;
import ru.yandex.autotests.market.pushapi.steps.SettingsSteps;
import ru.yandex.qatools.allure.annotations.Features;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThat;
import static org.openqa.jetty.http.HttpResponse.__200_OK;
import static ru.yandex.autotests.market.pushapi.beans.PushApiDataUtils.getEnclosingClassName;
import static ru.yandex.autotests.market.pushapi.request.PushApiTestCase.createCases;
import static ru.yandex.autotests.market.pushapi.utils.ApiDateFormatter.getNowSimpleTime;


/**
 * Created by strangelet on 24.09.15.
 */
@RunWith(Parameterized.class)
@Aqua.Test(title = "Копирование настроек чекаута магазинов на мультитестинг")
@Features("Copy")
public class CopyPushApiSettings {

    private transient static final Logger LOG = LogManager.getLogger(CopyPushApiSettings.class);
    private HttpRequestSteps resource = new HttpRequestSteps(new AllureRestAssuredLogger(), new DefaultRestAssuredLogger());
    private static final ProjectConfig projectConfig = new ProjectConfig();
    private SettingsSteps settingsSteps = new SettingsSteps();

    @Parameterized.Parameter(0)
    public String shopId;
    @Parameterized.Parameter(1)
    public String virtualBaseUrl;
    private static String virtualHost;
    private static String shopBaseUrl;


    @Parameterized.Parameters(name = "Copy settings for shopId {0}  to host {1}")
    public static Collection<Object[]> data() {
        virtualHost = projectConfig.getVirtualHost();
        shopBaseUrl = projectConfig.getShopBaseUrl();
        Integer virtualPort = projectConfig.getSettingsVirtualPort();
        String shopList = projectConfig.getShopList();
        boolean allWikilist = projectConfig.isAllWikilist();
        return createCases(virtualHost, virtualPort, shopList, allWikilist);
    }

    @Test
    public void copySettings() {
        assertThat("Exclude fake shop 246002", shopId, not(equalTo("246002")));
        LOG.info("Copy settings for shop = " + shopId + " to " + virtualBaseUrl);
        long shopIdInt = 0;
        try {
            shopIdInt = Long.parseLong(shopId.trim());
        } catch (NumberFormatException e) {
            throw new AssertionError("shopId is not a Number (" + shopId + ")" + e.getMessage());
        }

        Settings settings = settingsSteps.getSettingsByShopId(shopIdInt, projectConfig.getGravicapaURL());
        if (settings.getUrl() != null) {
            String url = settingsSteps.updateUrl(shopIdInt, settings.getUrl(), shopBaseUrl);
            settings.setUrl(url);
        }
        settings.setChangerId("changed in " + getEnclosingClassName(1));

        String requestId = "Change from push-api test " + getEnclosingClassName(1)
                + "  for host=" + virtualBaseUrl + " " + getNowSimpleTime();

        Response response = resource.getResponse(
                SettingsRequestData.postSettingRequestForHostAndShop(virtualBaseUrl, shopIdInt, settings, requestId));
        assertThat("Error during POST settings for shop (" + shopIdInt + ") to " + virtualBaseUrl, response.getStatusCode(),
                is(__200_OK));
        String settingsInNewHost = resource.getResponseBodyAsString(
                SettingsRequestData.getSettingRequestForHostAndShop(virtualBaseUrl, shopIdInt));
        settingsSteps.checkSettingsResponse(settingsInNewHost, shopIdInt);

    }


}
