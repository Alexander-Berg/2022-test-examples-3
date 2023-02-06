package ru.yandex.common.util.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.common.util.http.LinkResolver.resolve;

/**
 * Created on 16:03:28 20.04.2007
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class LinkResolverTest {
    @Test
    public void testLinkResolver() {
        assertEquals("http://site.com/?hello",
            resolve("http://site.com", "?hello"));

        assertEquals("http://site.com/products/high_performance/",
            resolve("http://site.com/products/tv/", "../high_performance/"));

        assertEquals("http://site.com/search.php",
            resolve("http://site.com/products/tv.php", "../search.php"));

        assertEquals("http://site.com/search.php",
            resolve("http://site.com", "../search.php"));

        assertEquals("http://site.com/search.php",
            resolve("http://site.com/products/tv/tv.php", "../../search.php"));

        assertEquals("http://site.com/",
            resolve("http://site.com/products/tv/ty/tv.php", "../../../"));

        assertEquals("http://site.com/search.php",
            resolve("http://site.com/", "../../search.php"));

        assertEquals("http://www.spbpages.ru/comp55630_1029/",
            resolve("http://www.spbpages.ru/ind1029_18", "../comp55630_1029/"));

        assertEquals("http://site.com/products/high_performance/",
            resolve("http://site.com/", "products/high_performance/"));

        assertEquals("http://site.com/products/high_performance/",
            resolve("http://site.com/products/", "high_performance/"));

        assertEquals("http://site.com/other_stuff/",
            resolve("http://site.com/products/", "/other_stuff/"));

        assertEquals("http://site.com/products/high_performance/",
            resolve("http://site.com/", "/products/high_performance/"));

        assertEquals("http://site.com/products/high_performance/",
            resolve("http://site.com/products/tv", "/products/high_performance/"));

        assertEquals("http://site.com/products/high_performance/",
            resolve("http://site.com/products/", "/products/high_performance/"));

        assertEquals("http://www.ariston.ru/index.phtml?did=2&chid=7&subchid=9",
            resolve("http://www.ariston.ru/index.phtml?did=2", "index.phtml?did=2&chid=7&subchid=9"));

        assertEquals("http://site.com/outdex.php?c=1&d=2",
            resolve("http://site.com/index.php?a=1", "outdex.php?c=1&d=2"));

        assertEquals("http://another.com/?c=1&d=2",
            resolve("http://site.com/index.php?a=1", "http://another.com/?c=1&d=2"));

        assertEquals("http://vaio.sony.ru/view/ShowProductCategory.action?site=voe_ru_RU_cons&category=AC-Adapters#w1",
            resolve("http://vaio.sony.ru/view/ShowProductCategory.action?site=voe_ru_RU_cons&category=AC-Adapters", "#w1"));

        assertEquals("http://www.sonyericsson.com/spg.jsp?cc=ru&lc=ru&ver=4000&template=pp1&zone=pp&lm=pp",
            resolve("http://www.sonyericsson.com/spg.jsp?cc=ru&lc=ru&ver=4000&template=ph1&zone=ph", "spg.jsp?cc=ru&lc=ru&ver=4000&template=pp1&zone=pp&lm=pp"));

        assertEquals("http://www.samsung.ru/",
            resolve("http://www.samsung.ru/products/phones/gsm/sgh-c250/", "/"));

        assertEquals("http://www.atlant.by/?r=442&p=12&la=r&item=705#705",
            resolve("http://www.atlant.by/?r=442&p=12&la=r", "?r=442&p=12&la=r&item=705#705"));

        assertEquals("http://www.bbk.ru/7__173_6_1.htm",
            resolve("http://www.bbk.ru/7_173.htm", "7__173_6_1.htm"));

        assertEquals("http://www.digitex.ru/product_group.php?id=106",
            resolve("http://www.digitex.ru/catalog.php", "product_group.php?id=106"));

        assertEquals("http://www.ardorussia.ru/main.php?trid=269",
            resolve("http://www.ardorussia.ru/main.php?trid=8", " main.php?trid=269"));

        assertEquals("http://shopping.yahoo.com/b:Clothing%2C%20Accessories%20%26%20Shoes:22658899;_ylt=AqcCKjJlCejFoVfkkE3yT1sEgFoB;_ylu=X3oDMTBhanZuZW9iBHNlYwNicm93c2U-",
            resolve("http://shopping.yahoo.com/", "http://shopping.yahoo.com/b:Clothing%2C%20Accessories%20%26%20Shoes:22658899;_ylt=AqcCKjJlCejFoVfkkE3yT1sEgFoB;_ylu=X3oDMTBhanZuZW9iBHNlYwNicm93c2U-"));

        assertEquals("http://www.sonyericsson.com/LinkServlet.jsp?pageID=cip://mc/page/LoS_1&srcSite=cws&targetSite=fd&cc=ru&lc=ru",
            resolve("http://www.sonyericsson.com/spg.jsp?cc=ru&lc=ru&ver=4000&template=ph1&zone=ph", "LinkServlet.jsp?pageID=cip://mc/page/LoS_1&srcSite=cws&targetSite=fd&cc=ru&lc=ru"));

        assertEquals("http://www.pioneer-rus.ru/ru/products/62/63/413/overview.html",
            resolve("http://www.pioneer-rus.ru/ru/products/62/index.html", "/ru/products/62/63/413/overview.html"));

        assertEquals("http://www.olympus.com.ru/",
            resolve("http://www.olympus.com.ru/", ""));

        assertEquals("http://www.etencorp.com/products/index.php",
            resolve("http://www.etencorp.com", "products/index.php"));

        assertEquals("http://site.com/?a=1&b=2",
            resolve("http://site.com/", "?a=1&b=2"));

        assertEquals("http://site.com/index.php?a=1&b=2",
            resolve("http://site.com/index.php", "?a=1&b=2"));

        assertEquals("http://site.com/index.php?a=1&b=2",
            resolve("http://site.com/", "index.php?a=1&b=2"));

        assertEquals("http://site.com/index.php?a=1&b=2",
            resolve("http://site.com/index.php?a=1", "?a=1&b=2"));

        assertEquals("http://site.com/index.php?a=1&b=2",
            resolve("http://site.com/index.php?a=1", "?a=1&b=2"));

        assertEquals("www.megaauto.md/img/thumb.php?id=9451_1.jpg&w=500",
            resolve("www.megaauto.md/BMW/X3/auto_9451.htm", "www.megaauto.md/img/thumb.php?id=9451_1.jpg&w=500"));
    }
}
