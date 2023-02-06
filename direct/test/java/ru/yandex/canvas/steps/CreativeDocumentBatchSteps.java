package ru.yandex.canvas.steps;

import java.util.Date;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.model.CreativeDocumentBatch;

import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static ru.yandex.canvas.steps.CreativeDocumentSteps.createEmptyCreativeDocument;

@ParametersAreNonnullByDefault
public class CreativeDocumentBatchSteps {
    public static CreativeDocumentBatch createCreativeDocumentBatch(Long clientId) {
        return createCreativeDocumentBatch(clientId, UUID.randomUUID().toString());
    }

    public static CreativeDocumentBatch createCreativeDocumentBatch(Long clientId, String batchName) {
        String batchId = UUID.randomUUID().toString();

        CreativeDocument document = createEmptyCreativeDocument("testBundle", batchId, batchName, 1);

        return new CreativeDocumentBatch()
                .withName(batchName)
                .withItems(singletonList(document))
                .withTotal(1)
                .withDate(Date.from(now()))
                .withId(batchId)
                .withClientId(clientId)
                .withArchive(false);
    }
}
