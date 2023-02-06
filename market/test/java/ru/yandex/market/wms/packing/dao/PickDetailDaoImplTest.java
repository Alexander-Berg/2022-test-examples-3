package ru.yandex.market.wms.packing.dao;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.PickDetailStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;

import static java.util.Arrays.asList;

public class PickDetailDaoImplTest extends IntegrationTest {

    @Autowired
    private PickDetailDaoImpl pickDetailDao;

    @Test
    @DatabaseSetup("/db/dao/pick-detail/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/pick-detail/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void getPickDetailsStatusesByParcelIdTest_empty() {
        List<PickDetailStatus> statuses = pickDetailDao.getPickDetailsStatusesByParcelId("P000000502");

        Assertions.assertEquals(Collections.emptyList(), statuses);
    }

    @Test
    @DatabaseSetup("/db/dao/pick-detail/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/pick-detail/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void getPickDetailsStatusesByParcelIdTest() {
        List<PickDetailStatus> expectedStatuses = asList(PickDetailStatus.NORMAL, PickDetailStatus.RELEASED,
                PickDetailStatus.IN_PROCESS, PickDetailStatus.PICKED, PickDetailStatus.LOADED,
                PickDetailStatus.SHIPPED);

        List<PickDetailStatus> statuses = pickDetailDao.getPickDetailsStatusesByParcelId("P000000501");

        Assertions.assertEquals(expectedStatuses, statuses);
    }
}
