package ru.yandex.market.mboc.common.offers.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.SearchLiteMappingsByKeysRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchLiteMappingsByMarketSkuIdRequest;
import ru.yandex.market.mboc.http.MboMappings.SearchLiteMappingsResponse;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon.MappingInfoLite;

import static ru.yandex.market.mboc.http.MboMappings.ApprovedMappingInfo;

/**
 * @author eremeevvo
 */
public class MboMappingsServiceMock implements MboMappingsService {
    private final List<ApprovedMappingInfo> mappings = new ArrayList<>();

    public List<ApprovedMappingInfo> getMappings() {
        return new ArrayList<>(mappings);
    }

    public void addMapping(ApprovedMappingInfo mapping) {
        mappings.add(mapping);
    }

    public void addLiteMappings(ModelKey key, Collection<ShopSkuKey> sskuKeys) {
        long mskuId = key.getModelId();
        long categoryId = key.getCategoryId();
        sskuKeys.forEach(ssku -> {
            var approvedMappingInfo = ApprovedMappingInfo.newBuilder()
                .setMarketSkuId(mskuId)
                .setMarketCategoryId(categoryId)
                .setShopSku(ssku.getShopSku())
                .setSupplierId(ssku.getSupplierId());
            addMapping(approvedMappingInfo.build());
        });
    }

    public void replaceMapping(ApprovedMappingInfo existing, ApprovedMappingInfo updated) {
        int i = mappings.indexOf(existing);
        if (i < 0) {
            throw new NoSuchElementException("No such ApprovedMappingInfo in the mock");
        }
        mappings.set(i, updated);
    }

    public void removeMapping(ApprovedMappingInfo existing) {
        mappings.remove(existing);
    }

    public void clearMappings() {
        mappings.clear();
    }

    @Override
    public MboMappings.SearchApprovedMappingsResponse searchApprovedMappingsByMarketSkuId(
        MboMappings.SearchApprovedMappingsRequest request
    ) {
        MboMappings.SearchApprovedMappingsResponse.Builder response =
            MboMappings.SearchApprovedMappingsResponse.newBuilder();

        List<ApprovedMappingInfo> matchedMappings = mappings.stream()
            .filter(m -> request.getMarketSkuIdList().contains(m.getMarketSkuId()))
            .collect(Collectors.toList());

        response.addAllMapping(matchedMappings);
        return response.build();
    }

    @Override
    public MboMappings.SearchApprovedMappingsResponse searchApprovedMappingsByKeys(
        MboMappings.SearchMappingsByKeysRequest request
    ) {
        MboMappings.SearchApprovedMappingsResponse.Builder response =
            MboMappings.SearchApprovedMappingsResponse.newBuilder();

        List<ApprovedMappingInfo> matchedMappings = mappings.stream()
            .filter(m -> request.getKeysList().contains(
                MboMappings.SearchMappingsByKeysRequest.ShopSkuKey.newBuilder()
                    .setSupplierId(m.getSupplierId())
                    .setShopSku(m.getShopSku())
                    .build()
            ))
            .collect(Collectors.toList());

        response.addAllMapping(matchedMappings);
        return response.build();
    }

    @Override
    public MboMappings.SearchApprovedMappingsResponse searchApprovedMappingsByBusinessKeys(
        MboMappings.SearchMappingsByBusinessKeysRequest searchMappingsByBusinessKeysRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.ProviderProductInfoResponse addProductInfo(
        MboMappings.ProviderProductInfoRequest providerProductInfoRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.ProviderProductInfoResponse updateMappings(
        MboMappings.UpdateMappingsRequest updateMappingsRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateAvailabilityResponse updateAvailability(
        MboMappings.UpdateAvailabilityRequest updateAvailabilityRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateAntiMappingsResponse updateAntiMappings(MboMappings.UpdateAntiMappingsRequest updateAntiMappingsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchVendorsResponse searchVendorsByShopId(
        MboMappings.SearchVendorsBySupplierIdRequest searchVendorsBySupplierIdRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByShopId(
        MboMappings.SearchMappingsBySupplierIdRequest searchMappingsBySupplierIdRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByMarketSkuId(
        MboMappings.SearchMappingsByMarketSkuIdRequest searchMappingsByMarketSkuIdRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchBaseOfferMappingsByMarketSkuId(
        MboMappings.SearchBaseOfferMappingsByMarketSkuIdRequest searchBaseOfferMappingsByMarketSkuIdRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByKeys(
        MboMappings.SearchMappingsByKeysRequest searchMappingsByKeysRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsByBusinessKeys(
        MboMappings.SearchMappingsByBusinessKeysRequest searchMappingsByBusinessKeysRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingsResponse searchMappingsForContentLab(
        MboMappings.SearchMappingsForContentLab searchMappingsForContentLab
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchProductInfoByYtStampResponse searchProductInfoByYtStamp(
        MboMappings.SearchProductInfoByYtStampRequest searchProductInfoByYtStampRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchProductInfoLiteByYtStampResponse searchProductInfoLiteByYtStamp(
        MboMappings.SearchProductInfoByYtStampRequest searchProductInfoByYtStampRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchProductInfoLiteByYtStampResponse searchProductInfoLiteByYtStampNotDataCamp(
        MboMappings.SearchProductInfoByYtStampRequest request
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchAntiMappingInfoByStampResponse searchAntiMappingInfoByStamp(
        MboMappings.SearchProductInfoByYtStampRequest searchProductInfoByYtStampRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchOfferAntiMappingsResponse searchAntiMappingsByMarketSkuId(MboMappings.SearchAntiMappingsByMarketSkuIdRequest searchAntiMappingsByMarketSkuIdRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchMappingCategoriesResponse searchMappingCategoriesByShopId(
        MboMappings.SearchMappingCategoriesRequest searchMappingCategoriesRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SearchOfferProcessingStatusesResponse searchOfferProcessingStatusesByShopId(
        MboMappings.SearchOfferProcessingStatusesRequest searchOfferProcessingStatusesRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Response uploadExcelFile(MboMappings.OfferExcelUpload.Request request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Response uploadExcelFileStreamed(
        MboMappings.OfferExcelUpload.Request request
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Response getUploadStatus(
        MboMappings.OfferExcelUpload.ProcessingRequest processingRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.Result downloadUploadResult(
        MboMappings.OfferExcelUpload.ResultRequest resultRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.OfferExcelUpload.StreamedResult downloadUploadStreamedResult(
        MboMappings.OfferExcelUpload.ResultRequest resultRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateOfferProcessingStatusResponse updateOfferProcessingStatus(
        MboMappings.UpdateOfferProcessingStatusRequest updateOfferProcessingStatusRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.UpdateContentProcessingTasksResponse updateContentProcessingTasks(
        MboMappings.UpdateContentProcessingTasksRequest updateContentProcessingTasksRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.ReprocessOffersClassificationResponse reprocessOffersClassification(MboMappings.ReprocessOffersClassificationRequest reprocessOffersClassificationRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchLiteMappingsResponse searchLiteApprovedMappingsByKeys(SearchLiteMappingsByKeysRequest request) {
        MboMappings.SearchLiteMappingsResponse.Builder result = MboMappings.SearchLiteMappingsResponse
            .newBuilder();

        Set<ShopSkuKey> keys = request.getKeysList()
            .stream().map(k -> new ShopSkuKey(k.getSupplierId(), k.getShopSku())).collect(Collectors.toSet());

        List<ApprovedMappingInfo> offers = mappings.stream()
            .filter(o -> keys.contains(new ShopSkuKey(o.getSupplierId(), o.getShopSku())))
            .collect(Collectors.toList());

        List<MappingInfoLite> approvedMappingInfos = offers.stream()
            .map(this::offerToMappingInfoLite)
            .filter(mapping -> mapping.hasModelId() && mapping.getModelId() != 0)
            .collect(Collectors.toList());

        result.addAllMapping(approvedMappingInfos);
        return result.build();
    }

    @Override
    public SearchLiteMappingsResponse searchLiteApprovedMappingsByMarketSkuId(
        SearchLiteMappingsByMarketSkuIdRequest request
    ) {
        MboMappings.SearchLiteMappingsResponse.Builder result = MboMappings.SearchLiteMappingsResponse
            .newBuilder();

        Set<Long> mskuIds = new HashSet<>(request.getMarketSkuIdList());

        List<ApprovedMappingInfo> offers = mappings.stream()
            .filter(o -> mskuIds.contains(o.getMarketSkuId()))
            .collect(Collectors.toList());

        List<MappingInfoLite> approvedMappingInfos = offers.stream()
            .map(this::offerToMappingInfoLite)
            .filter(mapping -> mapping.hasModelId() && mapping.getModelId() != 0)
            .collect(Collectors.toList());

        result.addAllMapping(approvedMappingInfos);
        return result.build();
    }

    @Override
    public MboMappings.CountBusinessOffersResponse countBusinessOffers(
        MboMappings.CountBusinessOffersRequest countBusinessOffersRequest) {
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

    @Override
    public MboMappings.SKUMappingResponse getShopSKU(MboMappings.ShopSKURequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.AddToContentProcessingResponse addOfferToContentProcessing(MboMappings.AddToContentProcessingRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.GetAssortmentChildSskusResponse getAssortmentChildSskus(MboMappings.GetAssortmentChildSskusRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.SendToRecheckMappingResponse sendToRecheck(MboMappings.SendToRecheckMappingRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboMappings.ResetMappingResponse resetMapping(MboMappings.ResetMappingRequest request) {
        throw new UnsupportedOperationException();
    }

    private MappingInfoLite offerToMappingInfoLite(ApprovedMappingInfo info) {
        MappingInfoLite.Builder builder = MappingInfoLite.newBuilder()
            .setSupplierId(info.getSupplierId())
            .setModelId(info.getMarketSkuId())
            .setShopSku(info.getShopSku());
        if (info.hasMarketCategoryId()) {
            builder.setCategoryId(info.getMarketCategoryId());
        }
        return builder.build();
    }
}
