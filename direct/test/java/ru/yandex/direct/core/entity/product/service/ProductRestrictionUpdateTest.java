package ru.yandex.direct.core.entity.product.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.TransactionalRunnable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.product.model.ConditionName;
import ru.yandex.direct.core.entity.product.model.ProductRestriction;
import ru.yandex.direct.core.entity.product.model.ProductRestrictionCondition;
import ru.yandex.direct.core.entity.product.repository.ProductRepository;
import ru.yandex.direct.core.testing.data.TestProducts;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ProductRestrictionUpdateTest {

    private ProductService service;
    private ProductRepository productRepositoryMock;
    private DSLContext transactionCtxMock;

    @Before
    public void setUp() {
        DslContextProvider dslContextProvider = mock(DslContextProvider.class);
        DSLContext mockPpcdict = mock(DSLContext.class);
        when(dslContextProvider.ppcdict()).thenReturn(mockPpcdict);
        org.jooq.Configuration conf = mock(org.jooq.Configuration.class);
        doAnswer(invocation -> {
            TransactionalRunnable tr = invocation.getArgument(0);
            tr.run(conf);
            return null;
        }).when(mockPpcdict).transaction(any(TransactionalRunnable.class));
        transactionCtxMock = mock(DSLContext.class);
        when(conf.dsl()).thenReturn(transactionCtxMock);

        productRepositoryMock = mock(ProductRepository.class);
        List<ProductRestriction> dbRestrictions = newFakeProductRestrictions();
        when(productRepositoryMock.getAllProductRestrictionsForUpdate(transactionCtxMock)).thenReturn(dbRestrictions);

        service = new ProductService(null, productRepositoryMock, dslContextProvider);
    }

    @Test(expected = org.apache.commons.lang.NotImplementedException.class)
    public void testReplaceProductRestrictions_delete_throws() {
        List<ProductRestriction> newRestrictions = prepareNewFakeProductRestrictions();
        newRestrictions.remove(2);
        service.replaceProductRestrictions(newRestrictions);
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testReplaceProductRestrictions_bothConditionJsonAndList_throws() {
        List<ProductRestriction> newRestrictions = prepareNewFakeProductRestrictions();
        newRestrictions.get(2).setConditionJson(JsonUtils.toJson(newRestrictions.get(2).getConditions()));
        service.replaceProductRestrictions(newRestrictions);
    }

    @Test(expected = java.lang.RuntimeException.class)
    public void testReplaceProductRestrictions_changeId_throws() {
        List<ProductRestriction> newRestrictions = prepareNewFakeProductRestrictions();
        newRestrictions.get(2).setId(4L);
        service.replaceProductRestrictions(newRestrictions);
    }

    @Test
    public void testReplaceProductRestrictions_addNew() {
        List<ProductRestrictionCondition> conditions = asList(new ProductRestrictionCondition()
                .withAvailableAny(false)
                .withName(ConditionName.CORRECTION_TRAFFIC)
                .withRequired(false)
                .withValues(List.of(Map.of("value", "1"))));
        ProductRestriction newRestriction = new ProductRestriction()
                .withGroupType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withProductId(508596L)
                .withPublicNameKey("yndx_frontpage")
                .withPublicDescriptionKey("yndx_frontpage_description")
                .withUnitCountMin(null)
                .withUnitCountMax(null)
                .withConditions(conditions);
        List<ProductRestriction> newRestrictions = new ArrayList<>(prepareNewFakeProductRestrictions());
        newRestrictions.add(newRestriction);

        String expectedConditionJson = JsonUtils.toDeterministicJson(conditions);
        ProductRestriction expectedRestriction = new ProductRestriction()
                .withGroupType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withProductId(508596L)
                .withPublicNameKey("yndx_frontpage")
                .withPublicDescriptionKey("yndx_frontpage_description")
                .withUnitCountMin(null)
                .withUnitCountMax(null)
                .withConditions(conditions)
                .withConditionJson(expectedConditionJson);

        service.replaceProductRestrictions(newRestrictions);
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);

        verify(productRepositoryMock).addProductRestrictions(same(transactionCtxMock), captor.capture());
        List<ProductRestriction> restrictionsAdded = (List<ProductRestriction>) captor.getValue();
        assertThat(restrictionsAdded).hasSize(1);
        ProductRestriction restrictionAdded = restrictionsAdded.get(0);
        assertThat(restrictionAdded).is(matchedBy(beanDiffer(expectedRestriction)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReplaceProductRestrictions_updateFields() {

        List<ProductRestriction> newRestrictions = prepareNewFakeProductRestrictions();
        ProductRestriction pr = newRestrictions.get(2);
        List<ProductRestrictionCondition> newConditions = new ArrayList<>(pr.getConditions());
        newConditions.add(new ProductRestrictionCondition()
                .withAvailableAny(false)
                .withName(ConditionName.CORRECTION_TRAFFIC)
                .withRequired(false)
                .withValues(List.of(Map.of("value", "1"))));
        String expectedPublicDescriptionKey = "very_outdoor";
        long expectedUnitCountMin = 123L;
        long expectedUnitCountMax = 1234L;
        String expectedConditionJson = JsonUtils.toDeterministicJson(newConditions);

        pr.setConditions(newConditions);
        pr.withPublicDescriptionKey(expectedPublicDescriptionKey);
        pr.withUnitCountMin(expectedUnitCountMin);
        pr.withUnitCountMax(expectedUnitCountMax);

        service.replaceProductRestrictions(newRestrictions);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(productRepositoryMock).updateProductRestrictions(same(transactionCtxMock),
                (List<AppliedChanges<ProductRestriction>>) captor.capture());
        List<AppliedChanges<ProductRestriction>> changesList =
                (List<AppliedChanges<ProductRestriction>>) captor.getValue();
        assertThat(changesList).hasSize(1);
        AppliedChanges<ProductRestriction> changes = changesList.get(0);
        assertThat(changes.changed(ProductRestriction.PUBLIC_DESCRIPTION_KEY)).isTrue();
        assertThat(changes.getModel().getPublicDescriptionKey()).isEqualTo(expectedPublicDescriptionKey);
        assertThat(changes.changed(ProductRestriction.UNIT_COUNT_MIN)).isTrue();
        assertThat(changes.getModel().getUnitCountMin()).isEqualTo(expectedUnitCountMin);
        assertThat(changes.changed(ProductRestriction.UNIT_COUNT_MAX)).isTrue();
        assertThat(changes.getModel().getUnitCountMax()).isEqualTo(expectedUnitCountMax);
        assertThat(changes.changed(ProductRestriction.CONDITION_JSON)).isTrue();
        assertThat(changes.getModel().getConditionJson()).isEqualTo(expectedConditionJson);

    }

    private List<ProductRestriction> newFakeProductRestrictions() {
        ArrayList<ProductRestriction> list = new ArrayList<>();
        list.add(TestProducts.defaultProductRestriction().withProductId(508594L).withId(1L));
        list.add(TestProducts.defaultProductRestriction().withProductId(508595L).withId(2L));
        list.add(TestProducts.defaultProductRestriction().withProductId(508596L).withId(3L));

        return list;
    }

    private List<ProductRestriction> prepareNewFakeProductRestrictions() {
        List<ProductRestriction> newProductRestrictions = newFakeProductRestrictions();
        for (var pr : newProductRestrictions) {
            pr.setConditionJson(null);
        }
        return newProductRestrictions;
    }
}
