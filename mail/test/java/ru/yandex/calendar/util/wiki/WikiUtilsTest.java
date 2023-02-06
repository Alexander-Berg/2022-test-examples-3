package ru.yandex.calendar.util.wiki;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author Eugene Voytitsky
 */
public class WikiUtilsTest extends CalendarTestBase {

    @Test
    public void cleanHeads() {
        String head = "<h1 class=\"wiki-head i-bem\" onclick=\"return &quot;wiki-head&quot;\">" +
                "<div class=\"wiki-head__anchors\">" +
                "<a class=\"wiki-head__anchor\" name=\"address\"></a>" +
                "<a class=\"wiki-head__anchor\" name=\"h-1\"></a></div>" +
                "Адрес / Address" +
                "<span class=\"wiki-head__edit no-print\">" +
                "<a class=\"b-link wiki-head__link wiki-head__link_type_anchor\" href=\"#address\">&#167;</a>" +
                "<a class=\"b-link wiki-head__link wiki-head__link_type_edit\" href=\".edit\">edit</a></span>" +
                "</h1>";

        String expectedCleanedHead = "<h1>Адрес / Address</h1>";

        ListF<String> addons = Cf.list("", "a", "bb");
        for (String prefix : addons) {
            for (String suffix : addons) {
                for (int i : Cf.list(0, 1, 2, 3)) {
                    String actualCleanedHead = WikiUtils.cleanHeads(Cf.repeat(head, i).mkString(prefix, "", suffix));
                    Assert.equals(Cf.repeat(expectedCleanedHead, i).mkString(prefix, "", suffix), actualCleanedHead);
                }
            }
        }
    }

    @Test
    public void stripHideRefererFromSrc() {
        Assert.equals(
                "<img class=\"wiki-img\" src=\"http://api-maps.yandex.ru/services/constructor/" +
                        "1.0/static/?sid=9pzEVNNbd28FY7cI60JN0oemn9MY41ph&width=600&height=450\" alt=\"\"",
            WikiUtils.stripHideRefererFromLinks(
                    "<img class=\"wiki-img\" src=\"//h.yandex-team.ru/?http%3A%2F%2Fapi-maps.yandex.ru%2Fservices%2F" +
                            "constructor%2F1.0%2Fstatic%2F%3F" +
                            "sid%3D9pzEVNNbd28FY7cI60JN0oemn9MY41ph%26width%3D600%26height%3D450\" alt=\"\""));
    }

    @Test
    public void stripHideRefererFromHref() {
        Assert.equals(
            "<a class=\"b-link wiki-ref\" href=\"http://maps.yandex.ru/-/CVfBZTlE\"" +
                    "target=\"_blank\"",
            WikiUtils.stripHideRefererFromLinks(
                    "<a class=\"b-link wiki-ref\" href=\"//h.yandex-team.ru/?http%3A%2F%2Fmaps.yandex.ru%2F-%2FCVfBZTlE\"" +
                            "target=\"_blank\""));
    }

}
