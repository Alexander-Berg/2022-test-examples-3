package ru.yandex.market.admin.service.remote;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit-тесты для {@link RemoteDataFeedHelper}.
 *
 * @author fbokovikov
 */
public class RemoteDataFeedHelperTest {

    public static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("http://market-mbi-test.s3.mdst.yandex.net/upload-feed/100/upload-feed-829100", true),
                Arguments.of("http://jing.yandex-team.ru/feed.xls", false),
                Arguments.of("http://yandex1net/feed.xls", false),
                Arguments.of("http://yandex.ru", false),
                Arguments.of("https://umkamall.ru/yml/export/7.yml", false),
                Arguments.of("https://disk.yandex.ru/client/disk", false)
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testIsUpload(String url, boolean expectedUpload) {
        boolean actualUpload = RemoteDataFeedHelper.isUpload(url);
        Assertions.assertEquals(expectedUpload, actualUpload);
    }
}
