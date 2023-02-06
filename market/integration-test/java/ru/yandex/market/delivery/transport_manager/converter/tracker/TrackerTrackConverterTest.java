package ru.yandex.market.delivery.transport_manager.converter.tracker;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Tracks;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class TrackerTrackConverterTest extends AbstractContextualTest {
    @Autowired
    private TrackerTrackConverter converter;

    @Test
    @SneakyThrows
    void convert() {
        ObjectMapper om = new ObjectMapper();
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.registerModule(new JavaTimeModule());

        String jsonData = extractFileContent("controller/tracker/movement_tracks.json");

        Tracks expected = om.readValue(jsonData, Tracks.class);
        List<DeliveryTrack> trackerEntity = om.readValue(jsonData, new TypeReference<>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {

                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{
                            DeliveryTrack.class
                        };
                    }

                    @Override
                    public Type getRawType() {
                        return List.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                };
            }
        });

        softly.assertThat(converter.convert(trackerEntity))
            .isEqualToComparingFieldByFieldRecursively(expected);
    }
}
