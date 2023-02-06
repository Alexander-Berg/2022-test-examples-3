import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.bangenproxy.client.zenmeta.ZenMetaInfoClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(Parameterized.class)
public class GetZenContentIdByUrlTest {

    public static final String EMPTY_OPTIONAL_TEXT = "empty";

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public String inputUrl;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"GetZenContentId", "https://zen.yandex.ru/media/super-blog/some-content12345", "super-blog/some" +
                        "-content12345"},
                {"GetZenContentIdOnZenChannel", "https://zen.yandex.ru/super-blog", "super-blog",},
                {"GetZenContentIdWithParams", "https://zen.yandex.ru/super-blog/some-content12345?&", "super-blog" +
                        "/some-content12345"},
                {"GetZenContentIdWithParams2()", "https://zen.yandex.ru/super-blog/some-content12345?abc=1&qwerty" +
                        "=1234", "super-blog/some-content12345"},
                {"GetZenContentIdWithParamsOnChannel", "https://zen.yandex.ru/super-blog?abc=1&qwerty=1234", "super" +
                        "-blog"},
                {"GetContentIdFromDomainChannel", "https://zen.yandex.ru/geekbrains.ru", "geekbrains.ru"},
                {"GetContentIdFromDomainChannelWithParams", "https://zen.yandex.ru/media/geekbrains" +
                        ".ru/kak-strukturirovat-rabochie-zadachi-61ba27515fc102109526b43f?&", "geekbrains" +
                        ".ru/kak-strukturirovat-rabochie-zadachi-61ba27515fc102109526b43f"},
                {"GetContentIdFromZenIdUrl", "https://zen.yandex.ru/media/id/606f008b1d54017e1d4436d1/porabotal-paru" +
                        "-let-a-potom-nachal-tormozit--5-oshibok-pri-pokupke-noutbuka-61ce0723af72224b7aa68880?&",
                        "id/606f008b1d54017e1d4436d1/porabotal-paru-let-a-potom-nachal-tormozit--5-oshibok-pri" +
                                "-pokupke-noutbuka-61ce0723af72224b7aa68880"},
                {"GetContentIdFromZenChannelIdUrl", "https://zen.yandex.ru/id/5b071a35f03173cc7a3d655b", "id" +
                        "/5b071a35f03173cc7a3d655b"},
                {"GetContentIdFromZenUrlWithoutPath", "https://zen.yandex.ru", EMPTY_OPTIONAL_TEXT},
                {"GetContentIdFromZenUrlWithoutPathWithClosingSlash", "https://zen.yandex.ru/", EMPTY_OPTIONAL_TEXT}
        });
    }

    @Test
    public void testGetContentIdByUrl() {
        String actual = ZenMetaInfoClient.getContentIdByUrl(inputUrl).orElse(EMPTY_OPTIONAL_TEXT);
        assertThat(actual, equalTo(expected));
    }
}
