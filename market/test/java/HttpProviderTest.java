
import org.junit.Test;

import ru.yandex.common.util.http.CrawlParams;
import ru.yandex.common.util.http.HttpGetLocation;
import ru.yandex.common.util.http.Page;
import ru.yandex.common.util.http.Response;
import ru.yandex.common.util.http.js.HtmlUnitPageProvider;
import ru.yandex.common.util.http.js.HttpJsLocation;

/**
 * @author Tatiana Litvinenko <a href="mailto:tanlit@yandex-team.ru"/>
 * @date 06.11.13
 */
public class HttpProviderTest {

    @Test
    public void testCreate() throws Exception {
        HtmlUnitPageProvider provider = new HtmlUnitPageProvider();
        Response response = provider.fetchResponse(
                new HttpJsLocation(new HttpGetLocation("http://ya.ru")), new CrawlParams(0, false, 0)
        );
        response.throwExceptionIfNeed();
        Page page = response.getPage();

    }
}
