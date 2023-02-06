package ru.yandex.common.util.http.loader;

import junit.framework.TestCase;

/**
 * Date: 08.02.2007
 * Time: 21:14:39
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class SimplePageLoaderTest extends TestCase {
    public void testFake() throws Exception {
    }
    /*
    Тесты используют внешние сайты, нужно их переписать на использование моков
    private HttpPageLoader loader;
    private HttpClientFactory factory = HttpClientFactoryImpl.getInstance();

    protected void setUp() throws Exception {
        super.setUp();
        loader = new HttpPageLoader();
        loader.setHttpClient(createTestClient());
    }

    public void testLoadYaRu() throws Exception {
        String page = new String(loader.getPage(factory.createGetMethod("http://ya.ru")).getBytes());
        assertTrue(!page.isEmpty());
    }

    public void testLoadYaRuByPostMethod() throws Exception {
        String page = new String(
            loader.getPage(factory.createPostMethod("http://ya.ru", Collections.emptyMap())).getBytes()
        );
        assertTrue(page.contains(year()));
    }
    private String year() {
        return Calendar.getInstance().get(Calendar.YEAR)+"";
    }

    public void testExOnGet() throws Exception {
        try {
            loader.getPage(new HttpGet(""));
            fail();
        } catch (Exception e) {
            //ok
        }
    }

    public void testZanussi() throws Exception {
        loader.getPage(new HttpGet("http://www.bosch-bt.ru/livingbosch/housewife/container17/article2207/"));
    }

    public void testLoadHttps() throws Exception {
        loader.getPage(factory.createGetMethod("https://sauth.yandex.ru/passport?mode=passport"));
    }

    public void testRedirectToHttps() throws Exception {
        String url = "http://www.sapato.ru/personal/basket/?r1=ya_market&r2=ya_market_model&wpp=webmarketing.mp.yandex_market&utm_source=market.yandex&utm_medium=cpc&cpm=skidka20xml&utm_campaign=G%26G_Greta%2BSilver_Strap_99298_V1013&addProduct=95296";
        String page = new String(loader.getPage(factory.createGetMethod(url)).getBytes());

        url = "http://sprinthost.ru/tariffs/upgrade_3p3.html?period=12m";
        loader.getPage(factory.createGetMethod(url));
    }

    public void testDoesNotGetBinaryPages() throws Exception {
        try {
            loader.getPage(factory.createGetMethod("http://img.yandex.net/i/yandex-big.png"));

            fail();
        } catch(Exception e) {
            // ok
        }
        try {
            loader.getPage(factory.createGetMethod("http://org.downloadcenter.samsung.com/downloadfile/ContentsFile.aspx?CDSite=RU&CttFileID=291995&CDCttType=UM&ModelType=N&ModelName=MM-%D0%A16&VPath=UM/200504/20050412104846984_MM-C6-RUS.pdf"));

            fail();
        } catch(Exception e) {
            // ok
        }
        try {
            loader.getPage(factory.createGetMethod("http://org.downloadcenter.samsung.com/downloadfile/ContentsFile.aspx?CDSite=RU&CttFileID=2016134&CDCttType=SW&ModelType=N&ModelName=075.918-3&VPath=SW/200809/20080918150353328_Samsung_PC_Studio_322_HF1.exe"));

            fail();
        } catch(Exception e) {
            // ok
        }
    }

    private HttpClient createTestClient() {
        return factory.createHttpClient();
    }
    */
}
