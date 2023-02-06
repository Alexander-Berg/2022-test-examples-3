package ru.yandex.canvas.steps;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.CreativeDocument;

public class CreativeDocumentSteps {
    @NotNull
    public static CreativeDocument createEmptyCreativeDocument(String bundleName) {
        return createEmptyCreativeDocument(bundleName, 1);
    }

    @NotNull
    public static CreativeDocument createEmptyCreativeDocument(String bundleName, Integer presetId) {
        String batchId = "batchId";
        String batchName = "Test CreativeDocument";
        return createEmptyCreativeDocument(bundleName, batchId, batchName, presetId);
    }

    @NotNull
    public static CreativeDocument createEmptyCreativeDocument(String bundleName, String batchId, String batchName,
                                                               Integer presetId) {
        Bundle bundle = new Bundle();
        bundle.setName(bundleName);
        bundle.setVersion(1);

        CreativeData.Options options = new CreativeData.Options();
        options.setIsAdaptive(false);

        CreativeData data = new CreativeData();
        data.setElements(new ArrayList<>());
        data.setHeight(10);
        data.setWidth(10);
        data.setMediaSets(new HashMap<>());
        data.setBundle(bundle);
        data.setOptions(options);

        CreativeDocument creative = new CreativeDocument().withPresetId(presetId);
        creative.setData(data);
        creative.setId(0L);
        creative.setBatchId(batchId);
        creative.setDate(Date.from(Instant.now()));
        creative.setName(batchName);
        return creative;
    }
}
