package ru.yandex.market.sc.core.domain.cell;

import lombok.Builder;

import ru.yandex.market.sc.core.domain.cell.model.CellCargoType;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;

public record CellField(
        CellType type,
        CellSubType subType,
        CellStatus status,
        boolean deleted,
        String warehouseYandexId,
        Long courier,
        Zone zone,
        Long sequenceNumber,
        Long alleyNumber,
        Long sectionNumber,
        Long levelNumber,
        Long lotsCapacity,
        CellCargoType cargoType
) {
    @Builder
    public CellField {
    }
}
