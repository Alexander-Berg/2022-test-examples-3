package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.MobileContentOsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.MobileContentStatusiconmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
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
@Description("ppcUpdateStoreContent.pl обновление icon_hash")
public class IconPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    @Override
    protected String getLogin() {
        return "at-direct-upd-store-icon";
    }

    private static final String ICON_HASH = "31337/bef1ec042fac33ce24ac0b5dadd60a98";

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setIcon("http://avatars.mds.yandex.net/itunes-icon/" + ICON_HASH + " /orig");
        return bean;
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
        mobileContent.setOsType(MobileContentOsType.iOS);
        mobileContent.setMinOsVersion(OSVersions.IOS_VERSIONS[5]);
        mobileContent.setStatusiconmoderate(MobileContentStatusiconmoderate.Yes);
    }

    @Test
    public void test() {
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        assertThat("поле icon_hash заполнилось верно", mobileContent.getIconHash(), equalTo(ICON_HASH));
        assertThat("статус модерации иконки сбросился", mobileContent.getStatusiconmoderate(),
                equalTo(MobileContentStatusiconmoderate.Ready));
    }
}

