package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.math.BigInteger;

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
@Description("ppcUpdateStoreContent.pl обновление app_size_bytes")
public class AppSizePpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    public static final Long APP_SIZE_BYTES = 31337000L;

    @Override
    protected String getLogin() {
        return "at-direct-upd-store-size";
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setFileSize(APP_SIZE_BYTES.toString());
        return bean;
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
    }

    @Test
    public void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        assertThat("поле app_size_bytes заполнилось верно", mobileContent.getAppSizeBytes(), equalTo(APP_SIZE_BYTES));
    }
}

