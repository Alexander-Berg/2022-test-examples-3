package ru.yandex.market.sc.core.util.flow.xdoc;

import java.util.Set;

import javax.persistence.EntityManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.model.CreateLotRequest;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class LotCreation {

    private final TestFactory testFactory;
    private final LotCommandService lotCommandService;
    private final EntityManager entityManager;

    private Cell bufferCell;

    public LotCreation bufferCell(Cell bufferCell) {
        this.bufferCell = bufferCell;
        return this;
    }

    @Transactional
    public SortableLot createAndGet() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        validateCellType();
        var savedLot = lotCommandService.createLot(
                new CreateLotRequest(SortableType.XDOC_BASKET, bufferCell, sc,
                        false, LotStatus.CREATED, null, null, null, null, null, null, false)
        );
        savedLot.setLotStatus(LotStatus.CREATED, null);
        entityManager.flush();
        return savedLot;
    }

    private void validateCellType() {
        if (bufferCell == null ||
                !Set.of(CellSubType.BUFFER_XDOC_LOCATION, CellSubType.BUFFER_XDOC).contains(bufferCell.getSubtype())) {
            throw new IllegalArgumentException("Lot should be created in the buffer xdoc cell");
        }
    }
}
