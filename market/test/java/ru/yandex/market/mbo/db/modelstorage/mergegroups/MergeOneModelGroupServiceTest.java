package ru.yandex.market.mbo.db.modelstorage.mergegroups;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;

@SuppressWarnings("checkstyle:magicnumber")
public class MergeOneModelGroupServiceTest {

    private MboMappingsService mboMappingsService;
    private MergeOneModelGroupService service;

    @Before
    public void setup() {
        mboMappingsService = Mockito.mock(MboMappingsService.class);
        service = new MergeOneModelGroupService(mboMappingsService, true, 0L);
    }

    @Test
    public void testChoseMostOffersModel() {
        //when
        //офферы
        List<SupplierOffer.Offer> offers = ImmutableMap.of(
            1L, SupplierOffer.SupplierType.TYPE_FMCG,
            2L, SupplierOffer.SupplierType.TYPE_THIRD_PARTY,
            3L, SupplierOffer.SupplierType.TYPE_THIRD_PARTY,
            4L, SupplierOffer.SupplierType.TYPE_THIRD_PARTY,
            5L, SupplierOffer.SupplierType.TYPE_THIRD_PARTY
        ).entrySet().stream()
            .map(e -> SupplierOffer.Offer.newBuilder()
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                    .setSkuId(e.getKey())
                    .build())
                .setSupplierType(e.getValue())
                .build())
            .collect(Collectors.toList());

        MboMappings.SearchMappingsResponse resp = MboMappings.SearchMappingsResponse.newBuilder()
            .addAllOffers(offers)
            .build();
        Mockito.when(mboMappingsService.searchBaseOfferMappingsByMarketSkuId(Mockito.any())).thenReturn(resp);

        //модели
        Map<Long, CommonModel> models = ImmutableList.of(1L, 2L, 3L).stream()
            .map(id -> createModel(id, CommonModel.Source.GURU))
            .collect(Collectors.toMap(CommonModel::getId, Function.identity()));

        //ску
        Map<Long, Collection<CommonModel>> skus = ImmutableMap.of(
            1L, ImmutableList.of(createSKU(1L), createSKU(2L)),
            2L, ImmutableList.of(createSKU(3L), createSKU(4L)),
            3L, ImmutableList.of(createSKU(5L))
        );

        CommonModel choosen = service.chooseRootModel(models, skus);
        Assertions.assertThat(choosen).isEqualTo(models.get(2L));
    }

    @Test
    public void testWhenNoOffersThenRandomGuru() {
        //when
        //офферы
        List<SupplierOffer.Offer> offers = Collections.emptyList();

        MboMappings.SearchMappingsResponse resp = MboMappings.SearchMappingsResponse.newBuilder()
            .addAllOffers(offers)
            .build();
        Mockito.when(mboMappingsService.searchBaseOfferMappingsByMarketSkuId(Mockito.any())).thenReturn(resp);

        //модели
        Map<Long, CommonModel> models = ImmutableList.of(1L, 2L, 3L).stream()
            .map(id -> createModel(id, CommonModel.Source.GURU))
            .collect(Collectors.toMap(CommonModel::getId, Function.identity()));

        //ску
        Map<Long, Collection<CommonModel>> skus = ImmutableMap.of(
            1L, ImmutableList.of(createSKU(1L), createSKU(2L)),
            2L, ImmutableList.of(createSKU(3L), createSKU(4L)),
            3L, ImmutableList.of(createSKU(5L))
        );

        CommonModel choosen = service.chooseRootModel(models, skus);
        Assertions.assertThat(choosen).isNotNull();
    }

    @Test
    public void testWhenNoSkuThenNull() {
        //when
        //офферы
        List<SupplierOffer.Offer> offers = Collections.emptyList();

        MboMappings.SearchMappingsResponse resp = MboMappings.SearchMappingsResponse.newBuilder()
            .addAllOffers(offers)
            .build();
        Mockito.when(mboMappingsService.searchBaseOfferMappingsByMarketSkuId(Mockito.any())).thenReturn(resp);

        //модели
        Map<Long, CommonModel> models = ImmutableList.of(1L, 2L, 3L).stream()
            .map(id -> createModel(id, CommonModel.Source.GURU))
            .collect(Collectors.toMap(CommonModel::getId, Function.identity()));

        //ску
        Map<Long, Collection<CommonModel>> skus = ImmutableMap.of(
            1L, ImmutableList.of(),
            2L, ImmutableList.of(),
            3L, ImmutableList.of()
        );

        CommonModel choosen = service.chooseRootModel(models, skus);
        Assertions.assertThat(choosen).isNull();
    }

    private CommonModel createModel(Long id, CommonModel.Source type) {
        CommonModel model = new CommonModel();
        model.setCurrentType(type);
        model.setSource(type);
        model.setId(id);
        return model;
    }

    private CommonModel createSKU(Long id) {
        return createModel(id, CommonModel.Source.SKU);
    }
}
