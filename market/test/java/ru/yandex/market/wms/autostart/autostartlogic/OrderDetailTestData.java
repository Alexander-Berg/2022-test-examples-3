package ru.yandex.market.wms.autostart.autostartlogic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import ru.yandex.market.wms.common.spring.dao.entity.AssigmentType;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;

public interface OrderDetailTestData {

    static List<OrderDetail> sampleOrdersDetails() {
        return Arrays.asList(
                b00000100300001ROV12(),
                b00000100400001ROV11(),
                b00000100300002ROV23(),
                b00000200200001ROV21(),
                b00000300100001ROV31(),
                b00000300300001ROV51(),
                b00000400100001ROV71(),
                b00000100300003ROV91(),
                b00000500200001ROV81(),
                b00000300400001ROV61(),
                b00000300200001ROV41()
        );
    }


    static OrderDetail b00000100300001ROV12() {
        return template()
                .orderKey("000001003")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000001")
                .openQty(new BigDecimal("2.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000100300001ROV13() {
        return template()
                .orderKey("000001003")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000001")
                .openQty(new BigDecimal("3.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000100300002ROV23() {
        return template()
                .orderKey("000001003")
                .orderLineNumber("00002")
                .storerKey("100")
                .sku("ROV0000000000000000002")
                .openQty(new BigDecimal("3.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000100300003ROV91() {
        return template()
                .orderKey("000001003")
                .orderLineNumber("00003")
                .storerKey("100")
                .sku("ROV0000000000000000009")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000100400001ROV11() {
        return template()
                .orderKey("000001004")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000001")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000200200001ROV21() {
        return template()
                .orderKey("000002002")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000002")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000300100001ROV31() {
        return template()
                .orderKey("000003001")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000003")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000300200001ROV41() {
        return template()
                .orderKey("000003002")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000004")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000300300001ROV51() {
        return template()
                .orderKey("000003003")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000005")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000300400001ROV61() {
        return template()
                .orderKey("000003004")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000006")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.NON_SORTABLE_NON_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000400100001ROV71() {
        return template()
                .orderKey("000004001")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000007")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail b00000500200001ROV81() {
        return template()
                .orderKey("000005002")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000008")
                .openQty(new BigDecimal("1.00000"))
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail odEmpty() {
        return template()
                .orderKey("000003003")
                .orderLineNumber("00001")
                .storerKey("100")
                .sku("ROV0000000000000000005")
                .openQty(BigDecimal.ZERO)
                .assigmentType(AssigmentType.SORTABLE_CONVEYABLE)
                .build();
    }

    static OrderDetail.OrderDetailBuilder template() {
        return OrderDetail.builder()
                .isMaster(1)
                .rotation("1")
                .skuRotation("1")
                .packKey("STD")
                .cartonGroup("BC1")
                .shelfLife(BigDecimal.ZERO);
    }
}
