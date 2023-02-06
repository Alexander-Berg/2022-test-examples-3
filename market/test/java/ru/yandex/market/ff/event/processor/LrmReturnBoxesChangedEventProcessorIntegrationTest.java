package ru.yandex.market.ff.event.processor;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBox;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxesChangedPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class LrmReturnBoxesChangedEventProcessorIntegrationTest extends IntegrationTest {

    @Autowired
    private LrmReturnBoxesChangedEventProcessor lrmReturnBoxesChangedEventProcessor;

    @Test
    @DatabaseSetup("classpath:event-processor/return-boxes-changed/before-create.xml")
    @ExpectedDatabase(value = "classpath:event-processor/return-boxes-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCreateNewBoxesOnUpdate() {
        lrmReturnBoxesChangedEventProcessor.process(createReturnEvent());
    }

    @Test
    @DatabaseSetup("classpath:event-processor/return-boxes-changed/before-delete.xml")
    @ExpectedDatabase(value = "classpath:event-processor/return-boxes-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testDeleteBoxesOnUpdate() {
        lrmReturnBoxesChangedEventProcessor.process(createReturnEvent());
    }

    @Test
    @DatabaseSetup("classpath:event-processor/return-boxes-changed/before-create-and-delete.xml")
    @ExpectedDatabase(value = "classpath:event-processor/return-boxes-changed/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testCreateAndDeleteBoxesOnUpdate() {
        lrmReturnBoxesChangedEventProcessor.process(createReturnEvent());
    }

    @Test
    @DatabaseSetup("classpath:event-processor/return-boxes-changed/before-update-single-place-box.xml")
    @ExpectedDatabase(value = "classpath:event-processor/return-boxes-changed/after-update-single-place-box.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testUpdateSinglePlaceBox() {
        lrmReturnBoxesChangedEventProcessor.process(createSinglePlaceReturnEvent());
    }

    private ReturnEvent createReturnEvent() {
        new ReturnBoxesChangedPayload();
        ReturnBoxesChangedPayload payload = new ReturnBoxesChangedPayload();
        payload.setBoxes(List.of(ReturnBox.builder().externalId("box1").build(),
                ReturnBox.builder().externalId("box2").build()));
        return ReturnEvent.builder()
                .returnId(11L)
                .payload(payload)
                .build();
    }

    private ReturnEvent createSinglePlaceReturnEvent() {
        new ReturnBoxesChangedPayload();
        ReturnBoxesChangedPayload payload = new ReturnBoxesChangedPayload();
        payload.setBoxes(List.of(ReturnBox.builder().externalId("box2").build()));
        return ReturnEvent.builder()
                .returnId(11L)
                .payload(payload)
                .build();
    }
}
