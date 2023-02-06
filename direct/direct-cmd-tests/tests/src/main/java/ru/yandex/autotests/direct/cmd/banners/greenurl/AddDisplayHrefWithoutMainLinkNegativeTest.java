package ru.yandex.autotests.direct.cmd.banners.greenurl;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Валидация при добавлении отображаемой ссылки в баннер без ссылки на сайт")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class AddDisplayHrefWithoutMainLinkNegativeTest extends DisplayHrefBaseTest {

    public AddDisplayHrefWithoutMainLinkNegativeTest() {
        ContactInfo contactInfo = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
        bannersRule = new TextBannersRule().withUlogin(CLIENT);
        bannersRule.getBanner().
                withContactInfo(contactInfo).
                withHref("").
                withHasHref(0d).
                withHasVcard(1).
                withIsVcardOpen(1d).
                withHrefModel(null).
                withDomain(null).
                withDomainRedir(null).
                withDomainSign(null).
                withDomainRedirSign(null);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Валидация при добавлении отображаемой ссылки в баннер без ссылки на сайт")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9192")
    public void testAddDisplayHrefWithoutMainLinkNegative() {
        editDisplayHref();
    }
}
