package ru.yandex.market.mbo.classifier.service.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.classifier.dao.SearchOfferConverter;
import ru.yandex.market.mbo.classifier.dao.loaders.OfferLoader;
import ru.yandex.market.mbo.classifier.dao.tables.SourceTable;
import ru.yandex.market.mbo.classifier.model.DataPage;
import ru.yandex.market.mbo.classifier.model.Offer;
import ru.yandex.market.mbo.classifier.model.OfferId;
import ru.yandex.market.mbo.classifier.model.Pagination;
import ru.yandex.market.mbo.classifier.model.SearchOffer;
import ru.yandex.market.mbo.classifier.reload.IndexFields;

/**
 * @author moskovkin@yandex-team.ru
 * @since 29.09.17
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class OffersIndexTest {
    private static final Offer OFFER_1 = createOffer("1", "42", "This is offer 1");
    private static final SearchOffer SEARCH_OFFER_1 = createSearchOffer(10, OFFER_1);

    private static final Offer OFFER_2 = createOffer("2", "43", "This is offer 2");
    private static final SearchOffer SEARCH_OFFER_2 = createSearchOffer(20, OFFER_2);

    private static final Pagination PAGINATION = new Pagination(100, 0);

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Mock
    private SearchOfferConverter converter;

    @Mock
    private SourceTable sourceTable;

    @Mock
    private OfferLoader offerLoader;

    private OffersIndexMock index;

    @Before
    public void setup() throws IOException {
        index = new OffersIndexMock();
        index.setSourceTable(sourceTable);
        index.converter = converter;

        Mockito.doReturn(TEMPORARY_FOLDER.newFolder().getAbsolutePath())
                .when(sourceTable)
                .getIndexPath();
        Mockito.doReturn("mocked_last_session_id")
                .when(sourceTable)
                .getLastSessionId();
        Mockito.doReturn(offerLoader)
                .when(sourceTable)
                .getOfferLoader();

        Mockito.doAnswer(invocation -> createDocument(invocation.getArgument(0)))
                .when(converter).convert(Mockito.any());

        index.refresh();
    }

    private static Document createDocument(SearchOffer data) {
        Document document = new Document();
        document.add(IndexFields.getIdField(data.getId()));
        document.add(IndexFields.getOfferIdField(data.getOffer().getOfferId().getClassifierMagicId()));
        document.add(IndexFields.getGoodIdField(data.getOffer().getOfferId().getClassifierGoodId()));
        document.add(IndexFields.getOfferField(data.getOffer().getTitle()));
        return document;
    }

    private static SearchOffer createSearchOffer(int id, Offer offer) {
        SearchOffer result = new SearchOffer(offer);
        result.setId(id);
        return result;
    }

    private static Offer createOffer(String id, String goodId, String title) {
        Offer result = new Offer(id, goodId);
        result.setTitle(title);
        return result;
    }

    @Test
    public void testUpdate() throws IOException {
        index.update(Arrays.asList(SEARCH_OFFER_2, SEARCH_OFFER_1));

        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);

        DataPage<OfferId> results = index.find(qb.build(), new ArrayList<>(), PAGINATION);
        Assert.assertTrue(results.getData().contains(OFFER_1.getOfferId()));
        Assert.assertTrue(results.getData().contains(OFFER_2.getOfferId()));
    }

    @Test
    public void testUpdateWithClosedWriter() throws IOException {
        index.getWriter().close();
        index.update(Arrays.asList(SEARCH_OFFER_2, SEARCH_OFFER_1));
        index.refresh();

        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);

        DataPage<OfferId> results = index.find(qb.build(), new ArrayList<>(), PAGINATION);
        Assert.assertTrue(results.getData().contains(OFFER_1.getOfferId()));
        Assert.assertTrue(results.getData().contains(OFFER_2.getOfferId()));
    }

    @Test
    public void testRemove() throws IOException {
        index.update(Arrays.asList(SEARCH_OFFER_2, SEARCH_OFFER_1));

        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);

        DataPage<OfferId> results = index.find(qb.build(), new ArrayList<>(), PAGINATION);
        Assert.assertTrue(results.getData().contains(OFFER_1.getOfferId()));
        Assert.assertTrue(results.getData().contains(OFFER_2.getOfferId()));

        index.remove(Arrays.asList(OFFER_2.getOfferId()));
        results = index.find(qb.build(), new ArrayList<>(), PAGINATION);
        Assert.assertTrue(results.getData().contains(OFFER_1.getOfferId()));
        Assert.assertFalse(results.getData().contains(OFFER_2.getOfferId()));
    }

    @Test
    public void testRemoveWithClosedWriter() throws IOException {
        index.update(Arrays.asList(SEARCH_OFFER_2, SEARCH_OFFER_1));

        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);

        DataPage<OfferId> results = index.find(qb.build(), new ArrayList<>(), PAGINATION);
        Assert.assertTrue(results.getData().contains(OFFER_1.getOfferId()));
        Assert.assertTrue(results.getData().contains(OFFER_2.getOfferId()));

        index.getWriter().close();
        index.remove(Arrays.asList(OFFER_2.getOfferId()));
        index.refresh();

        results = index.find(qb.build(), new ArrayList<>(), PAGINATION);
        Assert.assertTrue(results.getData().contains(OFFER_1.getOfferId()));
        Assert.assertFalse(results.getData().contains(OFFER_2.getOfferId()));
    }
}
