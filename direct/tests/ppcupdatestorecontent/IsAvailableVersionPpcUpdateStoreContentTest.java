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
@Description("ppcUpdateStoreContent.pl обновление is_available")
@RunWith(Parameterized.class)
public class IsAvailableVersionPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    @Override
    protected String getLogin() {
        return "at-direct-upd-store-available";
    }

    @Parameterized.Parameter(0)
    public Integer initialIsAvailable;

    @Parameterized.Parameter(1)
    public MobileContentOsType osType;

    @Parameterized.Parameter(2)
    public String bundleId;

    @Parameterized.Parameter(3)
    public String supportedDevices;

    @Parameterized.Parameter(4)
    public Integer expectedIsAvailable;

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, MobileContentOsType.Android, "ru.yandex.autotests.bundle", null, 1}
                , {0, MobileContentOsType.iOS, null, "[\"iPhone6Plus\"]", 1}
                , {1, MobileContentOsType.iOS, null, "[\"iPhone6Plus\"]", 1}
                , {1, MobileContentOsType.iOS, "ru.yandex.autotests.bundle", null, 0}
        });
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
        mobileContent.setOsType(osType);
        mobileContent.setMinOsVersion("5.1");
        mobileContent.setIsAvailable(initialIsAvailable);
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setBundle(bundleId);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setSupportedDevices(supportedDevices);

        return bean;
    }

    @Test
    public void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        assertThat("поле is_available заполнилось верно"
                , mobileContent.getIsAvailable()
                , equalTo(expectedIsAvailable));
    }
}
