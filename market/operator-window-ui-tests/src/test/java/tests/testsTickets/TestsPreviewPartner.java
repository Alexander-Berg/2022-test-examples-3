package ui_tests.src.test.java.tests.testsTickets;

import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Critical;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Partners;

public class TestsPreviewPartner {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsPreviewPartner.class);

    @InfoTest(
            descriptionTest = "Поиск партнера на превью партнера по названию - shop",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-1081"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1081_FindPartnerInThePartnerPreviewByShopName() {

        // Получить тикет b2b без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null,'clientEmail':'"+Tools.other().getRandomText()+"@yandex.ru'])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Скрыть тосты
        Entity.toast(webDriver).hideNotificationError();

        // Перейти на таб все партнеры
        Pages.ticketPage(webDriver).partnerPreviewTab().gotoAllPartners();

        // Ввести имя партнёра в поиск
        Pages.ticketPage(webDriver).partnerPreviewTab().setPartner(Partners.shopName);

        // Нажать на кнопку поиска
        Pages.ticketPage(webDriver).partnerPreviewTab().clickSearchButton();

        // Убедиться, что нужный партнёр нашёлся
        Assert.assertTrue("Партнёр не найден",
                Pages.ticketPage(webDriver).partnerPreviewTab().findPartner(Partners.shopName));
    }

    @InfoTest(
            descriptionTest = "Поиск партнера на превью партнёра по названию - supplier",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1082"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1082_FindPartnerInThePartnerPreviewBySupplierName() {

        // Получить тикет b2b без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Скрыть тосты
        Entity.toast(webDriver).hideNotificationError();

        // Перейти на таб все партнеры
        Pages.ticketPage(webDriver).partnerPreviewTab().gotoAllPartners();

        // Ввести имя партнёра в поиск
        Pages.ticketPage(webDriver).partnerPreviewTab().setPartner(Partners.supplierName);

        // Нажать на кнопку поиска
        Pages.ticketPage(webDriver).partnerPreviewTab().clickSearchButton();

        // Убедиться, что нужный партнёр нашёлся
        Assert.assertTrue("Партнёр не найден",
                Pages.ticketPage(webDriver).partnerPreviewTab().findPartner(Partners.supplierName));
    }

    @InfoTest(
            descriptionTest = "Поиск партнера на превью партнера по названию - vendor",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1083"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1083_FindPartnerInThePartnerPreviewByVendorName() {

        // Получить тикет b2b без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Скрыть тосты
        Entity.toast(webDriver).hideNotificationError();

        // Перейти на таб все партнеры
        Pages.ticketPage(webDriver).partnerPreviewTab().gotoAllPartners();

        // Ввести имя партнёра в поиск
        Pages.ticketPage(webDriver).partnerPreviewTab().setPartner(Partners.vendorName);

        // Нажать на кнопку поиска
        Pages.ticketPage(webDriver).partnerPreviewTab().clickSearchButton();

        // Убедиться, что нужный партнёр нашёлся
        Assert.assertTrue("Партнёр не найден",
                Pages.ticketPage(webDriver).partnerPreviewTab().findPartner(Partners.vendorName));
    }

    @InfoTest(
            descriptionTest = "Поиск партнера на превью партнёра по id - shop",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1084"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1084_FindPartnerInThePartnerPreviewByShopId() {

        // Получить тикет b2b без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Скрыть тосты
        Entity.toast(webDriver).hideNotificationError();

        // Перейти на таб все партнеры
        Pages.ticketPage(webDriver).partnerPreviewTab().gotoAllPartners();

        // Ввести id партнёра в поиск
        Pages.ticketPage(webDriver).partnerPreviewTab().setPartner(Partners.shopId);

        // Нажать на кнопку поиска
        Pages.ticketPage(webDriver).partnerPreviewTab().clickSearchButton();

        // Убедиться, что нужный партнёр нашёлся
        Assert.assertTrue("Партнёр не найден",
                Pages.ticketPage(webDriver).partnerPreviewTab().findPartner(Partners.shopName));
    }

    @InfoTest(
            descriptionTest = "Поиск партнера на превью партнера по id - supplier",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1085"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1085_FindPartnerInThePartnerPreviewBySupplierId() {

        // Получить тикет b2b без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Скрыть тосты
        Entity.toast(webDriver).hideNotificationError();

        // Перейти на таб все партнеры
        Pages.ticketPage(webDriver).partnerPreviewTab().gotoAllPartners();

        // Ввести id партнёра в поиск
        Pages.ticketPage(webDriver).partnerPreviewTab().setPartner(Partners.supplierId);

        // Нажать на кнопку поиска
        Pages.ticketPage(webDriver).partnerPreviewTab().clickSearchButton();

        // Убедиться, что нужный партнёр нашёлся
        Assert.assertTrue("Партнёр не найден",
                Pages.ticketPage(webDriver).partnerPreviewTab().findPartner(Partners.supplierName));
    }

    @InfoTest(
            descriptionTest = "Поиск партнера на превью партнера по id - vendor",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1086"
    )
    @Category({Critical.class})
    @Test
    public void ocrm1086_FindPartnerInThePartnerPreviewByVendorId() {

        // Получить тикет b2b без партнёра
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    eq('partner',null)\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "if (!ticket){\n" +
                        "ticket = api.db.of('ticket$b2b').withFilters{\n" +
                        "    not(eq('partner',null))\n" +
                        String.format("between ('creationTime',%s)\n", Tools.date().getDatesInterval()) +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "api.bcp.edit(ticket,['partner':null])\n" +
                        "}\n" +
                        "return ticket");

        // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Скрыть тосты
        Entity.toast(webDriver).hideNotificationError();

        // Перейти на таб все партнеры
        Pages.ticketPage(webDriver).partnerPreviewTab().gotoAllPartners();

        // Ввести id партнёра в поиск
        Pages.ticketPage(webDriver).partnerPreviewTab().setPartner(Partners.vendorId);

        // Нажать на кнопку поиска
        Pages.ticketPage(webDriver).partnerPreviewTab().clickSearchButton();

        // Убедиться, что нужный партнёр нашёлся
        Assert.assertTrue("Партнёр не найден",
                Pages.ticketPage(webDriver).partnerPreviewTab().findPartner(Partners.vendorName));
    }

}
