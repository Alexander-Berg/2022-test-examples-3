package ru.yandex.market.mbo.classifier.service.index;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

import ru.yandex.market.mbo.classifier.model.OfferId;
import ru.yandex.market.mbo.classifier.model.SearchOffer;
import ru.yandex.market.mbo.classifier.reload.IndexFields;

/**
 * @author moskovkin@yandex-team.ru
 * @since 29.09.17
 */
public class OffersIndexMock extends AbstractOffersIndex {
    @Override
    protected void doUpdate(Map<OfferId, SearchOffer> offers) throws IOException {
        for (Map.Entry<OfferId, SearchOffer> entry : offers.entrySet()) {
            Term term = new Term(IndexFields.OFFER_ID, entry.getKey().getClassifierMagicId());
            Document document = getConverter().convert(entry.getValue());
            getWriter().updateDocument(term, document);
        }
    }
}
