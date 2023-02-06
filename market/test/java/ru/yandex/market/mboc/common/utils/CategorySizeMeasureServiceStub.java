package ru.yandex.market.mboc.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.export.MboSizeMeasures.CategorySizeMeasure;
import ru.yandex.market.mbo.export.MboSizeMeasures.GetCategorySizeMeasureResponse;
import ru.yandex.market.mbo.export.MboSizeMeasures.GetCategorySizeMeasuresRequest;
import ru.yandex.market.mbo.export.MboSizeMeasures.GetSizeMeasureInfoVendorRequest;
import ru.yandex.market.mbo.export.MboSizeMeasures.GetSizeMeasuresInfoRequest;
import ru.yandex.market.mbo.export.MboSizeMeasures.GetSizeMeasuresInfoResponse;
import ru.yandex.market.mbo.export.MboSizeMeasures.GetSizeMeasuresInfoVendorResponse;
import ru.yandex.market.mbo.export.MboSizeMeasures.SizeMeasure;

public class CategorySizeMeasureServiceStub implements CategorySizeMeasureService {

    private Map<Long, List<SizeMeasure>> sizeMeasureByCategoryId = new HashMap<>();
    private Map<Long, List<MboSizeMeasures.ScaleInfo>> scaleInfosByCategoryIds = new HashMap<>();

    @Override
    public GetCategorySizeMeasureResponse getSizeMeasures(
        GetCategorySizeMeasuresRequest getCategorySizeMeasuresRequest) {
        List<CategorySizeMeasure> sizeMeasures = getCategorySizeMeasuresRequest.getCategoryIdList().stream()
            .map(categoryId -> {
                List<MboSizeMeasures.SizeMeasure> categorySizeMeasures = sizeMeasureByCategoryId.get(categoryId);
                if (categorySizeMeasures == null) {
                    return null;
                }
                return MboSizeMeasures.CategorySizeMeasure.newBuilder()
                    .addAllSizeMeasure(categorySizeMeasures)
                    .setCategoryId(categoryId)
                    .build();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return MboSizeMeasures.GetCategorySizeMeasureResponse.newBuilder()
            .addAllCategorySizeMeasure(sizeMeasures)
            .build();
    }

    @Override
    public GetSizeMeasuresInfoResponse getSizeMeasuresInfo(GetSizeMeasuresInfoRequest getSizeMeasuresInfoRequest) {
        List<GetSizeMeasuresInfoResponse.CategoryResponse> sizeMeasureInfos =
            getSizeMeasuresInfoRequest.getCategoryIdsList().stream()
                .map(categoryId -> {
                    List<MboSizeMeasures.ScaleInfo> scaleInfos = scaleInfosByCategoryIds.get(categoryId);

                    if (scaleInfos == null) {
                        return null;
                    }

                    return MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse.newBuilder()
                        .setCategoryId(categoryId)
                        .addSizeMeasureInfos(MboSizeMeasures.SizeMeasureInfo.newBuilder()
                            .addAllScales(scaleInfos)
                            .build())
                        .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .addAllSizeMeasures(sizeMeasureInfos)
            .build();
    }

    @Override
    public GetSizeMeasuresInfoVendorResponse getSizeMeasuresVendorsInfo(
        GetSizeMeasureInfoVendorRequest getSizeMeasureInfoVendorRequest) {
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

    public void initializeSizeMeasures(Map<Long, List<SizeMeasure>> sizeMeasureByCategoryId) {
        this.sizeMeasureByCategoryId.putAll(sizeMeasureByCategoryId);
    }

    public void initializeScaleInfos(Map<Long, List<MboSizeMeasures.ScaleInfo>> scaleInfosByCategoryIds) {
        this.scaleInfosByCategoryIds = scaleInfosByCategoryIds;
    }

}
