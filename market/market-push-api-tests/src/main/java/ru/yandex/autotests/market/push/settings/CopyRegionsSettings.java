package ru.yandex.autotests.market.push.settings;

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
import ru.yandex.autotests.market.push.api.beans.request.regions.PostRegionsRequestBody;
import ru.yandex.autotests.market.push.api.beans.request.regions.Regions;
import ru.yandex.autotests.market.pushapi.data.ShopAdminStubRequestData;
import ru.yandex.autotests.market.pushapi.steps.SettingsSteps;
import ru.yandex.qatools.allure.annotations.Features;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.openqa.jetty.http.HttpResponse.__200_OK;
import static ru.yandex.autotests.market.pushapi.data.bodies.CartRequestProvider.formatRequestAsXML;
import static ru.yandex.autotests.market.pushapi.request.PushApiTestCase.createCases;
import static ru.yandex.autotests.market.pushapi.utils.TestDataUtils.convertArray;


/**
 * Created by strangelet on 24.09.15.
 */
@RunWith(Parameterized.class)
@Aqua.Test(title = "Копирование  регионов  маказинов на мультитестинг")
@Features("Copy")
public class CopyRegionsSettings {

    private transient static final Logger LOG = LogManager.getLogger(UpdateUrlUtil.class);
    private HttpRequestSteps resource = new HttpRequestSteps(new AllureRestAssuredLogger(), new DefaultRestAssuredLogger());
    private static final ProjectConfig projectConfig = new ProjectConfig();
    private SettingsSteps settingsSteps = new SettingsSteps();


    @Parameterized.Parameter(0)
    public String shopId;
    @Parameterized.Parameter(1)
    public String virtualHost;

    @Parameterized.Parameters(name = "Copy Regions for shopId {0}  to host {1}")
    public static Collection<Object[]> data() {
        String virtualHost = projectConfig.getVirtualHost();
        Integer virtualPort = projectConfig.getRegionsVirtualPort();
        String shopList = projectConfig.getShopList();
        boolean allWikilist = projectConfig.isAllWikilist();
        return createCases(virtualHost, virtualPort, shopList,allWikilist );
    }




    @Test
    public void copyRegions() {
        assertThat("Exclude fake shop 246002", shopId, not(equalTo("246002")));

        LOG.info("Copy Regions for shop = " + shopId + " to " + virtualHost);
        long shopIdInt = 0;
        try {
            shopIdInt = Integer.parseInt(shopId.trim());
        } catch (NumberFormatException e) {
            throw new AssertionError("shopId is not a Number (" + shopId + ")" + e.getMessage());
        }
        String regionResponse = resource.getResponseBodyAsString(ShopAdminStubRequestData.getGravicapaRegionsRequest(shopIdInt));
        settingsSteps.checkRegionsResponse(regionResponse, shopIdInt);

        String[] regions = regionResponse.substring(1, regionResponse.length() - 1).split(", ");
        Integer[] regionsInt = convertArray(regions, Integer::parseInt, Integer[]::new);

        PostRegionsRequestBody bodyForRequest = new PostRegionsRequestBody().withId(shopIdInt).withRegions(new Regions().withRegions(regionsInt));
        String body = formatRequestAsXML(bodyForRequest);
        LOG.info("body = " + body);

        Response response = resource.getResponse(ShopAdminStubRequestData.postRegionsRequest(virtualHost, shopIdInt, body));
        assertThat("Error during POST regions for shop (" + shopIdInt + ") to " + virtualHost + " " + response.toString(), response.getStatusCode(),
                is(__200_OK));

        String settingsInNewHost = resource.getResponseBodyAsString(ShopAdminStubRequestData.getRegionsRequest(virtualHost, shopIdInt));
        settingsSteps.checkRegionsResponse(settingsInNewHost, shopIdInt);

    }



}
