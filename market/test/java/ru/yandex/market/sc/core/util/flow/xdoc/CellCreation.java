package ru.yandex.market.sc.core.util.flow.xdoc;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.cell.CellCreator;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CellCreation {

    private final CellCreator cellCreator;
    private final TestFactory testFactory;

    private String number;
    private CellType type;
    private CellSubType subType;
    private String warehouseYandexId;

    public CellCreation number(String number) {
        this.number = number;
        return this;
    }

    public CellCreation type(CellType type) {
        this.type = type;
        return this;
    }

    public CellCreation subType(CellSubType subType) {
        this.subType = subType;
        return this;
    }

    public CellCreation warehouseYandexId(String warehouseYandexId) {
        this.warehouseYandexId = warehouseYandexId;
        return this;
    }

    public Cell createAndGet() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        return cellCreator.createCell(new Cell(sc, number, CellStatus.ACTIVE, type,
                subType, false, warehouseYandexId, null, null, null, null, null, null, null, null));
    }
}
