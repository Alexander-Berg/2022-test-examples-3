package ru.yandex.market.wms.common.spring.dao;

import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.WaveDetails;
import ru.yandex.market.wms.common.spring.dao.implementation.WaveDetailDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class WaveDetailDaoTest extends IntegrationTest {

    @Autowired
    private WaveDetailDao dao;

    @Test
    @DatabaseSetup("/db/dao/wave-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/wave-detail/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void createWaveDetails() {
        WaveDetails waveDetails = WaveDetails.builder()
                .waveKey("WAVE-001")
                .waveDetailKey("DETAIL-001")
                .orderKey("ORDER-001")
                .build();

        dao.create(Collections.singletonList(waveDetails), "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/wave-detail/delete-by-orderkeys/before.xml")
    @ExpectedDatabase(value = "/db/dao/wave-detail/delete-by-orderkeys/after.xml", assertionMode = NON_STRICT)
    void deleteDetailByOrderKeys() {
        dao.deleteDetailByOrderKeys(Arrays.asList("ORDER-001", "ORDER-002", "ORDER-003"));
    }
}
