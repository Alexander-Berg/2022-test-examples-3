package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.UpdateStoreContentMobileResponseBean;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.CountryCurrencies;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.StoreContentHelper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.math.BigInteger;
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
@Description("ppcUpdateStoreContent.pl обновление rating_votes")
@RunWith(Parameterized.class)
public class RatingVotesPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    @Override
    protected String getLogin() {
        return "at-direct-upd-store-votes";
    }

    @Parameterized.Parameter(0)
    public Long ratingVotes;

    @Parameterized.Parameters(name = "rating_votes = {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {12345678l}
                , {0l}
        });
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setRatingCount(ratingVotes.toString());
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

        assertThat("поле rating_votes заполнилось верно", mobileContent.getRatingVotes(), equalTo(ratingVotes));
    }
}
