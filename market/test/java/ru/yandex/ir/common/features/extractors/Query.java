package ru.yandex.ir.common.features.extractors;

import java.util.List;

import ru.yandex.ir.classify.om.be.RequestInfo;
import ru.yandex.ir.classify.om.be.RequestType;
import ru.yandex.ir.common.be.RankingRequest;

public class Query extends Text implements RankingRequest<Long>, RequestInfo {
    private final List<Long> documents;
    private RequestType type;

    public Query(RequestType type, String title, String description, List<Long> documents) {
        super(title, description);
        this.type = type;
        this.documents = documents;
    }

    @Override
    public List<Long> getDocuments() {
        return documents;
    }

    @Override
    public int documentsSize() {
        return documents.size();
    }

    @Override
    public RequestType getRequestType() {
        return type;
    }
}
