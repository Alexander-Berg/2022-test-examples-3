package ru.yandex.market.psku.postprocessor.service.tracker.processing;

import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuTrackerTicketType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.TrackerTicketPskuStatus;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.IssueMock;

import java.sql.Timestamp;
import java.time.Instant;

public interface ProcessingStrategyTest {
    String MBO_ENTITY_BASE_URL =
            "https://mbo-testing.market.yandex.ru/gwt/#modelEditor/entity-id=";
    String MBOC_SUPPLIERS_BASE_URL =
            "http://mbo-http-exporter.tst.vs.market.yandex.net:8084/mboUsers/";
    long COMPONENT_ID = 12345L;

    long CATEGORY_ID = 1L;
    String CATEGORY_NAME = "Category name";
    long PSKU_ID_2 = 2L;
    long PSKU_ID_3 = 3L;
    String PSKU_NAME = "PSKU name";
    String USER_FULL_NAME = "User full name";
    long PSKU_ID_4 = 4L;
    long SUPLIER_ID = 5L;
    String MAPPING_ID = "6";
    long MSKU_ID = 7L;
    String MSKU_NAME = "MSKU name";
    long PSKU_ID_8 = 8L;

    String getExpectedTitle();

    default IssueMock getDefaultTicket() {
        IssueMock defaultTicket = new IssueMock();

        defaultTicket.setKey("PSKUSUPTEST-123");
        defaultTicket.setIssueStatus(IssueStatus.OPEN);
        defaultTicket.setSummary(getExpectedTitle());

        return defaultTicket;
    }

    default TrackerTicketPskuStatus createTrackerTicketPskuStatus(String ticketKey,
                                                                  long pskuId,
                                                                  PskuTrackerTicketType type) {
        TrackerTicketPskuStatus pskuStatus = new TrackerTicketPskuStatus();

        pskuStatus.setTrackerTicketKey(ticketKey);
        pskuStatus.setPskuId(pskuId);
        pskuStatus.setTicketType(type);
        pskuStatus.setIsClosed(false);

        return pskuStatus;
    }

    default PskuResultStorage getPskuResultStorage(long pskuId, PskuStorageState storageState) {
        PskuResultStorage pskuResultStorage = new PskuResultStorage();
        pskuResultStorage.setPskuId(pskuId);
        pskuResultStorage.setCategoryId(CATEGORY_ID);
        pskuResultStorage.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorage.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorage.setState(storageState);
        return pskuResultStorage;
    }
}
