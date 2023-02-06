package ru.yandex.market.replenishment.autoorder.repository;

import java.sql.Timestamp;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.pdb.PdbInterWarehouseTruckError;
import ru.yandex.market.replenishment.autoorder.model.entity.pdb.PdbInterWarehouseTruckInfo;
import ru.yandex.market.replenishment.autoorder.repository.postgres.TruckInfoRepository;
public class TruckInfoRepositoryTest extends FunctionalTest {

    @Autowired
    private TruckInfoRepository truckInfoRepository;

    @Test
    @DbUnitDataSet(after = "TruckInfoRepositoryTest_info.after.csv")
    public void testSaveTruckInfos() {
        PdbInterWarehouseTruckInfo truckInfo = new PdbInterWarehouseTruckInfo();
        truckInfo.setAxOrderId("ax_order_1");
        truckInfo.setOrderId("123");
        truckInfo.setStatus(1);
        truckInfo.setStatusDate(Timestamp.valueOf("2019-06-10 12:10:11").toInstant());
        truckInfo.setStatusText("статус 1");
        truckInfo.setFfOrderId("ff_order_1");
        truckInfo.setFfReceiveOrderId("ff_receive_order_id_1");
        truckInfoRepository.saveTruckInfos(Collections.singletonList(truckInfo));
    }

    @Test
    @DbUnitDataSet(after = "TruckInfoRepositoryTest_error.after.csv")
    public void testSaveTruckError() {
        PdbInterWarehouseTruckError truckError = new PdbInterWarehouseTruckError();
        truckError.setErrorText("error_1");
        truckError.setMsku(123123L);
        truckError.setSsku("123123ssku");
        truckError.setOrderId("123");
        truckError.setSystemType(1);
        truckError.setCreatedDateTime(Timestamp.valueOf("2019-06-10 12:10:11").toInstant());
        truckError.setSupplierId(100500L);
        truckError.setErrorNum(1);
        truckInfoRepository.saveTruckErrors(Collections.singletonList(truckError));
    }
}
