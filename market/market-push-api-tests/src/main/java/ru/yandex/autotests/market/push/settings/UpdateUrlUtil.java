package ru.yandex.autotests.market.push.settings;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.common.request.log.AllureRestAssuredLogger;
import ru.yandex.autotests.common.request.rest.HttpRequestSteps;
import ru.yandex.autotests.market.pushapi.ProjectConfig;
import ru.yandex.autotests.market.push.api.beans.request.settings.Settings;
import ru.yandex.autotests.market.pushapi.dao.PushApiBillingDao;
import ru.yandex.autotests.market.pushapi.steps.SettingsSteps;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;

import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.beans.PushApiDataUtils.getEnclosingClassName;
import static ru.yandex.autotests.market.pushapi.steps.SettingsSteps.changeFingerprint;
import static ru.yandex.autotests.market.pushapi.request.PushApiTestCase.createCases;
import static ru.yandex.autotests.market.pushapi.utils.ApiDateFormatter.getNowSimpleTime;

@Feature("Update settings")
@Aqua.Test(title = "Обновить url, fingerprint, shopType ,!!!!!!!НЕ ДЛЯ РЕГРЕССА!!!!!!")
@RunWith(Parameterized.class)
@Issues({
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1185"),
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1841")})
public class UpdateUrlUtil {
    private transient static final Logger LOG = LogManager.getLogger(UpdateUrlUtil.class);
    private static String shopType;

    private HttpRequestSteps resource = new HttpRequestSteps(new AllureRestAssuredLogger());
    public PushApiBillingDao storage = PushApiBillingDao.getInstance();
    public SettingsSteps settingsSteps = new SettingsSteps();
    private static ProjectConfig projectConfig = new ProjectConfig();

    @Parameterized.Parameter(0)
    public String shopIdStr;
    @Parameterized.Parameter(1)
    public String shopBaseUrl;
    private static String fingerprint;
    private String requestId;
    private long shopId;
    private boolean isMultitesting;


    @Parameterized.Parameters(name = "Update settings for shopId {0}  to shopBaseUrl {1}")
    public static Collection<Object[]> data() {

        String virtualHost = projectConfig.getShopBaseUrl();
        fingerprint = projectConfig.getFingerprint();
        shopType = projectConfig.getShopType(); //  указывать с выключеной allWikilist , для конкретного магазина иначе всем поменяется
        boolean allWikilist = projectConfig.isAllWikilist();
        String shopList = projectConfig.getShopList();
        return createCases(virtualHost, null, shopList, allWikilist);
    }

    @Before
    public void genRequestId() {
        requestId = "Change from push-api test "+ getEnclosingClassName(1) + "  " + getNowSimpleTime();
        shopId = Integer.parseInt(shopIdStr);
        isMultitesting = projectConfig.isInJenkins();
    }


    @Test
    public void setNewUrl() {
        if (shopId == 774)
            return;

        Settings settingsByShopId = settingsSteps.getSettingsByShopId(shopId);

        LOG.info("Shop for Update = " + shopId);
        String url = settingsSteps.updateUrl(shopId, settingsByShopId.getUrl(), shopBaseUrl, shopType);
        settingsByShopId.setUrl(url);

        settingsSteps.postSettings(shopId, settingsByShopId, requestId);

        if (!isMultitesting) {
            storage.updateShopUrl(shopId, url);
        }

    }

    @Test
    public void setNewFingerPrint() {
        if (fingerprint == null || shopId == 774)
            return;

        Settings settingsByShopId = settingsSteps.getSettingsByShopId(shopId);

        LOG.info("Shop for Update = " + shopId);
        changeFingerprint(shopId, settingsByShopId, fingerprint);

        settingsSteps.postSettings(shopId, settingsByShopId, requestId);

        if (!isMultitesting) {
            storage.updateShopFingerprint(shopId, fingerprint);
        }
    }
}
