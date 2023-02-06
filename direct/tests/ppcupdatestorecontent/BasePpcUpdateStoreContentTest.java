package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.FakeBSProxyLogBean;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.UpdateStoreContentMobileResponseBean;
import ru.yandex.autotests.direct.fakebsproxy.dao.FakeBSProxyLogBeanMongoHelper;
import ru.yandex.autotests.directapi.darkside.model.MobileContentUtils;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.CountryCurrencies;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.allure.annotations.Step;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static ru.yandex.autotests.directapi.darkside.model.MobileContentUtils.getPricesJsonFor;
import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;

/**
 * Created by buhter on 18/08/15.
 */
public abstract class BasePpcUpdateStoreContentTest {
    private static final FakeBSProxyLogBeanMongoHelper HELPER = new FakeBSProxyLogBeanMongoHelper();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.SUPER_LOGIN);

    @Rule
    public Trashman trashman = new Trashman(api);

    protected static String login;
    protected int shard;
    protected MobileContentRecord mobileContent;
    protected Long mobContentId;

    protected abstract UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent);

    protected abstract void updateMobileContent(MobileContentRecord mobileContent);

    protected abstract String getLogin();

    protected void initLogin() {
        login = getLogin();
        shard = api.userSteps.clientFakeSteps().getUserShard(login);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void before() {
        initLogin();
        mobileContent = MobileContentUtils.getDefaultMobileContent(User.get(login).getClientID());
        mobContentId = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().saveMobileContent(mobileContent);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        mobileContent.setModifyTime(Timestamp.from(Instant.now().minus(1, DAYS).minus(1, HOURS)));
        mobileContent.setPricesJson(getPricesJsonFor(CountryCurrencies.values()));
        updateMobileContent(mobileContent);
        api.userSteps.getDirectJooqDbSteps().useShard(shard).mobileContentSteps().updateMobileContent(mobileContent);

        for (CountryCurrencies countryCurrencies : CountryCurrencies.values()) {
            FakeBSProxyLogBean logBean = new FakeBSProxyLogBean().withObjectIds(
                    Collections.singletonList(
                            mobileContent.getStoreContentId() + ":" + countryCurrencies.requestDataCountry())
            );
            HELPER.saveFakeBSProxyLogBean(logBean.withResponseEntity(
                    getResponseBean(countryCurrencies, mobileContent).toString())
            );
            addJsonAttachment("Ответ ручки для страны " + countryCurrencies.country(), logBean.getResponseEntity());
        }
    }

    @After
    public void after() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).mobileContentSteps().deleteMobileContent(mobileContent);
        for (CountryCurrencies countryCurrencies : CountryCurrencies.values()) {
            HELPER.deleteFakeBSProxyLogBeansById(
                    mobileContent.getStoreContentId() + ":" + countryCurrencies.requestDataCountry());
        }
    }

}
