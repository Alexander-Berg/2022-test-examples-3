package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.hamcrest.Matcher;
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

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pashkus on 01.04.16.
 * https://st.yandex-team.ru/TESTIRT-8821
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_UPDATE_STORE_CONTENT)
@Issue("https://st.yandex-team.ru/DIRECT-51665 ")
@Description("ppcUpdateStoreContent.pl:  Валидация publisher_domain_id")
@RunWith(Parameterized.class)
public class PublisherDomainInPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    @Override
    protected String getLogin() {
        return "at-direct-upd-publisher-domain";
    }

    @Parameterized.Parameter(0)
    public String testWebsite;

    @Parameterized.Parameter(1)
    public Matcher expectedPublisherDomainId;

    @Parameterized.Parameters(name = "Проверяем валидацию доменного имени. В ответе ручки website={0}; Ожидаем в publisher_domain_id = {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"http://domain", nullValue()}     // неправильный домен
                , {"http://%D1%82%D0%B0%D0%BA%D1%81%D0%B8%D0%B1%D0%BE%D0%BD%D1%83%D1%81.%D1%80%D1%84", nullValue()}    //url coded
                , {"таксибонус.рф", notNullValue()}    //кириллический домен
                , {"www.zeptolab.com", notNullValue()} //латиница
        });
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies, mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setWebsite(testWebsite);
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

        assertThat("поле publisher_domain_id заполнилось верно", mobileContent.getPublisherDomainId(), expectedPublisherDomainId);
    }
}
