package ru.yandex.market.b2b.clients.impl;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.Randoms;
import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.GenerationStatusType;

public class DocumentDaoImplTest extends AbstractFunctionalTest {

    @Autowired
    DocumentDaoImpl dao;

    @Test
    public void saveAndGet() {
        DocumentDto document = Documents.random();

        dao.save(List.of(document));

        List<DocumentDto> result = dao.getDocuments(document.getOrder());

        Assertions.assertEquals(1, result.size());
        DocumentDto d = result.get(0);
        Documents.assertEquals(document, d);
    }

    @Test
    public void saveAndGetWithMeta() {
        DocumentDto document = Documents.random(true);

        dao.save(List.of(document));

        List<DocumentDto> result = dao.getDocuments(document.getOrder());

        Assertions.assertEquals(1, result.size());
        DocumentDto d = result.get(0);

        document.setForward(null);
        Documents.assertEquals(document, d);
    }

    @Test
    public void update() {
        DocumentDto document = Documents.random();

        dao.save(List.of(document));

        document.setDate(Randoms.offsetDateTime());
        document.setUrl(Randoms.string());

        dao.save(List.of(document));

        List<DocumentDto> result = dao.getDocuments(document.getOrder());
        Assertions.assertEquals(1, result.size());
        DocumentDto d = result.get(0);
        Documents.assertEquals(document, d);
    }

    @Test
    public void saveAndGetByList() {
        DocumentDto document = Documents.random();
        dao.save(List.of(document));
        DocumentDto document2 = Documents.random();
        dao.save(List.of(document2));

        List<DocumentDto> result = dao.getDocuments(List.of(document.getOrder(), document2.getOrder()));

        Assertions.assertEquals(2, result.size());
        Documents.assertContains(document, result);
        Documents.assertContains(document2, result);
    }

    @Test
    public void saveAndGetWithMetaByList() {
        DocumentDto document = Documents.random(true);
        dao.save(List.of(document));
        DocumentDto document2 = Documents.random(true);
        dao.save(List.of(document2));

        List<DocumentDto> result = dao.getDocuments(List.of(document.getOrder(), document2.getOrder()));

        document.setForward(null);
        document2.setForward(null);

        Assertions.assertEquals(2, result.size());
        Documents.assertContains(document, result);
        Documents.assertContains(document2, result);
    }

    @Test
    public void saveAndGetByNeedGenerate() {
        DocumentDto document = Documents.random();
        document.setGenerationStatus(GenerationStatusType.ZIP);

        DocumentDto document2 = Documents.random();
        document2.setGenerationStatus(GenerationStatusType.ZIP);

        dao.save(List.of(document, document2));

        List<DocumentDto> result = dao.getDocumentsForGeneration(GenerationStatusType.ZIP);
        Documents.assertContains(document, result);
        Documents.assertContains(document2, result);
    }

}
