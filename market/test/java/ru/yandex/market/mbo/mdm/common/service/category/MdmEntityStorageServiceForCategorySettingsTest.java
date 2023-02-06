package ru.yandex.market.mbo.mdm.common.service.category;

import java.math.BigDecimal;

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.util.MdmEntityConverterForCategorySettings;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByMdmIdsRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityResponse;
import ru.yandex.market.mdm.http.search.MdmEntityIds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.CATEGORY_SETTINGS_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MAX_LIMIT_SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.MIN_LIMIT_SHELF_LIFE;

@Import({
    MdmEntityConverterForCategorySettings.class,
    MdmEntityStorageServiceForCategorySettings.class
})
public class MdmEntityStorageServiceForCategorySettingsTest extends MdmBaseDbTestClass {

    @MockBean
    private CategoryCachingService categoryCachingService;

    @Autowired
    private MdmEntityStorageServiceForCategorySettings service;

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

        var request = GetMdmEntityByMdmIdsRequest.newBuilder()
            .setMdmIds(MdmEntityIds.newBuilder()
                .addMdmIds(categoryId)
                .setMdmEntityTypeId(CATEGORY_SETTINGS_ID)
            ).build();
        StreamObserver<GetMdmEntityResponse> responseObserver = mock(StreamObserver.class);

        // g
        service.getByMdmIds(request, responseObserver);

        // then
        ArgumentCaptor<GetMdmEntityResponse> captor = ArgumentCaptor.forClass(GetMdmEntityResponse.class);
        Mockito.verify(responseObserver).onNext(captor.capture());
        GetMdmEntityResponse response = captor.getValue();
        assertThat(response.getMdmEntitiesList()).hasSize(1);
        assertThat(response.getMdmEntities(0).getMdmId()).isEqualTo(categoryId);

    }

    private CategoryParamValue createNumericCategoryParamValue(long categoryId, long paramId, int value) {
        CategoryParamValue paramValue = new CategoryParamValue();
        paramValue.setCategoryId(categoryId)
            .setMdmParamId(paramId)
            .setNumeric(BigDecimal.valueOf(value));
        return paramValue;
    }

}
