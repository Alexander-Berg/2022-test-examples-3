package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.MobileContentOsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.MobileContentStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.Properties;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.UpdateStoreContentMobileResponseBean;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.CountryCurrencies;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.OSVersions;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.StoreContentHelper;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.Arrays;
import java.util.Map;

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
@Description("ppcUpdateStoreContent.pl обновление statusBsSynced")
@RunWith(Parameterized.class)
public class StatusBsSyncedPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {

    @Override
    protected String getLogin() {
        return "at-transport-tester-7";
    }

    private static final Gson GSON = new Gson();

    @Parameterized.Parameter(0)
    public String field;

    @Parameterized.Parameter(1)
    public String value;

    @Parameterized.Parameters(name = "rating_votes = {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Properties.BUNDLE, "ru.yandex.autotests.new.bundle"}
                , {Properties.PRICE, "1.1"}
                , {Properties.OS_VERSION, "1.0"}
                , {Properties.ADULT, "Teen"}
                , {Properties.RATING, "3.1337"}
                , {Properties.RATING_COUNT, "31337"}
                , {Properties.SUPPORTED_DEVICES, ""}
        });
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        Map<String, String> propertiesMap = GSON.fromJson(GSON.toJson(bean.getResponse().getResults()
                .get(0).getGroups().get(0).getDocuments().get(0).getProperties()), Map.class);
        propertiesMap.put(field, value);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0)
                .setProperties(GSON.fromJson(GSON.toJson(propertiesMap), Properties.class));
        return bean;
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
        mobileContent.setOsType(MobileContentOsType.iOS);
        mobileContent.setMinOsVersion(OSVersions.IOS_VERSIONS[5]);
        mobileContent.setStatusbssynced(MobileContentStatusbssynced.Yes);
    }

    @Test
    public void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        assertThat("поле statusBsSynced заполнилось верно", mobileContent.getStatusbssynced(),
                equalTo(MobileContentStatusbssynced.No));
    }
}
