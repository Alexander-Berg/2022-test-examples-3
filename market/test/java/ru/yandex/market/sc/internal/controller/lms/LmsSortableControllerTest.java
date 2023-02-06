package ru.yandex.market.sc.internal.controller.lms;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcode;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LmsSortableControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;

    @Test
    @SneakyThrows
    void getSortablesTest() {
        var sortingCenter = testFactory.storedSortingCenter(989L, "ГрузоместаСЦ");
        var cell = testFactory.storedCell(sortingCenter, "000", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.READY, false);

        var resultActions = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortables")
        );
        expectSortable(resultActions, lot.getSortable());
    }

    @Test
    @SneakyThrows
    void getSortablesWithBarcodeFilterTest() {
        var sortingCenter = testFactory.storedSortingCenter(989L, "ГрузоместаСЦ");
        var cell = testFactory.storedCell(sortingCenter, "000", CellType.RETURN);
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.READY, false);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.READY, false);
        var lot3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell, LotStatus.READY, false);

        var resultActions = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/LMS/sortingCenter/sortables")
                        .param("barcodes", lot.getBarcode())
        );
        expectSortable(resultActions, lot.getSortable());
    }

    @SneakyThrows
    private void expectSortable(ResultActions resultActions, Sortable lot) {
        resultActions.andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath(
                        "$.items[0].id",
                        equalTo(Integer.parseInt(lot.getId().toString()))))
                .andExpect(jsonPath(
                        "$.items[0].values.barcodes",
                        equalTo(lot.getBarcodes().stream()
                                .map(SortableBarcode::getBarcode)
                                .toList()))
                )
                .andExpect(jsonPath(
                        "$.items[0].values.status",
                        equalTo(lot.getStatus().toString()))
                )
                .andExpect(jsonPath(
                        "$.items[0].values.sortingCenterId",
                        equalTo(Integer.parseInt(lot.getSortingCenter().getId().toString()))
                ));
    }
}
