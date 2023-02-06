package ru.yandex.market.mbo.db.modelstorage.index;

import org.apache.commons.collections.keyvalue.UnmodifiableMapEntry;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import ru.yandex.market.mbo.db.modelstorage.index.yt.CategoryVendorYtIndexQuery;
import ru.yandex.market.mbo.db.modelstorage.index.yt.GroupYtIndexQuery;
import ru.yandex.market.mbo.db.modelstorage.index.yt.ModelYtIndexQuery;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.search.SearchCriteria;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author apluhin
 * @created 11/13/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class FilterHelperTest {

    @Test
    public void liveCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.liveCriteria();
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getOperationContainers().size()).isEqualTo(1);
    }

    @Test
    public void deletedCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.deletedCriteria(true);
        assertThat(filter.getDeleted()).isTrue();
        assertThat(filter.getOperationContainers().size()).isEqualTo(1);
    }

    @Test
    public void modelTypeCriteriaTest() {
        List<CommonModel.Source> types = Arrays.asList(CommonModel.Source.BOOK,
            CommonModel.Source.CLUSTER);
        MboIndexesFilter filter = FilterHelper.modelTypeCriteria(types);
        assertThat(filter.getCurrentTypes()).containsExactlyInAnyOrderElementsOf(types);
        assertThat(filter.getOperationContainers().size()).isEqualTo(1);
    }

    @Test
    public void guruModificationCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.guruModelCriteria();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getParentIdExists()).isFalse();
        assertThat(filter.getOperationContainers().size()).isEqualTo(2);
    }

    @Test
    public void guruModelAllCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.guruModelAllCriteria();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getOperationContainers().size()).isEqualTo(1);
    }

    @Test
    public void guruModelCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.guruModelCriteria();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getParentIdExists()).isFalse();
        assertThat(filter.getOperationContainers().size()).isEqualTo(2);
    }

    @Test
    public void categoryIdCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.categoryIdCriteria(1);
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getOperationContainers().size()).isEqualTo(1);
    }

    @Test
    public void vendorIdCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.vendorIdCriteria(1);
        assertThat(filter.getVendorIds()).containsExactly(1L);
        assertThat(filter.getOperationContainers().size()).isEqualTo(1);
    }

    @Test
    public void categoryModelsTest() {
        List<CommonModel.Source> sources = Arrays.asList(CommonModel.Source.BOOK, CommonModel.Source.GURU);
        MboIndexesFilter filter = FilterHelper.categoryModels(1L, sources);
        assertThat(filter.getCurrentTypes()).containsExactlyElementsOf(sources);
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getOperationContainers().size()).isEqualTo(3);

        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void categoryVendorModelsWithType() {
        MboIndexesFilter filter = FilterHelper.categoryVendorModelsWithType(1L, 2L, CommonModel.Source.GURU);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getVendorIds()).containsExactly(2L);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getOperationContainers().size()).isEqualTo(4);
    }

    @Test
    public void categoryVendorModificationTest() {
        MboIndexesFilter f1 = FilterHelper.categoryVendorModification(1L, 2L);
        assertThat(f1.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(f1.getCategoryIds()).containsExactly(1L);
        assertThat(f1.getVendorIds()).containsExactly(2L);
        assertThat(f1.getDeleted()).isFalse();
        assertThat(f1.getOperationContainers().size()).isEqualTo(4);

        MboIndexesFilter f2 = FilterHelper.categoryVendorModification(1L, null);
        assertThat(f2.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(f2.getCategoryIds()).containsExactly(1L);
        assertThat(f2.getDeleted()).isFalse();
        assertThat(f2.getOperationContainers().size()).isEqualTo(3);
    }

    @Test
    public void categoryVendorModelsTest() {
        MboIndexesFilter filter = FilterHelper.categoryVendorModels(1L, 2L);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getVendorIds()).containsExactly(2L);
        assertThat(filter.getParentIdExists()).isFalse();
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getOperationContainers().size()).isEqualTo(5);
    }

    @Test
    public void liveGuruModelsOrderedByVendorFilterTest() {
        MboIndexesFilter filter = FilterHelper.liveGuruModelsOrderedByVendorFilter();
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getParentIdExists()).isFalse();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getOrderBy()).containsExactly(
            new UnmodifiableMapEntry(GenericField.VENDOR_ID, Sort.Direction.ASC));
        assertThat(filter.getOperationContainers().size()).isEqualTo(3);

        filter.setCategoryId(1L);
        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void modificationsFilterTest() {
        MboIndexesFilter filter = FilterHelper.getModifications(Collections.singleton(1L));
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getParentIds()).containsExactly(1L);
        assertThat(filter.getGroupModelIds()).containsExactly(1L);
        assertThat(filter.getOperationContainers().size()).isEqualTo(4);

        filter.setCategoryId(1L);
        assertThat(GroupYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void getCategoryParamQueryGuruTest() {
        Parameter usedParam = new Parameter();
        usedParam.setXslName("test");
        MboIndexesFilter filter = FilterHelper.getCategoryParamQuery(CommonModel.Source.GURU, usedParam);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getPublished()).isTrue();
        assertThat(filter.getAttributes().size()).isEqualTo(1);
        assertThat(filter.getAttributes().get(0).getParameterValues().getXslName()).isEqualTo(usedParam.getXslName());
        assertThat(filter.getOperationContainers().size()).isEqualTo(3);

        filter.setCategoryId(1L);
        assertThat(CategoryVendorYtIndexQuery.partlySupportRank(filter)).isGreaterThan(0);
    }

    @Test
    public void getCategoryParamQuerySkuTest() {
        Parameter usedParam = new Parameter();
        usedParam.setXslName("test");
        MboIndexesFilter filter = FilterHelper.getCategoryParamQuery(CommonModel.Source.SKU, usedParam);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.SKU);
        assertThat(filter.getPublished()).isTrue();
        assertThat(filter.getAttributes().size()).isEqualTo(1);
        assertThat(filter.getAttributes().get(0).getParameterValues().getXslName()).isEqualTo(usedParam.getXslName());
        assertThat(filter.getOperationContainers().size()).isEqualTo(3);

        filter.setCategoryId(1L);
        assertThat(CategoryVendorYtIndexQuery.partlySupportRank(filter)).isGreaterThan(0);
    }

    @Test(expected = IllegalStateException.class)
    public void getCategoryParamQueryUnknownTest() {
        Parameter usedParam = new Parameter();
        usedParam.setXslName("test");
        FilterHelper.getCategoryParamQuery(CommonModel.Source.CLUSTER, usedParam);
    }

    @Test
    public void idsCriteriaTest() {
        MboIndexesFilter filter = FilterHelper.idsCriteria(Arrays.asList(1L, 2L), false);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getModelIds()).containsExactly(1L, 2L);
        assertThat(filter.getOperationContainers().size()).isEqualTo(2);
        assertThat(ModelYtIndexQuery.isSupportFilter(filter)).isTrue();

        MboIndexesFilter filterSecond = FilterHelper.idsCriteria(Arrays.asList(1L, 2L), true);
        assertThat(filterSecond.getModelIds()).containsExactly(1L, 2L);
        assertThat(filterSecond.getOperationContainers().size()).isEqualTo(1);

        assertThat(ModelYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void emptyResultFilterTrueTest() {
        MboIndexesFilter f1 = FilterHelper.idsCriteria(Collections.emptyList(), true);
        MboIndexesFilter f2 = FilterHelper.getModifications(Collections.emptyList());
        assertThat(f1.emptyResult()).isTrue();
        assertThat(f2.emptyResult()).isTrue();

        //test update
        assertThat(f1.setModelId(1L).emptyResult()).isFalse();
        //not fixed yet
        assertThat(f2.setCategoryId(1L).emptyResult()).isTrue();

        assertThat(FilterHelper.idsCriteria(Arrays.asList(1L), true).emptyResult()).isFalse();
        assertThat(FilterHelper.getModifications(Arrays.asList(1L)).emptyResult()).isFalse();
    }

    @Test
    public void getModelsByVendorQueryTest() {
        MboIndexesFilter filter = FilterHelper.getModelsByVendorQuery(1L, 2L);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getVendorIds()).containsExactly(2L);
        assertThat(filter.getOperationContainers().size()).isEqualTo(3);

        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void getGuruModelsByVendorQueryTest() {
        MboIndexesFilter filter = FilterHelper.getGuruModelsByVendorQuery(1L, 2L);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getParentIdExists()).isFalse();
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getVendorIds()).containsExactly(2L);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getOperationContainers().size()).isEqualTo(5);

        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void getPartnerModelsByVendorQueryTest() {
        MboIndexesFilter filter = FilterHelper.getPartnerModelsByVendorQuery(1L, 2L);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getVendorIds()).containsExactly(2L);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.PARTNER);
        assertThat(filter.getOperationContainers().size()).isEqualTo(4);

        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void getClustersByVendorQueryTest() {
        MboIndexesFilter filter = FilterHelper.getClustersByVendorQuery(1L, 2L);
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getVendorIds()).containsExactly(2L);
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.CLUSTER);
        assertThat(filter.getOperationContainers().size()).isEqualTo(4);

        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void testConvertFromSearchCriteria() {
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCategoryId(Arrays.asList(1L, 2L));
        searchCriteria.setId(Arrays.asList(3L));
        searchCriteria.setVendorId(Arrays.asList(4L));
        searchCriteria.setDeleted(false);
        searchCriteria.setHasParent(true);
        searchCriteria.setSource(CommonModel.Source.GURU);

        MboIndexesFilter filter = FilterHelper.convertSearchCriteria(searchCriteria);

        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCategoryIds()).containsExactly(1L, 2L);
        assertThat(filter.getModelIds()).containsExactly(3L);
        assertThat(filter.getVendorIds()).containsExactly(4L);
        assertThat(filter.getParentIdExists()).isTrue();
        assertThat(filter.getOperationContainers().size()).isEqualTo(6);

        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void testGetOnlyOperatorQualityCriteria() {
        ParamValueSearch onlyOperatorQualityCriteria = FilterHelper.getOnlyOperatorQualityCriteria(true);
        System.out.println(onlyOperatorQualityCriteria);
    }

    @Test
    public void getLiveModificationsQueryTest() {
        MboIndexesFilter filter = FilterHelper.getLiveModificationsQuery(1L);
        assertThat(filter.getCategoryIds()).containsExactly(1L);
        assertThat(filter.getParentIdExists()).isTrue();
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getOperationContainers().size()).isEqualTo(4);
        assertThat(CategoryVendorYtIndexQuery.isSupportFilter(filter)).isTrue();
    }

    @Test
    public void getModificationsQueryTest() {
        MboIndexesFilter filter = FilterHelper.getModificationsQuery(Collections.singleton(1L));
        assertThat(filter.getDeleted()).isFalse();
        assertThat(filter.getCurrentTypes()).containsExactly(CommonModel.Source.GURU);
        assertThat(filter.getParentIds()).containsExactly(1L);
        assertThat(filter.getGroupModelIds()).containsExactly(1L);
        assertThat(filter.getOperationContainers().size()).isEqualTo(4);

        assertThat(filter.getOrderBy().get(GenericField.PARENT_ID)).isEqualByComparingTo(Sort.Direction.ASC);

        filter.setCategoryId(1L);
        assertThat(GroupYtIndexQuery.isSupportFilter(filter)).isTrue();
    }
}
