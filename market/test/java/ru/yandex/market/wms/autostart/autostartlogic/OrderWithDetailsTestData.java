package ru.yandex.market.wms.autostart.autostartlogic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;
import ru.yandex.market.wms.common.spring.dao.entity.OrderWithDetails;

import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000001003;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000001004;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000002002;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000003001;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000003002;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000003003;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000003004;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000004001;
import static ru.yandex.market.wms.autostart.AutostartTestData1.orderB000005002;

public interface OrderWithDetailsTestData {

    static OrderWithDetails owdB000002002() {
        return owd(orderB000002002(), l(
                OrderDetailTestData.b00000200200001ROV21()
        ));
    }

    static OrderWithDetails owdB000001004() {
        return owd(orderB000001004(), l(
                OrderDetailTestData.b00000100400001ROV11()
        ));
    }

    static OrderWithDetails owdB000001004SkuDups() {
        return owd(orderB000001004(), l(
                OrderDetailTestData.b00000100400001ROV11(),
                OrderDetailTestData.b00000100400001ROV11(),
                OrderDetailTestData.b00000100400001ROV11()
        ));
    }

    static OrderWithDetails owdB000001003() {
        return owd(orderB000001003(), l(
                OrderDetailTestData.b00000100300002ROV23(),
                OrderDetailTestData.b00000100300001ROV12(),
                OrderDetailTestData.b00000100300003ROV91()
        ));
    }

    static OrderWithDetails owdB000005002() {
        return owd(orderB000005002(), l(OrderDetailTestData.b00000500200001ROV81()));
    }

    static OrderWithDetails owdB000003001() {
        return owd(orderB000003001(), l(OrderDetailTestData.b00000300100001ROV31()));
    }

    static OrderWithDetails owdB000003002() {
        return owd(orderB000003002(), l(OrderDetailTestData.b00000300200001ROV41()));
    }

    static OrderWithDetails owdB000003004() {
        return owd(orderB000003004(), l(OrderDetailTestData.b00000300400001ROV61()));
    }

    static OrderWithDetails owdB000004001() {
        return owd(orderB000004001(), l(OrderDetailTestData.b00000400100001ROV71()));
    }

    static OrderWithDetails owdB000003003() {
        return owd(orderB000003003(), l(OrderDetailTestData.b00000300300001ROV51()));
    }

    static OrderWithDetails owd(Order o, List<OrderDetail> details) {
        return OrderWithDetails.builder().order(o).orderDetails(details).build();
    }

    static OrderWithDetails owdEmpty() {
        return owd(orderB000003003(), l(OrderDetailTestData.odEmpty()));
    }

    static <E> List<E> l(E e) {
        return Collections.singletonList(e);
    }

    @SafeVarargs
    static <E> List<E> l(E... e) {
        return Arrays.asList(e);
    }
}
