package ru.yandex.market.ff.grid;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.grid.DefaultGrid;
import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.validation.GridValidationResult;
import ru.yandex.market.ff.grid.validation.GridValidator;
import ru.yandex.market.ff.grid.validation.Violation;
import ru.yandex.market.ff.grid.validation.ViolationsContainer;
import ru.yandex.market.ff.model.entity.UploadError;
import ru.yandex.market.ff.service.ErrorDocumentGenerationService;
import ru.yandex.market.ff.service.UploadErrorService;
import ru.yandex.market.ff.service.exception.GridValidationException;
import ru.yandex.market.ff.service.implementation.GridServiceImpl;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.client.enums.DocumentType.SUPPLY;

/**
 * @author kotovdv 07/08/2017.
 */
@ExtendWith(MockitoExtension.class)
class GridServiceImplTest {

    private static final Grid EMPTY_GRID = new DefaultGrid(emptyMap());
    @InjectMocks
    private GridServiceImpl gridFacade;

    @Mock
    private GridValidator gridValidator;

    @Mock
    private ErrorDocumentGenerationService errorDocumentGenerationService;

    @Mock
    private UploadErrorService uploadErrorService;

    @BeforeEach
    void init() {
        when(uploadErrorService.create(any(), any(), any())).thenReturn(new UploadError());
    }

    @Test
    void testFailedGridValidation() {
        Assertions.assertThrows(GridValidationException.class, () -> {
            final ViolationsContainer container = filledContainer();
            given(gridValidator.validate(SUPPLY, EMPTY_GRID)).willReturn(new GridValidationResult(container, null));

            try {
                gridFacade.validateAndGetTemplate(EMPTY_GRID, SUPPLY, FileExtension.CSV, RequestType.SUPPLY);
            } finally {
                verify(gridValidator, times(1)).validate(SUPPLY, EMPTY_GRID);
                verify(errorDocumentGenerationService)
                        .generateDocumentFile(EMPTY_GRID, FileExtension.CSV, container.getCellViolations(),
                                RequestType.SUPPLY);
            }
        });
    }

    private ViolationsContainer filledContainer() {
        ViolationsContainer violationsContainer = new ViolationsContainer();
        violationsContainer.add(0, new Violation("Missing"));
        violationsContainer.add(new DefaultGridCell(0, 0, null), new Violation("Error"));

        return violationsContainer;
    }
}
