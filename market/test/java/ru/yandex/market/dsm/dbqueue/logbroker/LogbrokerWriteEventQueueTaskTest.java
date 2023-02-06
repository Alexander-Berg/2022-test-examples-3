package ru.yandex.market.dsm.dbqueue.logbroker;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.dsm.core.test.AbstractTest;
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil;
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue;
import ru.yandex.market.dsm.domain.courier.model.CourierType;
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory;
import ru.yandex.market.dsm.domain.employer.service.EmployerService;
import ru.yandex.market.dsm.test.TestUtil;
import ru.yandex.mj.generated.server.model.EmployerUpsertDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LogbrokerWriteEventQueueTaskTest extends AbstractTest {

    private final DbQueueTestUtil dbQueueTestUtil;
    private final EmployerService employerService;
    private final CourierTestFactory courierTestFactory;

    @BeforeEach
    void setUp() {
        dbQueueTestUtil.clear(DsmDbQueue.LOGBROKER_WRITE_EVENT);
    }

    @Test
    void task_execution_employer_courier() {
        EmployerUpsertDto upsertDto = TestUtil.Companion.getOBJECT_GENERATOR().nextObject(EmployerUpsertDto.class);
        upsertDto.setId(null);

        employerService.createEmployer(upsertDto);

        courierTestFactory.create(upsertDto.getId(), "48929839", "test364564", CourierType.PARTNER, false);

        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.LOGBROKER_WRITE_EVENT, 2);
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.LOGBROKER_WRITE_EVENT);
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.LOGBROKER_WRITE_EVENT, 0);
    }

    @Test
    void courierUpsert_skipMessageForAutoTestUsers() {
        EmployerUpsertDto upsertDto = TestUtil.Companion.getOBJECT_GENERATOR().nextObject(EmployerUpsertDto.class);
        upsertDto.setId(null);
        employerService.createEmployer(upsertDto);

        courierTestFactory.create(
                upsertDto.getId(), "48929840", "tpl-auto-user-1@yandex.ru", CourierType.PARTNER, false
        );

        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.LOGBROKER_WRITE_EVENT, 1);
    }

    @Test
    void task_producing_employer_courier() {
        EmployerUpsertDto upsertDto = TestUtil.Companion.getOBJECT_GENERATOR().nextObject(EmployerUpsertDto.class);
        upsertDto.setId(null);

        employerService.createEmployer(upsertDto);

        courierTestFactory.create(upsertDto.getId(), "48929838", "test364565", CourierType.PARTNER, false);

        dbQueueTestUtil.assertQueueLogHasSize(DsmDbQueue.LOGBROKER_WRITE_EVENT, 2);
    }

}
