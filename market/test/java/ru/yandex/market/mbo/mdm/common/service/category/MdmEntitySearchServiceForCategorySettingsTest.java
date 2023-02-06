package ru.yandex.market.mbo.mdm.common.service.category;

import java.math.BigDecimal;
import java.util.List;

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mbo.mdm.common.util.MdmEntityConverterForCategorySettings;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmOrderByClause;
import ru.yandex.market.mdm.http.MdmOrderByClauses;
import ru.yandex.market.mdm.http.entity.GetMdmEntityBySearchKeysRequest;
import ru.yandex.market.mdm.http.entity.GetSearchMdmEntityResponse;
import ru.yandex.market.mdm.http.entity.MdmSearchCondition;
import ru.yandex.market.mdm.http.entity.MdmSearchKey;
import ru.yandex.market.mdm.http.entity.MdmSearchKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.CATEGORY_SETTINGS_CATEGORY_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.CATEGORY_SETTINGS_CATEGORY_NAME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.CATEGORY_SETTINGS_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MAX_LIMIT_SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_LIMIT_SHELF_LIFE;

@Import({
    MdmEntityConverterForCategorySettings.class,
    MdmEntitySearchServiceForCategorySettings.class
})
public class MdmEntitySearchServiceForCategorySettingsTest extends MdmBaseDbTestClass {

    @MockBean
    private CategoryCachingService categoryCachingService;

    @Autowired
    private MdmEntitySearchServiceForCategorySettings service;

    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;

    @Test
    public void shouldReturnCategorySettingsById() {
        // given
        var categoryId = 1L;
        var otherCategoryId = 2L;
        var categoryName = "testCategoryName";
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MAX_LIMIT_SHELF_LIFE, 100));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MIN_LIMIT_SHELF_LIFE, 10));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MIN_LIMIT_SHELF_LIFE, 1111));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MAX_LIMIT_SHELF_LIFE, 9999));
        Mockito.when(categoryCachingService.getCategoryFullName(categoryId)).thenReturn(categoryName);

        var request = GetMdmEntityBySearchKeysRequest.newBuilder()
            .addMdmSearchKeys(MdmSearchKeys.newBuilder()
                .setMdmEntityTypeId(CATEGORY_SETTINGS_ID)
                .addMdmSearchKeys(MdmSearchKey.newBuilder()
                    .setCondition(MdmSearchCondition.EQ)
                    .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                        .setMdmAttributeId(CATEGORY_SETTINGS_CATEGORY_ID)
                        .addValues(MdmAttributeValue.newBuilder()
                            .setInt64(categoryId)
                            .build())
                        .build())
                )
            ).build();
        StreamObserver<GetSearchMdmEntityResponse> responseObserver = mock(StreamObserver.class);

        // when
        service.getBySearchKeys(request, responseObserver);

        // then
        ArgumentCaptor<GetSearchMdmEntityResponse> captor = ArgumentCaptor.forClass(GetSearchMdmEntityResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        GetSearchMdmEntityResponse response = captor.getValue();
        assertThat(response.getMdmEntitiesList()).hasSize(1);
        assertThat(extractCategoryId(response)).isEqualTo(categoryId);
        assertThat(extractCategoryName(response)).isEqualTo(categoryName);
    }

    @Test
    public void shouldReturnCategorySettingsByName() {
        // given
        var categoryId = 1L;
        var otherCategoryId = 2L;
        var categoryName = "testCategoryName";
        var otherCategoryName = "otherCategoryName";
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MAX_LIMIT_SHELF_LIFE, 100));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MIN_LIMIT_SHELF_LIFE, 10));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MIN_LIMIT_SHELF_LIFE, 1111));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MAX_LIMIT_SHELF_LIFE, 9999));
        when(categoryCachingService.getAllCategories()).thenReturn(List.of(
            new Category().setCategoryId(categoryId).setName(categoryName),
            new Category().setCategoryId(otherCategoryId).setName(otherCategoryName)))
        ;

        var request = GetMdmEntityBySearchKeysRequest.newBuilder()
            .addMdmSearchKeys(MdmSearchKeys.newBuilder()
                .setMdmEntityTypeId(CATEGORY_SETTINGS_ID)
                .addMdmSearchKeys(MdmSearchKey.newBuilder()
                    .setCondition(MdmSearchCondition.EQ)
                    .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                        .setMdmAttributeId(CATEGORY_SETTINGS_CATEGORY_NAME)
                        .addValues(MdmAttributeValue.newBuilder()
                            .setString(I18nStringUtils.fromSingleRuString(categoryName))
                            .build())
                        .build())
                )
            ).build();
        StreamObserver<GetSearchMdmEntityResponse> responseObserver = mock(StreamObserver.class);

        // when
        service.getBySearchKeys(request, responseObserver);

        // then
        ArgumentCaptor<GetSearchMdmEntityResponse> captor = ArgumentCaptor.forClass(GetSearchMdmEntityResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        GetSearchMdmEntityResponse response = captor.getValue();
        assertThat(response.getMdmEntitiesList()).hasSize(1);
        assertThat(extractCategoryId(response)).isEqualTo(categoryId);
    }

    @Test
    public void shouldReturnCategorySettingsByParam() {
        // given
        var categoryId = 1L;
        var otherCategoryId = 2L;
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MAX_LIMIT_SHELF_LIFE, 100));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MIN_LIMIT_SHELF_LIFE, 10));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MIN_LIMIT_SHELF_LIFE, 1111));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MAX_LIMIT_SHELF_LIFE, 9999));
        List<Long> bmdmMaxShelfLifePath = List.of(34837L, 34897L);

        var request = GetMdmEntityBySearchKeysRequest.newBuilder()
            .addMdmSearchKeys(MdmSearchKeys.newBuilder()
                .setMdmEntityTypeId(CATEGORY_SETTINGS_ID)
                .addMdmSearchKeys(MdmSearchKey.newBuilder()
                    .setCondition(MdmSearchCondition.GT)
                    .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                        .addAllMdmAttributePath(bmdmMaxShelfLifePath)
                        .setMdmAttributeId(34897)
                        .addValues(MdmAttributeValue.newBuilder()
                            .setNumeric(String.valueOf(1000))
                            .build())
                        .build())
                )
            ).build();
        StreamObserver<GetSearchMdmEntityResponse> responseObserver = mock(StreamObserver.class);

        // when
        service.getBySearchKeys(request, responseObserver);

        // then
        ArgumentCaptor<GetSearchMdmEntityResponse> captor = ArgumentCaptor.forClass(GetSearchMdmEntityResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        GetSearchMdmEntityResponse response = captor.getValue();
        assertThat(response.getMdmEntitiesList()).hasSize(1);
        assertThat(extractCategoryId(response)).isEqualTo(otherCategoryId);
    }

    @Test
    public void shouldReturnAllPaginatedCategory() {
        // given
        var categoryId = 1L;
        var otherCategoryId = 2L;
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MAX_LIMIT_SHELF_LIFE, 100));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(categoryId, MIN_LIMIT_SHELF_LIFE, 10));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, KnownMdmParams.MIN_LIMIT_GUARANTEE_PERIOD, 1111));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(otherCategoryId, MAX_LIMIT_SHELF_LIFE, 9999));
        when(categoryCachingService.getAllCategories()).thenReturn(List.of(
            new Category().setCategoryId(categoryId), new Category().setCategoryId(otherCategoryId)));

        var request = GetMdmEntityBySearchKeysRequest.newBuilder()
            .addMdmSearchKeys(MdmSearchKeys.newBuilder()
                .setMdmEntityTypeId(CATEGORY_SETTINGS_ID))
            .setPageSize(1)
            .setPageToken(1)
            .setOrderBy(MdmOrderByClauses.newBuilder()
                .addClauses(MdmOrderByClause.newBuilder()
                    .addMdmAttributePath(CATEGORY_SETTINGS_CATEGORY_ID)
                    .build())
                .buildPartial())

            .build();
        StreamObserver<GetSearchMdmEntityResponse> responseObserver = mock(StreamObserver.class);

        // when
        service.getBySearchKeys(request, responseObserver);

        // then
        ArgumentCaptor<GetSearchMdmEntityResponse> captor = ArgumentCaptor.forClass(GetSearchMdmEntityResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        GetSearchMdmEntityResponse response = captor.getValue();
        assertThat(response.getMdmEntitiesList()).hasSize(1);
        assertThat(extractCategoryId(response)).isEqualTo(otherCategoryId);
    }

    private long extractCategoryId(GetSearchMdmEntityResponse response) {
        return response.getMdmEntitiesList().get(0).getMdmAttributeValuesMap().get(CATEGORY_SETTINGS_CATEGORY_ID)
            .getValues(0).getInt64();
    }

    private String extractCategoryName(GetSearchMdmEntityResponse response) {
        return response.getMdmEntitiesList().get(0).getMdmAttributeValuesMap().get(CATEGORY_SETTINGS_CATEGORY_NAME)
            .getValues(0).getString().getI18NStringList().get(0).getString();
    }

    private CategoryParamValue createNumericCategoryParamValue(long categoryId, long paramId, int value) {
        CategoryParamValue paramValue = new CategoryParamValue();
        paramValue.setCategoryId(categoryId)
            .setMdmParamId(paramId)
            .setNumeric(BigDecimal.valueOf(value));
        return paramValue;
    }
}
