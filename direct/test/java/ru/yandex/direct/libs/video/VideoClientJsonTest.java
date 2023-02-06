package ru.yandex.direct.libs.video;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.libs.video.model.VideoBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

public class VideoClientJsonTest {
    @Test
    public void fromJson_returnsObject() throws IOException {
        URL url = Resources.getResource("test_data.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        Map<String, VideoBanner> banners = VideoBanner.parseBannerCollectionJson(response);

        assertThat(banners).isNotEmpty();
    }

    @Test
    public void fromJson_noData_returnsNull() throws IOException {
        URL url = Resources.getResource("test_no_data.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        Map<String, VideoBanner> banners = VideoBanner.parseBannerCollectionJson(response);

        assertThat(banners).isEmpty();
    }

    @Test(expected = IllegalStateException.class)
    public void fromJson_wrongFormat_throwsException() throws IOException {
        URL url = Resources.getResource("test_wrong_format_data.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        VideoBanner.parseBannerCollectionJson(response);
    }

    @Test
    public void toJson_propertiesAreSorted() throws IOException {
        // Кастомный маппер с претти-принтом, чтобы вычленить свойства на одном уровне вложенности
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);

        // Десериализуем стандартным маппером
        URL url = Resources.getResource("test_data.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        VideoBanner banner = VideoBanner.parseBannerCollectionJson(response).entrySet().iterator().next().getValue();

        // Сериализуем с претти-принтом
        String jsonResult = mapper.writeValueAsString(banner);
        // Берем все свойства верхнего уровня (на самом деле второго уровня)
        Pattern pattern = Pattern.compile("^\\s{4}\"(\\w+)\"", Pattern.MULTILINE);
        Matcher m = pattern.matcher(jsonResult);

        ArrayList<String> properties = new ArrayList<>();
        while (m.find()) {
            properties.add(m.group());
        }

        // Проверяем, что свойства отсортированы
        assertThat(StreamEx.of(properties).toList())
                .isEqualTo(StreamEx.of(properties).sorted().toList());
    }

    @Test
    public void fromJson_duplicateKeyGtaRelatedAttributes() throws IOException {
        URL url = Resources.getResource("test_double_key.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        Map<String, VideoBanner> banners = VideoBanner.parseBannerCollectionJson(response);

        assertThat(banners).hasSize(4);
    }

    @Test
    public void fromJson_halfFilledVideoData() throws IOException {
        URL url = Resources.getResource("test_half_filled_real_data.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        Object response = fromJson(json, Object.class);

        Map<String, VideoBanner> banners = VideoBanner.parseBannerCollectionJson(response);

        assertThat(banners).hasSize(0);
    }
}
