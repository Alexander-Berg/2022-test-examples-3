package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.MobileContentOsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.UpdateStoreContentMobileResponseBean;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.CountryCurrencies;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.StoreContentHelper;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 20/08/15.
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_UPDATE_STORE_CONTENT)
@Issue("https://st.yandex-team.ru/TESTIRT-6732")
@Description("ppcUpdateStoreContent.pl обновление min_os_version")
@RunWith(Parameterized.class)
public class MinOsVersionPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    @Override
    protected String getLogin() {
        return "at-direct-upd-store-min-os";
    }

    @Parameterized.Parameter(0)
    public MobileContentOsType osType;

    @Parameterized.Parameter(1)
    public String minOsVersion;

    @Parameterized.Parameter(2)
    public String expectedMinOsVersion;

    @Parameterized.Parameters(name = "OS = {0}, min_os_version = {1}, expected = {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MobileContentOsType.Android, "2.3.5 или выше", "2.3"}
                , {MobileContentOsType.Android, "любая", ""}
                , {MobileContentOsType.iOS, "7.1 или что-то около", "7.1"}
                , {MobileContentOsType.iOS, "любая", ""}
        });
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
        mobileContent.setOsType(osType);
        mobileContent.setMinOsVersion("5.1");
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setOsVersion(minOsVersion);
        return bean;
    }

    @Test
    public void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        assertThat("поле min_os_version заполнилось верно"
                , mobileContent.getMinOsVersion()
                , equalTo(expectedMinOsVersion));
    }
}
