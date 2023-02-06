package ru.yandex.market.ir.autogeneration_api.http.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.UpdateContentProcessingTasksRequest.ContentProcessingTask;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mboc.http.SupplierOffer.Offer.InternalProcessingStatus;

public class MboMappingsServiceMock implements MboMappingsService {
    private final Map<ShopSkuKey, SkuMappingValue> byKeyIndex = new HashMap<>();
    private final Map<ShopSkuKey, SupplierOffer.ApprovedMappingConfidence> confidenceByKeyIndex = new HashMap<>();
    private final Map<Long, Set<ShopSkuKey>> byMskuId = new HashMap<>();
    private final Map<ShopSkuKey, String> errorByKeyIndex = new HashMap<>();
    private final Map<ShopSkuKey, Error> updateMappingsErrorByKeyIndex = new HashMap<>();
    private final Map<ShopSkuKey, SimpleOffer> offerByKeyIndex = new HashMap<>();
    private final Map<ShopSkuKey, ContentProcessingTask.State> statesByKey = new HashMap<>();

    @Override
    public MboMappings.ProviderProductInfoResponse addProductInfo(
        MboMappings.ProviderProductInfoRequest providerProductInfoRequest
    ) {
        MboMappings.ProviderProductInfoResponse.Builder builder = MboMappings.ProviderProductInfoResponse.newBuilder();

        for (MboMappings.ProviderProductInfo product : providerProductInfoRequest.getProviderProductInfoList()) {
            ShopSkuKey key = new ShopSkuKey(product.getShopId(), product.getShopSkuId());

            String error = errorByKeyIndex.get(key);
            if (error != null) {
                builder.addResults(
                    MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                        .addErrors(
                            MboMappings.ProviderProductInfoResponse.Error.newBuilder()
                                .setErrorKind(MboMappings.ProviderProductInfoResponse.ErrorKind.OTHER)
                                .setMessage(error)
                                .build()
                        )
                        .build()
                );
                continue;
            }

            SkuMappingValue skuMappingValue = byKeyIndex.get(key);

            if (skuMappingValue == null) {
                byKeyIndex.put(key, new SkuMappingValue(product.getMarketSkuId(), product.getMarketCategoryId()));

                builder.addResults(
                    MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build()
                );
                continue;
            }

            if (skuMappingValue.marketSkuId == product.getMarketSkuId()) {
                builder.addResults(
                    MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build()
                );
                continue;
            }

            builder.addResults(
                MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                    .addErrors(
                        MboMappings.ProviderProductInfoResponse.Error.newBuilder()
                            .setErrorKind(MboMappings.ProviderProductInfoResponse.ErrorKind.HAS_APPROVED_MAPPING)
                            .setMessage("")
                            .build()
                    )
                    .build()
            );
        }

        return builder.build();
    }

    @Override
    public MboMappings.ProviderProductInfoResponse updateMappings(MboMappings.UpdateMappingsRequest updateMappingsRequest) {
        MboMappings.ProviderProductInfoResponse.Builder builder = MboMappings.ProviderProductInfoResponse.newBuilder();
        for (MboMappings.UpdateMappingsRequest.MappingUpdate mappingUpdate : updateMappingsRequest.getUpdatesList()) {
            ShopSkuKey key = new ShopSkuKey(mappingUpdate.getSupplierId(), mappingUpdate.getShopSku());

            Error error = updateMappingsErrorByKeyIndex.get(key);
            if (error != null) {
                builder.addResults(
                    MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                        .addErrors(
                            MboMappings.ProviderProductInfoResponse.Error.newBuilder()
                                .setErrorKind(error.getErrorKind())
                                .setMessage(error.getMessage())
                                .build()
                        )
                        .build()
                );
                continue;
            }

            SkuMappingValue skuMappingValue = byKeyIndex.get(key);

            if (skuMappingValue == null) {
                builder.addResults(
                    MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                        .addErrors(
                            MboMappings.ProviderProductInfoResponse.Error.newBuilder()
                                .setErrorKind(MboMappings.ProviderProductInfoResponse.ErrorKind.OTHER)
                                .setMessage("Not found mapping for " + key)
                                .build()
                        )
                        .build()
                );
                continue;
            }

            builder.addResults(
                MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                    .build()
            );

            if (skuMappingValue.marketSkuId != mappingUpdate.getMarketSkuId()) {
                final SkuMappingValue newSkuMappingValue = new SkuMappingValue(
                    mappingUpdate.getMarketSkuId(), skuMappingValue.categoryId
                );

                byKeyIndex.put(key, newSkuMappingValue);

                byMskuId.computeIfAbsent(
                        mappingUpdate.getMarketSkuId(), c -> new HashSet<>()
                ).add(key);
                byMskuId.remove(skuMappingValue.marketSkuId);
            }
        }

        return builder.build();
    }

    @Override
    public MboMappings.UpdateAvailabilityResponse updateAvailability(
            MboMappings.UpdateAvailabilityRequest updateAvailabilityRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateAntiMappingsResponse updateAntiMappings(
            MboMappings.UpdateAntiMappingsRequest updateAntiMappingsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchVendorsResponse searchVendorsByShopId(
            MboMappings.SearchVendorsBySupplierIdRequest searchVendorsBySupplierIdRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByShopId(
            MboMappings.SearchMappingsBySupplierIdRequest searchMappingsBySupplierIdRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByMarketSkuId(
        MboMappings.SearchMappingsByMarketSkuIdRequest request
    ) {
        if (request.getMarketSkuIdCount() == 0) {
            return MboMappings.SearchMappingsResponse.newBuilder()
                .setMessage("Error")
                .setStatus(MboMappings.SearchMappingsResponse.Status.ERROR)
                .build();
        }
        final MboMappings.SearchMappingsResponse.Builder builder = MboMappings.SearchMappingsResponse.newBuilder();
        for (Long mSkuId : request.getMarketSkuIdList()) {
            final Set<ShopSkuKey> keys = byMskuId.get(mSkuId);

            for (ShopSkuKey key : keys) {
                final MboMappings.SearchMappingsResponse errorMessage = getSearchMappingsResponse(builder, key);
                if (errorMessage != null) {
                    return errorMessage;
                }
            };
        }
        return builder.build();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchBaseOfferMappingsByMarketSkuId(
            MboMappings.SearchBaseOfferMappingsByMarketSkuIdRequest request) {
        if (request.getMarketSkuIdCount() == 0) {
            return MboMappings.SearchMappingsResponse.newBuilder()
                    .setMessage("Error")
                    .setStatus(MboMappings.SearchMappingsResponse.Status.ERROR)
                    .build();
        }
        final MboMappings.SearchMappingsResponse.Builder builder = MboMappings.SearchMappingsResponse.newBuilder();
        for (Long mSkuId : request.getMarketSkuIdList()) {
            final Set<ShopSkuKey> keys = byMskuId.getOrDefault(mSkuId, new HashSet<>());

            for (ShopSkuKey key : keys) {
                final MboMappings.SearchMappingsResponse errorMessage = getSearchMappingsResponse(builder, key);
                if (errorMessage != null) {
                    return errorMessage;
                }
            };
        }
        return builder.build();
    }

    private MboMappings.SearchMappingsResponse getSearchMappingsResponse(
        MboMappings.SearchMappingsResponse.Builder builder,
        ShopSkuKey key
    ) {
        final String errorMessage = errorByKeyIndex.get(key);
        if (errorMessage != null) {
            return builder
                .clearOffers()
                .setMessage(errorMessage)
                .setStatus(MboMappings.SearchMappingsResponse.Status.ERROR)
                .build();
        }
        final SimpleOffer simpleOffer = offerByKeyIndex.get(key);
        final SkuMappingValue skuMappingValue = byKeyIndex.get(key);
        if (skuMappingValue != null) {
            SupplierOffer.Offer.Builder offerBuilder = SupplierOffer.Offer.newBuilder()
                .setSupplierId(key.supplierId)
                .setShopSkuId(key.shopSku)
                .setCategoryRestriction(SupplierOffer.Offer.CategoryRestriction.newBuilder()
                    .setType(SupplierOffer.Offer.CategoryRestriction.AllowedType.ANY)
                    .build()
                );
            if (simpleOffer != null) {
                offerBuilder.setMarketCategoryId(simpleOffer.categoryId)
                    .setTitle(simpleOffer.title)
                    .setInternalProcessingStatus(simpleOffer.status)
                    .setBarcode(simpleOffer.barcode)
                    .setShopVendor(simpleOffer.vendor);
            }
            offerBuilder.setMarketCategoryId(skuMappingValue.categoryId)
                .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                    .setSkuId(skuMappingValue.marketSkuId)
                    .setCategoryId(skuMappingValue.categoryId)
                    .build());
            SupplierOffer.ApprovedMappingConfidence approvedMappingConfidence = confidenceByKeyIndex.get(key);
            if (approvedMappingConfidence != null) {
                offerBuilder.setApprovedMappingConfidence(approvedMappingConfidence);
            }
            builder.addOffers(offerBuilder.build());
        } else {
            if (simpleOffer != null) {
                SupplierOffer.Offer.Builder offerBuilder = SupplierOffer.Offer.newBuilder()
                    .setSupplierId(key.supplierId)
                    .setShopSkuId(key.shopSku)
                    .setMarketCategoryId(simpleOffer.categoryId)
                    .setTitle(simpleOffer.title)
                    .setInternalProcessingStatus(simpleOffer.status)
                    .setBarcode(simpleOffer.barcode)
                    .setShopVendor(simpleOffer.vendor)
                    .setCategoryRestriction(SupplierOffer.Offer.CategoryRestriction.newBuilder()
                        .setType(SupplierOffer.Offer.CategoryRestriction.AllowedType.ANY)
                        .build()
                    );

                if (simpleOffer.modelId != null) {
                    offerBuilder.setMarketModelId(simpleOffer.modelId);
                    offerBuilder.setApprovedMapping(SupplierOffer.Mapping.getDefaultInstance());
                }

                builder.addOffers(offerBuilder.build());
            }
        }
        return null;
    }

    private MboMappings.SearchLiteMappingsResponse getSearchLiteMappingsResponse(
            MboMappings.SearchLiteMappingsResponse.Builder builder,
            ShopSkuKey key
    ) {
        final String errorMessage = errorByKeyIndex.get(key);
        if (errorMessage != null) {
            return builder.build();
        }
        final SimpleOffer simpleOffer = offerByKeyIndex.get(key);
        final SkuMappingValue skuMappingValue = byKeyIndex.get(key);
        MbocCommon.MappingInfoLite.Builder mappingInfoLiteBuilder = MbocCommon.MappingInfoLite.newBuilder();
        mappingInfoLiteBuilder
                .setSupplierId(key.supplierId)
                .setShopSku(key.shopSku);
        if (skuMappingValue != null) {
            mappingInfoLiteBuilder
                    .setModelId(skuMappingValue.marketSkuId)
                    .setCategoryId(skuMappingValue.categoryId);
        }
        if (simpleOffer != null) {
            mappingInfoLiteBuilder
                    .setGroupId(simpleOffer.groupId);
        }
        builder.addMapping(mappingInfoLiteBuilder.build());
        return builder.build();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByKeys(MboMappings.SearchMappingsByKeysRequest request) {
        if (request.getKeysCount() == 0) {
            return MboMappings.SearchMappingsResponse.newBuilder()
                .setMessage("Error")
                .setStatus(MboMappings.SearchMappingsResponse.Status.ERROR)
                .build();
        }
        final MboMappings.SearchMappingsResponse.Builder builder = MboMappings.SearchMappingsResponse.newBuilder();
        for (MboMappings.SearchMappingsByKeysRequest.ShopSkuKey shopSkuKey : request.getKeysList()) {
            final ShopSkuKey key = new ShopSkuKey(shopSkuKey.getSupplierId(), shopSkuKey.getShopSku());
            final MboMappings.SearchMappingsResponse errorMessage = getSearchMappingsResponse(builder, key);
            if (errorMessage != null) {
                return errorMessage;
            }
        }
        return builder.build();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByBusinessKeys(
        MboMappings.SearchMappingsByBusinessKeysRequest searchMappingsByBusinessKeysRequest) {
        MboMappings.SearchMappingsByKeysRequest request = MboMappings.SearchMappingsByKeysRequest.newBuilder()
            .addAllKeys(
                searchMappingsByBusinessKeysRequest.getKeysList().stream()
                    .map(k -> MboMappings.SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                        .setShopSku(k.getOfferId())
                        .setSupplierId(k.getBusinessId())
                        .build()
                    )
                .collect(Collectors.toList())
            ).build();
        return searchMappingsByKeys(request);
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsForContentLab(MboMappings.SearchMappingsForContentLab searchMappingsForContentLab) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchProductInfoByYtStampResponse searchProductInfoByYtStamp(MboMappings.SearchProductInfoByYtStampRequest searchProductInfoByYtStampRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchProductInfoLiteByYtStampResponse searchProductInfoLiteByYtStamp(MboMappings.SearchProductInfoByYtStampRequest searchProductInfoByYtStampRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchProductInfoLiteByYtStampResponse searchProductInfoLiteByYtStampNotDataCamp(MboMappings.SearchProductInfoByYtStampRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchAntiMappingInfoByStampResponse searchAntiMappingInfoByStamp(MboMappings.SearchProductInfoByYtStampRequest searchProductInfoByYtStampRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchOfferAntiMappingsResponse searchAntiMappingsByMarketSkuId(MboMappings.SearchAntiMappingsByMarketSkuIdRequest searchAntiMappingsByMarketSkuIdRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingCategoriesResponse searchMappingCategoriesByShopId(MboMappings.SearchMappingCategoriesRequest searchMappingCategoriesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchOfferProcessingStatusesResponse searchOfferProcessingStatusesByShopId(MboMappings.SearchOfferProcessingStatusesRequest searchOfferProcessingStatusesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchApprovedMappingsResponse searchApprovedMappingsByMarketSkuId(MboMappings.SearchApprovedMappingsRequest searchApprovedMappingsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchApprovedMappingsResponse searchApprovedMappingsByKeys(
        MboMappings.SearchMappingsByKeysRequest searchMappingsByKeysRequest
    ) {
        MboMappings.SearchMappingsResponse searchMappingsResponse = searchMappingsByKeys(searchMappingsByKeysRequest);
        if (searchMappingsResponse.getStatus() != MboMappings.SearchMappingsResponse.Status.OK) {
            throw new RuntimeException(searchMappingsResponse.getMessage());
        }
        List<MboMappings.ApprovedMappingInfo> infos = searchMappingsResponse.getOffersList()
            .stream()
            .filter(SupplierOffer.Offer::hasApprovedMapping)
            .map(offer -> {
                MboMappings.ApprovedMappingInfo.Builder infoBuilder = MboMappings.ApprovedMappingInfo.newBuilder();
                infoBuilder.setMarketSkuId(offer.getApprovedMapping().getSkuId());
                infoBuilder.setSupplierId(Math.toIntExact(offer.getSupplierId()));
                infoBuilder.setShopSku(offer.getShopSkuId());
                infoBuilder.setMarketCategoryId(offer.getMarketCategoryId());
                return infoBuilder.build();
            })
            .collect(Collectors.toList());

        return MboMappings.SearchApprovedMappingsResponse.newBuilder()
            .addAllMapping(infos)
            .build();
    }

    @Override
    public MboMappings.SearchApprovedMappingsResponse searchApprovedMappingsByBusinessKeys(
        MboMappings.SearchMappingsByBusinessKeysRequest searchMappingsByBusinessKeysRequest
    ) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public MboMappings.SearchLiteMappingsResponse searchLiteApprovedMappingsByKeys(
        MboMappings.SearchLiteMappingsByKeysRequest searchLiteMappingsByKeysRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchLiteMappingsResponse searchLiteApprovedMappingsByMarketSkuId(
            MboMappings.SearchLiteMappingsByMarketSkuIdRequest request
    ) {
        final MboMappings.SearchLiteMappingsResponse.Builder builder =
                MboMappings.SearchLiteMappingsResponse.newBuilder();
        for (Long mSkuId : request.getMarketSkuIdList()) {
            final Set<ShopSkuKey> keys = byMskuId.getOrDefault(mSkuId, new HashSet<>());
            for (ShopSkuKey key : keys) {
                getSearchLiteMappingsResponse(builder, key);
            }
            ;
        }
        return builder.build();
    }

    @Override
    public MboMappings.OfferExcelUpload.Response uploadExcelFile(MboMappings.OfferExcelUpload.Request request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Response uploadExcelFileStreamed(MboMappings.OfferExcelUpload.Request request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Response getUploadStatus(MboMappings.OfferExcelUpload.ProcessingRequest processingRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Result downloadUploadResult(MboMappings.OfferExcelUpload.ResultRequest resultRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.StreamedResult downloadUploadStreamedResult(MboMappings.OfferExcelUpload.ResultRequest resultRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateOfferProcessingStatusResponse updateOfferProcessingStatus(MboMappings.UpdateOfferProcessingStatusRequest updateOfferProcessingStatusRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateContentProcessingTasksResponse updateContentProcessingTasks(
        MboMappings.UpdateContentProcessingTasksRequest request
    ) {
        request.getContentProcessingTaskList().forEach(t -> {
                ShopSkuKey shopSkuKey = new ShopSkuKey(t.getSupplierId(), t.getShopSku());
                SimpleOffer offer = offerByKeyIndex.get(shopSkuKey);
                if (offer != null) {
                    switch (t.getContentProcessingState()) {
                        case STARTED:
                            offerByKeyIndex.put(shopSkuKey, new SimpleOffer(offer.categoryId, offer.title,
                                offer.barcode, offer.vendor, InternalProcessingStatus.CONTENT_PROCESSING));
                            break;
                        case SUCCESS:
                            offerByKeyIndex.put(shopSkuKey, new SimpleOffer(offer.categoryId, offer.title,
                                offer.barcode, offer.vendor, InternalProcessingStatus.AUTO_PROCESSED));
                            break;
                        case ERROR:
                            if (byKeyIndex.containsKey(shopSkuKey)) {
                                offerByKeyIndex.put(shopSkuKey, new SimpleOffer(offer.categoryId, offer.title,
                                    offer.barcode, offer.vendor, InternalProcessingStatus.AUTO_PROCESSED));
                            } else {
                                offerByKeyIndex.put(shopSkuKey, new SimpleOffer(offer.categoryId, offer.title,
                                    offer.barcode, offer.vendor, InternalProcessingStatus.NEED_CONTENT));
                            }
                    }
                }
                statesByKey.put(shopSkuKey, t.getContentProcessingState());
            }
        );
        return MboMappings.UpdateContentProcessingTasksResponse.newBuilder()
            .setStatus(MboMappings.UpdateContentProcessingTasksResponse.Status.OK)
            .setMessage("Updated offers total: " + request.getContentProcessingTaskCount())
            .build();
    }

    @Override
    public MboMappings.ReprocessOffersClassificationResponse reprocessOffersClassification(
        MboMappings.ReprocessOffersClassificationRequest reprocessOffersClassificationRequest
    ) {
        MboMappings.ReprocessOffersClassificationResponse.Builder responseBuilder =
            MboMappings.ReprocessOffersClassificationResponse.newBuilder();

        reprocessOffersClassificationRequest.getSkuKeyList()
            .stream()
            .map(r -> MboMappings.ReprocessOffersClassificationResponse.ProcessedOfferResult.newBuilder()
                .setSkuKey(MbocCommon.ShopSkuKey.newBuilder()
                    .setSupplierId(r.getSupplierId())
                    .setSupplierSkuId(r.getSupplierSkuId())
                    .build())
                .setStatus(MboMappings.ReprocessOffersClassificationResponse.Status.ALREADY_IN_REPROCESS)
                .build())
            .forEach(responseBuilder::addOfferResults);

        return responseBuilder.build();
    }

    @Override
    public MboMappings.CountBusinessOffersResponse countBusinessOffers(MboMappings.CountBusinessOffersRequest countBusinessOffersRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SKUMappingResponse getShopSKU(MboMappings.ShopSKURequest shopSKURequest) {
        return null;
    }

    @Override
    public MboMappings.AddToContentProcessingResponse addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest request) {
        return null;
    }

    @Override
    public MboMappings.GetAssortmentChildSskusResponse getAssortmentChildSskus(MboMappings.GetAssortmentChildSskusRequest request) {
        return null;
    }

    @Override
    public MboMappings.SendToRecheckMappingResponse sendToRecheck(MboMappings.SendToRecheckMappingRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.ResetMappingResponse resetMapping(MboMappings.ResetMappingRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult ping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult monitoring() {
        throw new UnsupportedOperationException();
    }


    public void addMapping(long categoryId, int supplierId, String shopSku, long mSkuId) {
        final ShopSkuKey key = new ShopSkuKey(supplierId, shopSku);
        byKeyIndex.put(key, new SkuMappingValue(mSkuId, categoryId));
        byMskuId.computeIfAbsent(
                mSkuId, c -> new HashSet<>()
        ).add(key);
    }

    public void addMapping(long categoryId,
                           int supplierId,
                           String shopSku,
                           long mSkuId,
                           SupplierOffer.ApprovedMappingConfidence mappingConfidence
    ) {
        final ShopSkuKey key = new ShopSkuKey(supplierId, shopSku);
        byKeyIndex.put(key, new SkuMappingValue(mSkuId, categoryId));
        byMskuId.computeIfAbsent(
                mSkuId, c -> new HashSet<>()
        ).add(key);
        confidenceByKeyIndex.put(key, mappingConfidence);
    }

    public void addErrorMapping(int supplierId, String shopSku, String errorMessage) {
        errorByKeyIndex.put(new ShopSkuKey(supplierId, shopSku), errorMessage);
    }

    public void addUpdateMappingError(int supplierId,
                                      String shopSku,
                                      String errorMessage,
                                      MboMappings.ProviderProductInfoResponse.ErrorKind errorKind) {
        updateMappingsErrorByKeyIndex.put(new ShopSkuKey(supplierId, shopSku),
            new Error(errorMessage, errorKind));
    }

    public void removeUpdateMappingError(int supplierId, String shopSku) {
        updateMappingsErrorByKeyIndex.remove(new ShopSkuKey(supplierId, shopSku));
    }

    public void addOfferMapping(int supplierId,
                                String shopSku,
                                long categoryId,
                                String title,
                                String barcode,
                                String vendor,
                                InternalProcessingStatus status) {
        offerByKeyIndex.put(
            new ShopSkuKey(supplierId, shopSku),
            new SimpleOffer(categoryId, title, barcode, vendor, status)
        );
    }

    public void addOfferMapping(int supplierId,
                                String shopSku,
                                long categoryId,
                                String title,
                                String barcode,
                                String vendor,
                                InternalProcessingStatus status,
                                Long modelId) {
        offerByKeyIndex.put(
            new ShopSkuKey(supplierId, shopSku),
            new SimpleOffer(categoryId, title, barcode, vendor, status, modelId)
        );
    }

    public void addOfferMapping(int supplierId,
                                String shopSku,
                                long categoryId,
                                String title,
                                String barcode,
                                String vendor,
                                InternalProcessingStatus status,
                                Long modelId,
                                int groupId) {
        final ShopSkuKey key = new ShopSkuKey(supplierId, shopSku);
        offerByKeyIndex.put(
                key,
                new SimpleOffer(categoryId, title, barcode, vendor, status, modelId, groupId)
        );
        byKeyIndex.put(key, new SkuMappingValue(modelId, categoryId));
        byMskuId.computeIfAbsent(
                modelId, c -> new HashSet<>()
        ).add(key);
    }

    public ContentProcessingTask.State getMappingState(int supplierId, String shopSku) {
        return statesByKey.get(new ShopSkuKey(supplierId, shopSku));
    }

    public long getMappingSkuId(int supplierId, String shopSku) {
        return byKeyIndex.get(new ShopSkuKey(supplierId, shopSku)).marketSkuId;
    }

    private static final class SimpleOffer {
        private final long categoryId;
        private final String title;
        private final String barcode;
        private final String vendor;
        private final InternalProcessingStatus status;
        private final Long modelId;
        private final int groupId;

        private SimpleOffer(long categoryId, String title, String barcode, String vendor, InternalProcessingStatus status) {
            this(categoryId, title, barcode, vendor, status, null);
        }

        private SimpleOffer(long categoryId,
                            String title,
                            String barcode,
                            String vendor,
                            InternalProcessingStatus status,
                            Long modelId) {
            this(categoryId, title, barcode, vendor, status, modelId, 0);
        }

        private SimpleOffer(long categoryId,
                            String title,
                            String barcode,
                            String vendor,
                            InternalProcessingStatus status,
                            Long modelId,
                            int groupId) {
            this.categoryId = categoryId;
            this.title = title;
            this.barcode = barcode;
            this.vendor = vendor;
            this.status = status;
            this.modelId = modelId;
            this.groupId = groupId;
        }
    }

    private final class SkuMappingValue {
        private final long marketSkuId;
        private final long categoryId;

        private SkuMappingValue(long marketSkuId, long categoryId) {
            this.marketSkuId = marketSkuId;
            this.categoryId = categoryId;
        }
    }

    private class Error {
        private final String message;
        private final MboMappings.ProviderProductInfoResponse.ErrorKind errorKind;


        private Error(String message, MboMappings.ProviderProductInfoResponse.ErrorKind errorKind) {
            this.message = message;
            this.errorKind = errorKind;
        }

        public String getMessage() {
            return message;
        }

        public MboMappings.ProviderProductInfoResponse.ErrorKind getErrorKind() {
            return errorKind;
        }
    }

    private final class ShopSkuKey {
        private final int supplierId;
        private final String shopSku;


        private ShopSkuKey(int supplierId, String shopSku) {
            this.supplierId = supplierId;
            this.shopSku = shopSku;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ShopSkuKey that = (ShopSkuKey) o;
            return supplierId == that.supplierId &&
                Objects.equals(shopSku, that.shopSku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(supplierId, shopSku);
        }

        @Override
        public String toString() {
            return "ShopSkuKey{" +
                "supplierId=" + supplierId +
                ", shopSku='" + shopSku + '\'' +
                '}';
        }
    }
}
