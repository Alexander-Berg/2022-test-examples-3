package ru.yandex.market.wms.common.spring.dao;

import ru.yandex.market.wms.common.model.enums.RotationType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeCodeType;

public class AssertSkuExpectedParamThree {
    private final RotationType rotationType;
    private final ShelfLifeCodeType shelfLifeCodeType;
    private final String boxCount;
    private final String name;

    public AssertSkuExpectedParamThree(RotationType rotationType, ShelfLifeCodeType shelfLifeCodeType, String boxCount,
                                       String name) {
        this.rotationType = rotationType;
        this.shelfLifeCodeType = shelfLifeCodeType;
        this.boxCount = boxCount;
        this.name = name;
    }

    public RotationType getRotationType() {
        return rotationType;
    }

    public ShelfLifeCodeType getShelfLifeCodeType() {
        return shelfLifeCodeType;
    }

    public String getBoxCount() {
        return boxCount;
    }

    public String getName() {
        return name;
    }
}
