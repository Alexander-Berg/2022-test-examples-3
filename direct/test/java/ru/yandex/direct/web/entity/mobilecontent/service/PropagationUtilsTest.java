package ru.yandex.direct.web.entity.mobilecontent.service;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.web.entity.mobilecontent.service.PropagationUtils.TrackingUrl;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class PropagationUtilsTest {

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public String formerAppTrackingUrl;

    @Parameterized.Parameter(2)
    public String bannerTrackingUrl;

    @Parameterized.Parameter(3)
    public String updatedAppTrackingUrl;

    @Parameterized.Parameter(4)
    public TrackingUrl expectedBannerTrackingUrl;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
                // formerAppTrackingUrl, bannerTrackingUrl, updatedAppTrackingUrl, expected
                {"не было и не будет",
                        null, null, null, TrackingUrl.absent()},
                {"В приложение и на баннере не было урла, хотим чтобы был",
                        null, null, "http://google.com?a=1", TrackingUrl.of("http://google.com?a=1")},
                {"Актуальный без изменений, удаляем",
                        "http://ya.ru", "http://ya.ru", null, TrackingUrl.absent()},
                {"Актуальный с добавочным параметром, всё равно удаляем",
                        "http://ya.ru", "http://ya.ru?a=1", null, TrackingUrl.absent()},

                {"Актуальный без изменений, меняем на такой же",
                        "http://ya.ru", "http://ya.ru", "http://ya.ru", TrackingUrl.of("http://ya.ru")},
                {"Актуальный без изменений, меняем всё",
                        "http://ya.ru?a=1", "http://ya.ru?a=1", "http://google.com?a=2#f",
                        TrackingUrl.of("http://google.com?a=2#f")},
                {"В актуальном добавочный параметр, ожидаем, что он добавиться в созданный",
                        "http://ya.ru", "http://ya.ru?a=1", "http://google.com",
                        TrackingUrl.of("http://google.com?a=1")},
                {"В актуальном добавочный параметр, в новом -- ещё один, ожидаем, что все добавлятся в созданный",
                        "http://ya.ru", "http://ya.ru?a=1", "http://google.com?b=1",
                        TrackingUrl.of("http://google.com?b=1&a=1")},
                {"В актуальном добавочный параметр, в новом -- ещё один, с таким же названием, ожидаем, что все добавлятся в созданный",
                        "http://ya.ru", "http://ya.ru?a=1", "http://google.com?a=2",
                        TrackingUrl.of("http://google.com?a=2&a=1")},
                {"В актуальном добавочный параметр, в новом -- такой же, ожидаем, что параметр не дублируется",
                        "http://ya.ru?a=1", "http://ya.ru?a=1&a=2", "http://google.com?a=2",
                        TrackingUrl.of("http://google.com?a=2")},
                {"Ожидаем, что фрагмент берётся из нового",
                        "http://ya.ru#a", "http://ya.ru?a=1#b", "http://ya.ru#c", TrackingUrl.of("http://ya.ru?a=1#c")},

                {"Отличается протокол",
                        "http://ya.ru", "https://ya.ru", "http://ya.ru?a=b", null},
                {"Отличается домен",
                        "http://ya.ru", "http://google.com", "http://ya.ru?a=b", null},
                {"Разный путь",
                        "http://ya.ru/xxx", "http://ya.ru/yyy", "http://ya.ru?a=b", null},
                {"в actual нет параметра из former",
                        "http://ya.ru/?a=1", "http://ya.ru/?a=2", "http://ya.ru?a=b", null},
                {"В приложении не было, но на баннере указан -- не применимо",
                        null, "http://ya.ru?a=1", "http://google.com?a=2", null},
        };
    }

    @Test
    public void createNewBannerTrackingUrl() {
        Optional<TrackingUrl> newBannerTrackingUrl =
                PropagationUtils.createNewBannerTrackingUrl(formerAppTrackingUrl, updatedAppTrackingUrl,
                        bannerTrackingUrl);
        if (expectedBannerTrackingUrl != null) {
            assertThat(newBannerTrackingUrl.isPresent())
                    .as("createNewBannerTrackingUrl returns new url")
                    .isTrue();
            //noinspection OptionalGetWithoutIsPresent
            assertThat(newBannerTrackingUrl.get())
                    .as("new tracking url is the same as expected")
                    .isEqualTo(expectedBannerTrackingUrl);
        } else {
            assertThat(newBannerTrackingUrl.isPresent())
                    .as("createNewBannerTrackingUrl returns empty optional")
                    .isFalse();
        }
    }
}
