package ru.yandex.market.b2b.clients.quartz.task;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.common.ForwardableDocumentStatus;
import ru.yandex.market.b2b.clients.impl.DocumentDaoImpl;
import ru.yandex.market.b2b.clients.impl.ForwardableDocumentDaoImpl;
import ru.yandex.market.b2b.clients.impl.dto.ForwardableDocumentDto;
import ru.yandex.market.b2b.logbroker.yadoc.YaDocDocumentLogbrokerEventPublisher;
import ru.yandex.mj.generated.server.model.DocumentDto;

public class ForwardDocumentTaskTest extends AbstractFunctionalTest {
    @Autowired
    private ForwardDocumentTask task;
    @Autowired
    private DocumentDaoImpl documentDao;
    @Autowired
    private ForwardableDocumentDaoImpl forwardableDocumentDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private YaDocDocumentLogbrokerEventPublisher publisherMock;

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "document", "document_forwardable");
        Mockito.reset(publisherMock);
    }

    @Test
    public void testPublishSimple() {
        // подготовка документа на отправку
        List<ForwardableDocumentDto> result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, result.size());

        DocumentDto originDocument = Documents.random(true);
        documentDao.save(List.of(originDocument));
        forwardableDocumentDao.savePending(List.of(originDocument));

        // запуск таска
        task.start();

        // проверка отправки
        Mockito.verify(publisherMock, Mockito.times(1))
                .publishEvent(Mockito.any());

        // проверка статусов
        List<Map<String, Object>> forwardableDocuments =
                jdbcTemplate.queryForList("SELECT forward_status FROM document_forwardable");
        Assertions.assertEquals(1, forwardableDocuments.size());
        Assertions.assertEquals(
                ForwardableDocumentStatus.FORWARDED.name(),
                forwardableDocuments.get(0).get("forward_status"));
    }

    @Test
    public void testPublishTwice() {
        // подготовка документа на отправку
        List<ForwardableDocumentDto> result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, result.size());

        DocumentDto originDocument1 = Documents.random(true);
        DocumentDto originDocument2 = Documents.random(true);
        documentDao.save(List.of(originDocument1, originDocument2));
        forwardableDocumentDao.savePending(List.of(originDocument1, originDocument2));

        // запуск таска
        task.setGetLimit(1);
        task.start();

        // проверка отправки
        Mockito.verify(publisherMock, Mockito.times(2))
                .publishEvent(Mockito.any());

        // проверка статусов
        List<String> statuses =
                jdbcTemplate.queryForList("SELECT forward_status FROM document_forwardable", String.class);
        Assertions.assertEquals(2, statuses.size());
        statuses.forEach(status -> Assertions.assertEquals(ForwardableDocumentStatus.FORWARDED.name(), status));
    }

    @Test
    public void testPublishAndError() {
        // подготовка документа на отправку
        List<ForwardableDocumentDto> result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, result.size());

        DocumentDto originDocument1 = Documents.random(true);
        DocumentDto originDocument2 = Documents.random(true);
        DocumentDto originDocument3 = Documents.random(true);
        DocumentDto originDocument4 = Documents.random(true);
        documentDao.save(List.of(originDocument1, originDocument2, originDocument3, originDocument4));
        forwardableDocumentDao.savePending(List.of(originDocument1, originDocument2, originDocument3, originDocument4));

        // упасть на публикации второго документа
        Mockito
                .doThrow(RuntimeException.class)
                .when(publisherMock)
                .publishEvent(Mockito.argThat(a -> a.getPayload().getNumber().equals(originDocument4.getNumber())));
        task.setGetLimit(2);
        // запуск таска
        Assertions.assertThrows(RuntimeException.class, () -> task.start());

        // проверка отправки
        Mockito.verify(publisherMock, Mockito.times(4))
                .publishEvent(Mockito.any());

        // проверка статусов
        List<String> forwardedStatuses = jdbcTemplate.queryForList(
                "SELECT forward_status FROM document_forwardable WHERE number != ?",
                new Object[]{originDocument4.getNumber()},
                String.class);
        String pendigsStatus = jdbcTemplate.queryForObject(
                "SELECT forward_status FROM document_forwardable WHERE number = ?",
                new Object[]{originDocument4.getNumber()},
                String.class);

        Assertions.assertNotNull(pendigsStatus);
        Assertions.assertEquals(3, forwardedStatuses.size());
        forwardedStatuses.forEach(s -> Assertions.assertEquals(ForwardableDocumentStatus.FORWARDED.name(), s));
        Assertions.assertEquals(ForwardableDocumentStatus.PENDING.name(), pendigsStatus);
    }
}
