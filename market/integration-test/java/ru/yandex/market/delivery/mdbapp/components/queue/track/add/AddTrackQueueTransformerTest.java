package ru.yandex.market.delivery.mdbapp.components.queue.track.add;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.dto.AddTrackDto;

public class AddTrackQueueTransformerTest extends MockContextualTest {
    @Autowired
    private AddTrackFromLomQueueTransformer transformer;

    @Test
    public void serializationWorksAsExpected() {
        AddTrackDto payload = new AddTrackDto(12345L, 111L);

        Assertions.assertThat(transformer.toObject(transformer.fromObject(payload)))
            .as("Serialized and deserialize")
            .isEqualTo(payload);
    }
}
