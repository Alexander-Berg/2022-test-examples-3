package ru.yandex.market.mbo.cms.api.view.schemaeditor.converter;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.ChangesetApiDto;
import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.DocumentDescriptionApiDto;
import ru.yandex.market.mbo.cms.api.view.schemaeditor.schema.NodeTypeApiDto;
import ru.yandex.market.mbo.cms.core.models.ChangesetActionType;
import ru.yandex.market.mbo.cms.core.models.ChangesetSource;

import static org.junit.Assert.assertEquals;

public class ChangesetConverterTest {

    private static final String NODE_TYPE_1 = "node_type_1";
    private static final String DOCUMENT_TYPE_1 = "document_type_1";

    @Test
    public void testConvert() {
        NodeTypeApiDto nodeTypeApiDto = new NodeTypeApiDto(
            NODE_TYPE_1, null, null, null, null, null
        );
        DocumentDescriptionApiDto documentDescriptionApiDto = DocumentDescriptionApiDto
            .DocumentDescriptionApiDtoBuilder.aDocumentDescriptionApiDto()
            .withType(DOCUMENT_TYPE_1)
            .build();
        ChangesetApiDto changesetApiDto = new ChangesetApiDto(
            ChangesetActionType.MINUS.toString(),
            Collections.singletonList(nodeTypeApiDto),
            Collections.singletonList(documentDescriptionApiDto)
        );

        ChangesetApiDto converted = ChangesetConverter.convert(ChangesetConverter.convert(
            changesetApiDto,
            ChangesetSource.EDITOR
        ));

        assertEquals(NODE_TYPE_1, converted.getNodeTypes().get(0).getName());
        assertEquals(DOCUMENT_TYPE_1, converted.getDocuments().get(0).getType());
        assertEquals(ChangesetActionType.MINUS.toString(), converted.getActionType());
    }
}
