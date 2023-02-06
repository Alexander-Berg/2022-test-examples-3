package ru.yandex.market.logistics.lom.admin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.ydb.converter.BusinessProcessStateStatusHistoryYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение истории изменения статусов бизнес-процесса")
@DatabaseSetup("/controller/admin/business_process/prepare.xml")
public class GetBusinessProcessStateStatusHistoryTest extends AbstractContextualYdbTest {

    @Autowired
    private BusinessProcessStateStatusHistoryYdbConverter converter;

    @Autowired
    private BusinessProcessStateStatusHistoryTableDescription historyTable;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-08-30T11:12:13.00Z"), clock.getZone());
    }

    @Override
    @Nonnull
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(historyTable);
    }

    @Test
    @DisplayName("Получение истории изменения статусов бизнес-процесса. В ydb нет записей. Получаем пустой список")
    void getBusinessProcessStateStatusHistoryWithoutYdb() throws Exception {
        mockMvc.perform(get("/admin/business-processes/status-history").param("businessProcessStateId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/business_process/history/empty.json"));
    }

    @Test
    @DisplayName("Получение истории изменения статусов бизнес-процесса")
    void getBusinessProcessStateStatusHistory() throws Exception {
        insertAllIntoTable(historyTable, businessProcessStateStatusHistory(), converter::mapToItem);
        mockMvc.perform(get("/admin/business-processes/status-history").param("businessProcessStateId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/business_process/history/id_1.json"));
    }

    @Nonnull
    private BusinessProcessStateStatusHistoryYdb businessProcessStateStatusHistory(
        long id,
        long sequenceId,
        BusinessProcessStatus status,
        int day
    ) {
        return new BusinessProcessStateStatusHistoryYdb()
            .setId(id)
            .setSequenceId(sequenceId)
            .setStatus(status)
            .setMessage("message")
            .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd")
            .setCreated(clock.instant().plus(day, ChronoUnit.DAYS));
    }

    @Nonnull
    private List<BusinessProcessStateStatusHistoryYdb> businessProcessStateStatusHistory() {
        return List.of(
            businessProcessStateStatusHistory(1, 1001, BusinessProcessStatus.ENQUEUED, 1),
            businessProcessStateStatusHistory(1, 1001, BusinessProcessStatus.SYNC_PROCESS_SUCCEEDED, 2),
            businessProcessStateStatusHistory(2, 1002, BusinessProcessStatus.ENQUEUED, 2)
        );
    }
}
