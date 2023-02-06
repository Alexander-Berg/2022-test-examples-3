package ru.yandex.common.util.http.sita;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.http.CrawlParams;
import ru.yandex.common.util.http.HttpGetLocation;
import ru.yandex.common.util.http.Location;
import ru.yandex.common.util.http.Response;
import ru.yandex.sita.SitaClient;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 17.06.2013
 */
public class SitaPageProviderTest {

    private SitaPageProvider sitaPageProvider;

    /*
        ssh -f -N -L 12180:sita.yandex.net:12180 callisto
    */
    @Before
    public void setUp() throws Exception {
        sitaPageProvider = new SitaPageProvider();
        SitaClient sitaClient = new SitaClient("any");
        sitaClient.setSitaUrl("http://localhost:12180");
        sitaPageProvider.setSitaClient(sitaClient);
    }

//    @Test
//    public void testFetchResponse() throws Exception {
//        HttpLocation httpLocation = new HttpGetLocation("http://www.pratikcarsi.com");
//        Response response = sitaPageProvider.fetchResponse(httpLocation);
//    }

    @Test
    public void testFetchResponses() throws Exception {
        Location location = new HttpGetLocation("http://yandex.ru");
        Map<Location, Response> responses = sitaPageProvider.fetchResponses(Collections.singleton(location));
    }

    @Test
    public void testFresh() throws Exception {
        Location location = new HttpGetLocation("http://megaobzor.com/news-topic-15-page-1.html");
        CrawlParams crawlParams = new CrawlParams(42, false, 1000);
        Map<Location, Response> responses = sitaPageProvider.fetchResponses(
                Collections.singleton(location), crawlParams
        );
        Response response = responses.get(location);
        Assert.assertFalse(response.isOk());
    }

    @Test
    public void testLanguage() {
        Location location = new HttpGetLocation("http://ya.ru/");
        Response response = sitaPageProvider.fetchResponse(location);
        //Assert.assertEquals("rus", response.getPage().getLanguage());
    }
}
