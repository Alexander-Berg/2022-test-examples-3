package ru.yandex.market.mbo.cms.api.view.schemaeditor.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.ChangesetApiDto;
import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.ChangesetInfoApiDto;
import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.ChangesetWithInfoApiDto;
import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.ChangesetsExportApiDto;
import ru.yandex.market.mbo.cms.core.models.ChangesetActionType;
import ru.yandex.market.mbo.cms.core.models.ChangesetSource;

import static org.junit.Assert.assertEquals;

public class ChangesetExportConverterTest {

    private static final String NODE_TYPE_1 = "node_type_1";
    private static final String DOCUMENT_TYPE_1 = "document_type_1";

    @Test
    public void testConvert() {
        ChangesetWithInfoApiDto changeset1 = new ChangesetWithInfoApiDto(
            new ChangesetInfoApiDto(1, null, 1, ChangesetSource.OTHER),
            new ChangesetApiDto(ChangesetActionType.PLUS.toString(), Collections.emptyList(), Collections.emptyList())
        );
        ChangesetWithInfoApiDto changeset2 = new ChangesetWithInfoApiDto(
            new ChangesetInfoApiDto(2, null, 2, ChangesetSource.API),
            new ChangesetApiDto(ChangesetActionType.MINUS.toString(), Collections.emptyList(), Collections.emptyList())
        );

        List<ChangesetWithInfoApiDto> changesets = new ArrayList<>();
        changesets.add(changeset1);
        changesets.add(changeset2);

        ChangesetsExportApiDto changesetsExportApiDto = new ChangesetsExportApiDto(changesets);

        ChangesetsExportApiDto converted = ChangesetExportConverter.convert(
            ChangesetExportConverter.convert(changesetsExportApiDto)
        );

        assertEquals(
            ChangesetActionType.PLUS.toString(), converted.getChangesets().get(0).getChangeset().getActionType()
        );
        assertEquals(0, converted.getChangesets().get(0).getInfo().getId());
        assertEquals(0, converted.getChangesets().get(0).getInfo().getCreatorId());
        assertEquals(ChangesetSource.EDITOR, converted.getChangesets().get(0).getInfo().getSource());

        assertEquals(
            ChangesetActionType.MINUS.toString(), converted.getChangesets().get(1).getChangeset().getActionType()
        );
        assertEquals(0, converted.getChangesets().get(1).getInfo().getId());
        assertEquals(0, converted.getChangesets().get(1).getInfo().getCreatorId());
        assertEquals(ChangesetSource.EDITOR, converted.getChangesets().get(1).getInfo().getSource());
    }
}
