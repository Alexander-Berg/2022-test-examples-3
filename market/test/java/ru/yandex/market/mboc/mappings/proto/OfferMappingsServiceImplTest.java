package ru.yandex.market.mboc.mappings.proto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierFilter;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.Offer.Mapping;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.OfferMappings;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.http.OfferMappings.OfferMappingsUpdate.MappingType.APPROVED;
import static ru.yandex.market.mboc.http.OfferMappings.OfferMappingsUpdate.MappingType.SUPPLIER;

@SuppressWarnings("checkstyle:magicnumber")
@Ignore
@Deprecated
public class OfferMappingsServiceImplTest {
    private static final int SHOP_ID = 123;
    private static final String SHOP_SKU = "SKU123";

    private OfferRepositoryMock offerRepository;
    private MboModelsService mboModelsService;
    private CategoryCachingService categoryCachingService;
    private SupplierRepositoryMock supplierRepository;
    private MbiApiClient mbiApiClientMock;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private OfferMappingsServiceImpl service;
    private SupplierService supplierService;

    private final OfferDestinationCalculator offerDestinationCalculator = new DefaultOfferDestinationCalculator();

    @Before
    public void setup() {
        offerRepository = new OfferRepositoryMock();
        mboModelsService = mock(MboModelsService.class);
        categoryCachingService = mock(CategoryCachingService.class);
        supplierRepository = new SupplierRepositoryMock();
        supplierService = new SupplierService(supplierRepository);
        mbiApiClientMock = mock(MbiApiClient.class);
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator,
            new StorageKeyValueServiceMock());
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        service = new OfferMappingsServiceImpl(
            offerRepository, mboModelsService, categoryCachingService, supplierRepository,
            offerMappingActionService,
            mbiApiClientMock,
            Mockito.mock(ApplySettingsService.class),
            offerDestinationCalculator
        );
    }

    @Test
    public void testGetMappingsForUnknownOffer() {
        OfferMappings.GetMappingsResponse response = service.getMappings(defaultGetMappingsRequest());
        assertThat(response.getMappingsList()).isEmpty();
    }

    @Test
    public void testGetMappingForKnownOffer() {
        offerRepository.setOffers(Offer.builder()
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .mappingDestination(Offer.MappingDestination.WHITE)
            .supplierSkuMapping(new Mapping(21L, DateTimeUtils.dateTimeNow(), null))
            .approvedSkuMapping(new Mapping(22L, DateTimeUtils.dateTimeNow(), null))
            .supplierCategoryId(31L)
            .categoryId(32L)
            .supplierModelMappingId(41L)
            .modelId(42L)
            .build());

        OfferMappings.GetMappingsResponse response = service.getMappings(defaultGetMappingsRequest());

        assertThat(response.getMappingsList()).usingRecursiveFieldByFieldElementComparator().containsExactly(
            OfferMappings.Mappings.newBuilder()
                .setId(defaultOfferID())
                .setDestnation(OfferMappings.Mappings.Destination.WHITE)
                .setSupplierSku(OfferMappings.SkuMapping.newBuilder().setMappedId(21L).build())
                .setApprovedSku(OfferMappings.SkuMapping.newBuilder().setMappedId(22L).build())
                .setSupplierCategory(OfferMappings.CategoryMapping.newBuilder().setMappedId(31L).build())
                .setApprovedCategory(OfferMappings.CategoryMapping.newBuilder().setMappedId(32L).build())
                .setSupplierModel(OfferMappings.ModelMapping.newBuilder().setMappedId(41L).build())
                .setApprovedModel(OfferMappings.ModelMapping.newBuilder().setMappedId(42L).build())
                .build()
        );
    }

    @Test
    public void testUpdateUnknownOfferCreatesNewOne() {
        var request = OfferMappings.UpdateOfferMappingsRequest.newBuilder()
            .addUpdates(OfferMappings.OfferMappingsUpdate.newBuilder()
                .setId(defaultOfferID())
                .addAllSkuUpdates(List.of(
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(21L).setType(SUPPLIER).build()
                ))
                .build())
            .build();

        setModels(
            new Model()
                .setId(21L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
        );

        supplierRepository.insert(new Supplier(SHOP_ID, "some supplier"));

        OfferMappings.UpdateOfferMappingsResponse response = service.updateMappings(request);
        assertThat(response.getResultsList()).usingRecursiveFieldByFieldElementComparator().containsExactly(
            OfferMappings.MappingsUpdateResult.newBuilder()
                .setId(defaultOfferID())
                .setStatus(OfferMappings.MappingsUpdateResult.Status.OK)
                .build()
        );

        List<Offer> offers = offerRepository.findOffersByBusinessSkuKeys(
            OfferMappingsServiceImpl.offerIdToShopSku(defaultOfferID()));
        assertThat(offers).isNotEmpty();

        Offer offerAfterUpdate = offers.get(0);
        assertThat(offerAfterUpdate.getSupplierSkuMapping()).isNotNull();
        assertThat(offerAfterUpdate.getSupplierSkuMapping().getMappingId()).isEqualTo(21L);
        assertThat(offerAfterUpdate.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }

    @Test
    public void testUpdateKnownOffer() {
        var request = OfferMappings.UpdateOfferMappingsRequest.newBuilder()
            .addUpdates(OfferMappings.OfferMappingsUpdate.newBuilder()
                .setId(defaultOfferID())
                .addAllSkuUpdates(List.of(
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(21L).setType(SUPPLIER).build(),
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(22L).setType(APPROVED).build()
                ))
                .addAllCategoryUpdates(List.of(
                    OfferMappings.CategoryMappingUpdate.newBuilder().setCategoryId(31L).setType(SUPPLIER).build(),
                    OfferMappings.CategoryMappingUpdate.newBuilder().setCategoryId(32L).setType(APPROVED).build()
                ))
                .addAllModelUpdates(List.of(
                    OfferMappings.ModelMappingUpdate.newBuilder().setModelId(41L).setType(SUPPLIER).build(),
                    OfferMappings.ModelMappingUpdate.newBuilder().setModelId(42L).setType(APPROVED).build()
                ))
                .build())
            .build();

        long offerID = 1;
        offerRepository.setOffers(Offer.builder()
            .id(offerID)
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .build()
        );

        when(categoryCachingService.getCategories(anyList())).thenAnswer(
            arg -> ((List<Long>) arg.getArgument(0)).stream()
                .map(id -> new Category().setCategoryId(id))
                .collect(toList())
        );

        setModels(
            new Model()
                .setId(21L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
                .setSkuParentModelId(41L),

            new Model()
                .setId(22L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
                .setSkuParentModelId(42L),

            new Model()
                .setId(41L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.GURU),

            new Model()
                .setId(42L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.GURU)
        );

        supplierRepository.insert(new Supplier(SHOP_ID, "some supplier"));

        OfferMappings.UpdateOfferMappingsResponse response = service.updateMappings(request);
        assertThat(response.getResultsList()).usingRecursiveFieldByFieldElementComparator().containsExactly(
            OfferMappings.MappingsUpdateResult.newBuilder()
                .setId(defaultOfferID())
                .setStatus(OfferMappings.MappingsUpdateResult.Status.OK)
                .build()
        );

        Offer offerAfterUpdate = offerRepository.getOfferById(offerID);

        assertThat(offerAfterUpdate.getSupplierSkuMapping()).isNotNull();
        assertThat(offerAfterUpdate.getApprovedSkuMapping()).isNotNull();

        assertThat(offerAfterUpdate.getSupplierSkuMapping().getMappingId()).isEqualTo(21L);
        assertThat(offerAfterUpdate.getApprovedSkuMapping().getMappingId()).isEqualTo(22L);

        assertThat(offerAfterUpdate.getSupplierCategoryId()).isEqualTo(31L);
        assertThat(offerAfterUpdate.getMappedCategoryId()).isEqualTo(32L);

        assertThat(offerAfterUpdate.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.PARTNER);

        assertThat(offerAfterUpdate.getCategoryId()).isEqualTo(32L);
        assertThat(offerAfterUpdate.getBindingKind()).isEqualTo(Offer.BindingKind.SUPPLIER);

        assertThat(offerAfterUpdate.getSupplierModelMappingId()).isEqualTo(42L);
        assertThat(offerAfterUpdate.getModelId()).isEqualTo(42L);
    }

    @Test
    public void testResetAllMappings() {
        Long none = 0L;

        var request = OfferMappings.UpdateOfferMappingsRequest.newBuilder()
            .addUpdates(OfferMappings.OfferMappingsUpdate.newBuilder()
                .setId(defaultOfferID())
                .addAllSkuUpdates(List.of(
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(none).setType(SUPPLIER).build(),
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(none).setType(APPROVED).build()
                ))
                .addAllCategoryUpdates(List.of(
                    OfferMappings.CategoryMappingUpdate.newBuilder().setCategoryId(none).setType(SUPPLIER).build(),
                    OfferMappings.CategoryMappingUpdate.newBuilder().setCategoryId(none).setType(APPROVED).build()
                ))
                .addAllModelUpdates(List.of(
                    OfferMappings.ModelMappingUpdate.newBuilder().setModelId(none).setType(SUPPLIER).build(),
                    OfferMappings.ModelMappingUpdate.newBuilder().setModelId(none).setType(APPROVED).build()
                ))
                .build())
            .build();

        long offerID = 1;
        offerRepository.setOffers(Offer.builder()
            .id(offerID)
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .mappingDestination(Offer.MappingDestination.WHITE)
            .supplierSkuMapping(new Mapping(21L, DateTimeUtils.dateTimeNow(), null))
            .approvedSkuMapping(new Mapping(22L, DateTimeUtils.dateTimeNow(), null))
            .supplierCategoryId(31L)
            .categoryId(32L)
            .supplierModelMappingId(41L)
            .modelId(42L)
            .build()
        );

        supplierRepository.insert(new Supplier(SHOP_ID, "some supplier"));

        OfferMappings.UpdateOfferMappingsResponse response = service.updateMappings(request);
        assertThat(response.getResultsList()).usingRecursiveFieldByFieldElementComparator().containsExactly(
            OfferMappings.MappingsUpdateResult.newBuilder()
                .setId(defaultOfferID())
                .setStatus(OfferMappings.MappingsUpdateResult.Status.OK)
                .build()
        );

        Offer offerAfterUpdate = offerRepository.getOfferById(offerID);

        assertThat(offerAfterUpdate.getSupplierSkuMapping()).isNotNull();
        assertThat(offerAfterUpdate.getApprovedSkuMapping()).isNotNull();

        assertThat(offerAfterUpdate.getSupplierSkuMapping().getMappingId()).isEqualTo(none);
        assertThat(offerAfterUpdate.getApprovedSkuMapping().getMappingId()).isEqualTo(none);

        assertThat(offerAfterUpdate.getSupplierCategoryId()).isEqualTo(none);
        assertThat(offerAfterUpdate.getCategoryId()).isEqualTo(none);

        assertThat(offerAfterUpdate.getSupplierModelMappingId()).isEqualTo(none);
        assertThat(offerAfterUpdate.getModelId()).isEqualTo(none);
    }

    @Test
    public void testUpdateMappingForUnknownShopReturnsError() {
        var request = OfferMappings.UpdateOfferMappingsRequest.newBuilder()
            .addUpdates(OfferMappings.OfferMappingsUpdate.newBuilder()
                .setId(defaultOfferID())
                .addAllSkuUpdates(List.of(
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(21L).setType(SUPPLIER).build()
                ))
                .build())
            .build();

        setModels(
            new Model()
                .setId(21L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
        );

        OfferMappings.UpdateOfferMappingsResponse response = service.updateMappings(request);
        assertThat(response.getResultsList()).usingRecursiveFieldByFieldElementComparator().containsExactly(
            OfferMappings.MappingsUpdateResult.newBuilder()
                .setId(defaultOfferID())
                .setStatus(OfferMappings.MappingsUpdateResult.Status.BAD_REQUEST)
                .build()
        );
    }

    @Test
    public void testUpdateMappingForNewShop() {
        var request = OfferMappings.UpdateOfferMappingsRequest.newBuilder()
            .addUpdates(OfferMappings.OfferMappingsUpdate.newBuilder()
                .setId(defaultOfferID())
                .addAllSkuUpdates(List.of(
                    OfferMappings.SkuMappingUpdate.newBuilder().setSkuId(21L).setType(SUPPLIER).build()
                ))
                .build())
            .build();

        setModels(
            new Model()
                .setId(21L)
                .setCategoryId(1L)
                .setModelType(Model.ModelType.SKU)
        );

        Mockito.when(mbiApiClientMock.getShop(SHOP_ID)).thenReturn(
            new Shop(SHOP_ID, "SMB shop", null, null, null, null, null, null, false, false, null, null, null, false));

        OfferMappings.UpdateOfferMappingsResponse response = service.updateMappings(request);
        assertThat(response.getResultsList()).usingRecursiveFieldByFieldElementComparator().containsExactly(
            OfferMappings.MappingsUpdateResult.newBuilder()
                .setId(defaultOfferID())
                .setStatus(OfferMappings.MappingsUpdateResult.Status.OK)
                .build()
        );
        List<Supplier> addedSupplier = supplierRepository.find(
            SupplierFilter.builder()
                .supplierIds(Collections.singletonList(SHOP_ID))
                .build());
        assertThat(addedSupplier).containsExactlyInAnyOrder(new Supplier().setId(SHOP_ID).setName("SMB shop"));
    }

    private OfferMappings.GetMappingsRequest defaultGetMappingsRequest() {
        return OfferMappings.GetMappingsRequest.newBuilder()
            .addIds(defaultOfferID())
            .build();
    }

    private OfferMappings.OfferID defaultOfferID() {
        return OfferMappings.OfferID.newBuilder().setShopId(SHOP_ID).setShopSku(SHOP_SKU).build();
    }

    private void setModels(Model... models) {
        when(mboModelsService.loadAllModels(any())).thenReturn(
            Stream.of(models).collect(Collectors.groupingBy(Model::getId))
        );
    }
}
