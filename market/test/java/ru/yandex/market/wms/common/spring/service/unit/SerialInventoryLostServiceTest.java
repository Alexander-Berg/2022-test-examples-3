package ru.yandex.market.wms.common.spring.service.unit;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.enums.LostType;
import ru.yandex.market.wms.common.spring.service.SerialInventoryLostService;

@DatabaseSetup(value = "/db/dao/serial-inventory-lost/lost-db.xml")
public class SerialInventoryLostServiceTest extends IntegrationTest {

    @Autowired
    SerialInventoryLostService lostService;

    @Test
    public void getFixLost() {
        List<SerialInventory> fixLosts = lostService.getLost(LostType.FIX);
        assertions.assertThat(fixLosts.size()).isEqualTo(2);
        assertions.assertThat(fixLosts.get(0).getSerialNumber()).isIn("SERIAL_2", "SERIAL_5");
        assertions.assertThat(fixLosts.get(1).getSerialNumber()).isIn("SERIAL_2", "SERIAL_5");
        assertions.assertThat(fixLosts.get(0).getSerialNumber()).isNotEqualTo(fixLosts.get(1).getSerialNumber());
    }

    @Test
    public void getOperLost() {
        List<SerialInventory> operLost = lostService.getLost(LostType.OPER);
        assertions.assertThat(operLost.size()).isEqualTo(5);
        for (int i = 1; i < 6; i++) {
            int finalI = i;
            assertions.assertThat(operLost.stream()
                    .filter(a -> a.getSerialNumber().equals(String.format("SERIAL_%d", finalI))).findAny()).isPresent();
        }
    }
}
