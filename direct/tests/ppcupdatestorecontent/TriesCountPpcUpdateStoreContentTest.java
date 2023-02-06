package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import org.hamcrest.Matcher;
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

import java.sql.Timestamp;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 20/08/15.
 * https://st.yandex-team.ru/TESTIRT-7158
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_UPDATE_STORE_CONTENT)
@Issue("https://st.yandex-team.ru/DIRECT-45614")
@Description("ppcUpdateStoreContent.pl обновление tries_count")
@RunWith(Parameterized.class)
public class TriesCountPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    private static final long TRIES_COUNT_LIMIT = 144l;

    @Override
    protected String getLogin() {
        return "at-direct-upd-store-tries";
    }

    private String initialAppId;

    @Parameterized.Parameter(0)
    public Long initialTriesCount;

    @Parameterized.Parameter(1)
    public Long expectedTriesCount;

    @Parameterized.Parameter(2)
    public Matcher<Long> modifyTimeMatcher;

    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0L, 1L, not(equalTo(0L))}
                , {TRIES_COUNT_LIMIT - 1, TRIES_COUNT_LIMIT, not(equalTo(0L))}
                , {TRIES_COUNT_LIMIT, TRIES_COUNT_LIMIT, equalTo(0L)}
                , {TRIES_COUNT_LIMIT + 1, TRIES_COUNT_LIMIT + 1, equalTo(0L)}
        });
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
        mobileContent.setOsType(MobileContentOsType.iOS);
        mobileContent.setMinOsVersion("5.1");
        mobileContent.setIsAvailable(0);
        mobileContent.setTriesCount(initialTriesCount);
        initialAppId = mobileContent.getStoreContentId();
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies
                , mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setSupportedDevices(null);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setAppId("not.initial.app.id");
        return bean;
    }

    @Test
    public void test() {
        Timestamp initialModifyTime = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId).getModifyTime();
        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);
        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);
        Timestamp modifyTime = mobileContent.getModifyTime();
        assertThat("поле tries_count заполнилось верно", mobileContent.getTriesCount(), equalTo(expectedTriesCount));
        assertThat("поле modify_time заполнилось верно",
                modifyTime.toInstant().toEpochMilli() - initialModifyTime.toInstant().toEpochMilli(),
                modifyTimeMatcher);
        assertThat("поле store_content_id не изменилось", mobileContent.getStoreContentId(), equalTo(initialAppId));
    }
}
