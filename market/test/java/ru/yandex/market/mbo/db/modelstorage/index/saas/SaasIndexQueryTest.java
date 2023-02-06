package ru.yandex.market.mbo.db.modelstorage.index.saas;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.saas.search.SaasSearchRequest;
import ru.yandex.market.saas.search.term.Term;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author apluhin
 * @created 11/11/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SaasIndexQueryTest extends SaasIndexQuery {

    public SaasIndexQueryTest() {
        super(null);
    }

    @Test
    public void testSuccessBuildQuery() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter.setCategoryId(100L);
        filter.setModelId(100L);
        filter.setDeleted(true);
        filter.setLimit(50);
        filter.setOffset(100);
        filter.addOrderBy(GenericField.CLUSTERIZER_OFFER_COUNT, Sort.Direction.ASC);
        SaasSearchRequest mock = mock(SaasSearchRequest.class);
        new SaasIndexQuery(filter).buildQuery(mock);

        verify(mock, times(1)).pageNumber(eq(2));
        verify(mock, times(1)).withNumberOfResults(eq(50));
        verify(mock, times(1)).sortAsc();
        verify(mock, times(1)).sortBy(eq(
            new SaasGroupingAttributeImpl("i_clusterizer_offer_ids_count"))
        );
    }

    @Test
    public void testSuccessSortByLiteralQuery() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter.addOrderBy(GenericField.VENDOR_ID, Sort.Direction.ASC);
        SaasSearchRequest mock = mock(SaasSearchRequest.class);
        new SaasIndexQuery(filter).buildQuery(mock);

        verify(mock, times(1)).sortAsc();
        verify(mock, times(1)).sortBy(eq(
            new SaasGroupingAttributeImpl("i_vendor_id"))
        );
    }


    @Test
    public void testShuffleSortBuildQuery() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter.setRandom(true);
        SaasSearchRequest mock = mock(SaasSearchRequest.class);
        new SaasIndexQuery(filter).buildQuery(mock);

        verify(mock, times(1)).sortBy(eq(
            new SaasGroupingAttributeImpl("shuffle")
        ));
    }

    @Test
    public void testSuccessFacetQuery() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter.setCategoryId(100L);
        SaasSearchRequest mock = mock(SaasSearchRequest.class);
        new SaasIndexQuery(filter).getFieldValuesQuery(mock, GenericField.MODEL_ID);
        verify(mock, times(1)).withFacetsBy(eq(
            new SaasGroupingAttributeImpl(GenericField.MODEL_ID.saasField()))
        );
    }

    @Test(expected = RuntimeException.class)
    public void testPagingFailed() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter.setLimit(51);
        filter.setOffset(100);
        SaasSearchRequest mock = mock(SaasSearchRequest.class);
        new SaasIndexQuery(filter).buildQuery(mock);
    }

    @Test
    public void testBuildCountQueryIgnoreLimit() {
        MboIndexesFilter filter = new MboIndexesFilter();
        filter.setCategoryId(100L);
        filter.setLimit(100);
        filter.setOffset(50);
        SaasSearchRequest mock = mock(SaasSearchRequest.class);
        new SaasIndexQuery(filter).buildCountQuery(mock);
        verify(mock, times(0)).sortBy(any(SaasGroupingAttributeImpl.class));
        verify(mock, times(0)).pageNumber(anyInt());
        verify(mock, times(1)).withNumberOfResults(eq(1));
    }


    @Test
    public void buildOrQueryTest() {
        MboIndexesFilter root = new MboIndexesFilter().setModelId(1000L);
        MboIndexesFilter ch1 = new MboIndexesFilter().setModelId(1001L).setCurrentType(CommonModel.Source.GURU);
        MboIndexesFilter ch2 = new MboIndexesFilter().setModelId(1002L).setAll(false)
            .setCurrentType(CommonModel.Source.CLUSTER);

        root.addCriteria(ch1).addCriteria(ch2);

        Term term = new SaasIndexQuery(root).buildTerm();
        StringBuilder builder = new StringBuilder();
        term.mkString(builder);
        Assertions.assertThat(builder.toString().trim()).isEqualTo(
            "(  (  (  ( s_id:000000000000001000 )  )  &&  " +
                "(  (  ( s_id:000000000000001001 )  &&  ( s_current_type:GURU )  )  )  )  " +
                "|  (  (  ( s_id:000000000000001002 )  &&  ( s_current_type:CLUSTER )  )  )  )"
        );
        System.out.println(root.printableInfo());
    }

    @Test
    public void buildOrQueryTestWithEmptyRoot() {
        MboIndexesFilter root = new MboIndexesFilter();
        MboIndexesFilter ch1 = new MboIndexesFilter().setModelId(1001L)
            .setCurrentType(CommonModel.Source.GURU).setAll(false);
        MboIndexesFilter ch2 = new MboIndexesFilter().setModelId(1002L).setAll(false)
            .setCurrentType(CommonModel.Source.CLUSTER);

        root.addCriteria(ch1).addCriteria(ch2);

        Term term = new SaasIndexQuery(root).buildTerm();
        StringBuilder builder = new StringBuilder();
        term.mkString(builder);
        Assertions.assertThat(builder.toString().trim()).isEqualTo(
            "(  (  (  ( s_id:000000000000001001 )  &&  ( s_current_type:GURU )  )  )  " +
                "|  (  (  ( s_id:000000000000001002 )  &&  ( s_current_type:CLUSTER )  )  )  )"
        );
        System.out.println(root.printableInfo());
    }

}
