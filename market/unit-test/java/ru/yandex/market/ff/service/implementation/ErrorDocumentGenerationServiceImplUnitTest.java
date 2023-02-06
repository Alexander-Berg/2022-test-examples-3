package ru.yandex.market.ff.service.implementation;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.model.row.GridRow;
import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.reader.GridReaderProvider;
import ru.yandex.market.ff.grid.writer.GridWriter;
import ru.yandex.market.ff.grid.writer.GridWriterProvider;
import ru.yandex.market.ff.i18n.TemplateValidationMessages;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.entity.ShopRequestDocument;
import ru.yandex.market.ff.service.ErrorDocumentGenerationService;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.ff.service.MdsS3Service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link RequestItemErrorType}, {@link ErrorDocumentGenerationService}.
 */
@ExtendWith(MockitoExtension.class)
class ErrorDocumentGenerationServiceImplUnitTest {

    @Mock
    private GridReader gridReader;

    @Mock
    private GridReaderProvider gridReaderProvider;

    @Mock
    private GridWriterProvider gridWriterProvider;

    @Mock
    private Grid grid;

    @Mock
    private GridRow gridRow;

    @Mock
    private GridCell gridCell;

    @Mock
    private FileExtension fileExtension;

    @Mock
    private ShopRequestDocument shopRequestDocument;

    @Test
    void generateDocumentFileWithAllErrorTypes() {
        Map<FileExtension, String> errorDelimiters = Map.of(fileExtension, "");
        ErrorDocumentGenerationService service = new ErrorDocumentGenerationServiceImpl(
                gridReaderProvider, mock(MdsS3Service.class), gridWriterProvider,
                mock(TemplateValidationMessages.class), errorDelimiters, mock(FulfillmentInfoService.class)
        );
        when(gridReaderProvider.provide(any())).thenReturn(gridReader);
        when(gridWriterProvider.provide(any())).thenReturn(mock(GridWriter.class));
        when(gridReader.read(any(InputStream.class))).thenReturn(grid);
        when(grid.getNumberOfRows()).thenReturn(1);
        when(grid.getRow(anyInt())).thenReturn(gridRow);
        when(gridRow.getCell(anyInt())).thenReturn(gridCell);
        when(gridCell.getRawValue()).thenReturn(Optional.of("BBBBBBBBBBBBBBBBBB"));
        when(shopRequestDocument.getExtension()).thenReturn(fileExtension);

        var errorContainer = new EnrichmentResultContainer(2L);
        Arrays.stream(RequestItemErrorType.values()).forEach(type -> {
            if (type == RequestItemErrorType.NOT_ENOUGH_ON_STOCK) {
                errorContainer.addValidationError(type, Map.of(RequestItemErrorAttributeType.CURRENTLY_ON_STOCK, "5"));
            } else {
                errorContainer.addValidationError(type);
            }
        });
        InputStream is = service.generateDocumentFile(shopRequestDocument,
                Map.of("BBBBBBBBBBBBBBBBBB", errorContainer), false);
        assertNotNull(is);
    }
}
