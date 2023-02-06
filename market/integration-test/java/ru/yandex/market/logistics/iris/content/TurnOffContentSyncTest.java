package ru.yandex.market.logistics.iris.content;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.model.index.ReferenceIndexMergePayloadItem;
import ru.yandex.market.logistics.iris.jobs.model.index.ReferenceIndexMergeQueuePayload;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemNaturalKeyDTO;
import ru.yandex.market.logistics.iris.model.SourceDTO;
import ru.yandex.market.logistics.iris.service.index.ReferenceIndexMergeServiceImpl;
import ru.yandex.market.request.trace.RequestContextHolder;

public class TurnOffContentSyncTest extends AbstractContextualTest {

    private final static String SMALL_PAYLOAD =
            "{\"msku\" : {\"value\" : 666, \"utcTimestamp\" : \"1970-01-02T00:00:00\"} , " +
                    "\"name\": {\"value\": \"aboba\",\"utcTimestamp\": \"1970-01-02T00:00:00\"}}";

    @Autowired
    private ReferenceIndexMergeServiceImpl referenceIndexMergeService;

    @Before
    public void init() {
        RequestContextHolder.clearContext();
    }


    @Test
    @DatabaseSetup("classpath:fixtures/setup/content/turnoff/2.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/content/turnoff/2.xml")
    public void shouldCreatePutReferenceItem() {
        ReferenceIndexMergeQueuePayload payload = createPayload(SMALL_PAYLOAD, getDataCampMskuSource());

        referenceIndexMergeService.processPayload(payload);
    }

    @Nonnull
    private SourceDTO getDataCampMskuSource() {
        return new SourceDTO("1", SourceType.DATACAMP_MSKU);
    }


    private ReferenceIndexMergeQueuePayload createPayload(String payload, SourceDTO sourceDTO) {
        return new ReferenceIndexMergeQueuePayload(
                null,
                createPayloadItems(payload, sourceDTO)
        );
    }

    private List<ReferenceIndexMergePayloadItem> createPayloadItems(String payload, SourceDTO sourceDTO) {
        return Collections.singletonList(createPayloadItem(payload, sourceDTO));
    }

    private ReferenceIndexMergePayloadItem createPayloadItem(String payload, SourceDTO sourceDTO) {
        return new ReferenceIndexMergePayloadItem(payload, getKey(sourceDTO));
    }

    @Nonnull
    private ItemNaturalKeyDTO getKey(SourceDTO sourceDTO) {
        return new ItemNaturalKeyDTO(
                new ItemIdentifierDTO("1", "partner_sku_1"),
                sourceDTO
        );
    }
}
