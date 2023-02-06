package ru.yandex.market.b2b.clients.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.Randoms;
import ru.yandex.market.b2b.clients.common.ForwardableDocumentStatus;
import ru.yandex.market.b2b.clients.impl.dto.ForwardableDocumentDto;
import ru.yandex.mj.generated.server.model.DocumentDto;

public class ForwardableDocumentDaoImplTest extends AbstractFunctionalTest {

    @Autowired
    DocumentDaoImpl documentDao;
    @Autowired
    ForwardableDocumentDaoImpl forwardableDocumentDao;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "document", "document_forwardable");
    }

    @Test
    public void testSaveAndGet() {
        List<ForwardableDocumentDto> result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, result.size());

        DocumentDto originDocument = Documents.random(true);
        documentDao.save(List.of(originDocument));
        forwardableDocumentDao.savePending(List.of(originDocument));

        result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(1, result.size());

        ForwardableDocumentDto actualDocument = result.get(0);
        Documents.assertEquals(originDocument, actualDocument);
        Assertions.assertNotNull(actualDocument.getId());
        Assertions.assertEquals(ForwardableDocumentStatus.PENDING, actualDocument.getForwardStatus());
    }

    @Test
    public void testSaveAndGet_diffMetaForOneOrder() {
        List<ForwardableDocumentDto> result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, result.size());

        DocumentDto originDocument = Documents.random(true);
        Map<String, Object> meta1 = new HashMap<>(originDocument.getMeta());
        Map<String, Object> meta2 = new HashMap<>(originDocument.getMeta());

        meta2.put("key1", Randoms.string());
        meta2.put("key2", Randoms.string());
        originDocument.setMeta(meta1);
        documentDao.save(List.of(originDocument));
        forwardableDocumentDao.savePending(List.of(originDocument));

        meta2.put("key1", Randoms.string());
        meta2.put("key2", Randoms.string());
        originDocument.setMeta(meta2);
        documentDao.save(List.of(originDocument));
        forwardableDocumentDao.savePending(List.of(originDocument));

        result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(2, result.size());

        originDocument.setMeta(meta1);
        ForwardableDocumentDto actualDocument1 = result.get(0);
        Documents.assertEquals(originDocument, actualDocument1);
        Assertions.assertNotNull(actualDocument1.getId());
        Assertions.assertEquals(ForwardableDocumentStatus.PENDING, actualDocument1.getForwardStatus());

        originDocument.setMeta(meta2);
        ForwardableDocumentDto actualDocument2 = result.get(1);
        Documents.assertEquals(originDocument, actualDocument2);
        Assertions.assertNotNull(actualDocument2.getId());
        Assertions.assertEquals(ForwardableDocumentStatus.PENDING, actualDocument2.getForwardStatus());
    }

    @Test
    public void testSetSuccess() {
        List<ForwardableDocumentDto> result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(0, result.size());

        DocumentDto originDocument = Documents.random(true);
        documentDao.save(List.of(originDocument));
        forwardableDocumentDao.savePending(List.of(originDocument));

        result = forwardableDocumentDao.getPendingsForUpdate(100);
        Assertions.assertEquals(1, result.size());

        List<Long> ids = result.stream().map(ForwardableDocumentDto::getId).collect(Collectors.toList());
        forwardableDocumentDao.setForwarded(ids);

        List<Map<String, Object>> actualStatuses = jdbcTemplate.queryForList(
                "SELECT forward_status FROM document_forwardable WHERE id = ?",
                ids.get(0));
        Assertions.assertEquals(1, actualStatuses.size());

        Object actualStatus = actualStatuses.get(0).get("forward_status");
        Assertions.assertEquals(ForwardableDocumentStatus.FORWARDED.name(), actualStatus);
    }
}
