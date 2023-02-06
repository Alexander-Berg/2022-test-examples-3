package ru.yandex.common.services.maps.router;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import ru.yandex.common.services.maps.router.model.RouterGpsCoordinate;
import ru.yandex.common.services.maps.router.model.RouterMetaData;
import ru.yandex.common.services.maps.router.model.RouterResponse;
import ru.yandex.common.services.maps.router.model.RouterSegmentData;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.common.services.maps.router.YandexMapsRouterTestUtils.checkCoordinates;
import static ru.yandex.common.services.maps.router.YandexMapsRouterTestUtils.checkSegments;

/**
 * Unit-тесты для {@link YandexMapsRouterParser}.
 *
 * @author Vladislav Bauer
 */
public class YandexMapsRouterParserTest {

    private static final String FILE_EXISTED_PATH = "test-router-existed-path.json";
    private static final String FILE_UNKNOWN_PATH = "test-router-unknown-path.json";

    private static final int CORRECT_LENGTH = 29238;
    private static final int CORRECT_TIME = 2262;
    private static final int CORRECT_JAMS_TIME = 2874;


    /**
     * Тест проверяющий ответ Яндекс Маршрутизатора о НЕ существующем пути между двумя GPS координатами.
     *
     * Запрос: http://route.tst.maps.yandex.net/1.x/?rll=54.858052,83.110501~82.900737,55.040361&format=json
     *
     * Точка отправления: 83.110501, 54.858052 Северный Ледовитый океан
     * Пункт назначения: 55.040361, 82.900737 Новосибирск, Красноярская 35
     */
    @Test
    public void testParserUnknownPath() throws Exception {
        final RouterResponse routerData = parseRouterData(FILE_UNKNOWN_PATH);
        assertThat(routerData,  nullValue());
    }

    /**
     * Тест проверяющий ответ Яндекс Маршрутизатора о существующем пути между двумя GPS координатами.
     *
     * Запрос: http://route.tst.maps.yandex.net/1.x/?rll=83.110501,54.858052~82.900737,55.040361&format=json
     *
     * Точка отправления: 54.858052, 83.110501 Новосибирск, Николаева, 11
     * Пункт назначения: 55.040361, 82.900737 Новосибирск, Красноярская 35
     */
    @Test
    public void testParserExistedPath() throws Exception {
        // Маршрут от Яндекс Маршрутизатора простроен корректно
        final RouterResponse routerData = parseRouterData(FILE_EXISTED_PATH);
        assertThat(routerData, notNullValue());

        // Проверить мета-информацию по всему маршруту
        final List<RouterGpsCoordinate> totalPath = routerData.getPath();
        final RouterMetaData metaData = routerData.getMetaData();

        checkCoordinates(metaData, totalPath);

        assertThat(totalPath, hasSize(490));
        assertThat((int) metaData.getLength(), equalTo(CORRECT_LENGTH));
        assertThat((int) metaData.getTime(), equalTo(CORRECT_TIME));
        assertThat((int) metaData.getJamsTime(), equalTo(CORRECT_JAMS_TIME));

        // Проверить сегменты пути
        final List<RouterSegmentData> segments = routerData.getSegments();
        assertThat(segments, hasSize(6));

        checkSegments(segments);
    }

    /**
     * Разобрать ответ Яндекс Маршрутизатора.
     */
    @Nullable
    private RouterResponse parseRouterData(final String resourceName) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(resourceName)) {
            final String jsonData = IOUtils.toString(stream, StandardCharsets.UTF_8);
            final Optional<RouterResponse> routerData = YandexMapsRouterParser.parse(jsonData);
            return routerData.orElse(null);
        }
    }

}
