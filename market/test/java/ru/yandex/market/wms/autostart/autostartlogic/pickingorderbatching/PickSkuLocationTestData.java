package ru.yandex.market.wms.autostart.autostartlogic.pickingorderbatching;

import ru.yandex.market.wms.common.spring.dao.entity.PickSkuLocation;

public interface PickSkuLocationTestData {

    static PickSkuLocation c4100001(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("FLOOR")
                .loc("C4-10-0001")
                .logicalLocation("100001")
                .build();
    }

    static PickSkuLocation c4101001(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("FLOOR")
                .loc("C4-10-1001")
                .logicalLocation("101001")
                .build();
    }

    static PickSkuLocation c4110003(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-11-0003")
                .logicalLocation("110003")
                .build();
    }

    static PickSkuLocation c4120005(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-12-0005")
                .logicalLocation("120005")
                .build();
    }

    static PickSkuLocation c4130007(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-13-0007")
                .logicalLocation("130007")
                .build();
    }

    static PickSkuLocation c4140009(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-14-0009")
                .logicalLocation("140009")
                .build();
    }

    static PickSkuLocation c4160008(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-16-0008")
                .logicalLocation("160008")
                .build();
    }

    static PickSkuLocation c4170006(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-17-0006")
                .logicalLocation("170006")
                .build();
    }

    static PickSkuLocation c4180004(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C4-18-0004")
                .logicalLocation("180004")
                .build();
    }

    static PickSkuLocation c4190002(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("FLOOR")
                .loc("C4-19-0002")
                .logicalLocation("190002")
                .build();
    }

    static PickSkuLocation c5200001(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C5-20-0001")
                .logicalLocation("5200001")
                .build();
    }

    static PickSkuLocation c5300001(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C5-30-0001")
                .id("SOME_ID")
                .logicalLocation("5300001")
                .build();
    }

    static PickSkuLocation c5300002(String lot) {
        return pickSkuLocationTemplate(lot)
                .zone("MEZONIN_2")
                .loc("C5-30-0002")
                .logicalLocation("5300002")
                .build();
    }

    static PickSkuLocation.PickSkuLocationBuilder pickSkuLocationTemplate(String lot) {
        return PickSkuLocation.builder()
                .lot(lot)
                .id("")
                .lottable08("1");
    }
}
