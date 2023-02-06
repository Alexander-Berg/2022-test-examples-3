package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.PickDetailStatus;
import ru.yandex.market.wms.common.model.enums.PickMethod;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class PickDetailDaoTest extends IntegrationTest {

    @Autowired
    private PickDetailDao dao;

    @Test
    @DatabaseSetup("/db/dao/pick-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/pick-detail/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void createPickDetails() {
        PickDetail pickDetail = PickDetail.builder()
                .pickDetailKey("pd-001")
                .caseId("P00031")
                .pickHeaderkey(" ")
                .orderKey("order-001")
                .orderLineNumber("00001")
                .lot("LOT-001")
                .sku("SKU-001")
                .storerKey("STORER-001")
                .packKey("STD")
                .uom("6")
                .uomQty(BigDecimal.ONE)
                .qty(BigDecimal.ONE)
                .loc("UNKNOWN")
                .toLoc("PICKTO")
                .id("")
                .cartonGroup("SHIPPABLE")
                .cartonType("BC1")
                .doReplenish("N")
                .doCartonize("Y")
                .replenishZone(" ")
                .pickMethod(PickMethod.TASK_DIRECTED)
                .waveKey("WAVE-001")
                .statusRequied("OK")
                .selectedCartonType("SHIPPABLE")
                .selectedCartonId("P00031")
                .fromLoc("UNKNOWN")
                .assignmentNumber("001")
                .pickContPlacement("WAVECASE")
                .equipment("BC1")
                .status(PickDetailStatus.RELEASED)
                .build();

        dao.insertOrderStart(Collections.singletonList(pickDetail), "TEST");
    }
}
