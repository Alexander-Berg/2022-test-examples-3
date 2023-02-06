package ru.yandex.market.clab.ui.service.document;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.clab.db.jooq.generated.enums.DocumentType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author anmalysh
 * @since 12/11/2018
 */
public class DocumentRepositoryStub implements DocumentRepository {

    private AtomicLong idGenerator = new AtomicLong();
    private Map<Pair<Long, DocumentType>, Document> documents = new HashMap<>();

    @Override
    public Long generateDocumentId() {
        return idGenerator.incrementAndGet();
    }

    @Override
    public Document save(Document document) {
        Document storageDoc = new Document(document);
        storageDoc.setId(idGenerator.incrementAndGet());
        documents.put(new Pair<>(storageDoc.getEntityId(), storageDoc.getType()), storageDoc);
        return storageDoc;
    }

    @Override
    public Document getByEntityAndType(long entityId, DocumentType type) {
        Document result = documents.get(new Pair<>(entityId, type));
        return result == null ? result : new Document(result);
    }
}
