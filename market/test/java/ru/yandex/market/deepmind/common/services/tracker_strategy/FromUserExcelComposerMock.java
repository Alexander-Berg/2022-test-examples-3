package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingService;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichSpecialOrderExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.ExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.services.yt.AbstractLoader;
import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtInfo;
import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtLoadRequest;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;

@Slf4j
public class FromUserExcelComposerMock extends ExcelComposer {

    private final List<Header> headers;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public FromUserExcelComposerMock(
        MskuRepository mskuRepository,
        SupplierRepository deepmindSupplierRepository,
        GlobalVendorsCachingService globalVendorsCachingService,
        MasterDataHelperService masterDataHelperService,
        ServiceOfferReplicaRepository serviceOfferReplicaRepository,
        DeepmindCategoryCachingService deepmindCategoryCachingService,
        DeepmindCategoryManagerRepository categoryManagerService,
        DeepmindCategoryTeamRepository categoryTeamRepository,
        MskuInfoRepository mskuInfoRepository,
        SeasonRepository seasonRepository,
        SeasonalMskuRepository seasonalMskuRepository,
        AbstractLoader<EnrichApproveToPendingYtInfo, EnrichApproveToPendingYtLoadRequest> mskuInfoFromYtLoader,
        OffersConverter offersConverter,
        SskuStatusRepository sskuStatusRepository,
        boolean isSpecialOrderStrategy) {
        super(
            mskuRepository,
            deepmindSupplierRepository,
            globalVendorsCachingService,
            masterDataHelperService,
            serviceOfferReplicaRepository,
            deepmindCategoryCachingService,
            categoryManagerService,
            categoryTeamRepository,
            mskuInfoRepository,
            seasonRepository,
            seasonalMskuRepository,
            mskuInfoFromYtLoader,
            offersConverter,
            sskuStatusRepository);
        headers = isSpecialOrderStrategy
            ? EnrichSpecialOrderExcelComposer.HEADERS : EnrichApproveToPendingExcelComposer.HEADERS;
    }

    public ExcelFile processKeys(Collection<ServiceOfferKey> sskus,
                                 HashMap<ServiceOfferKey, Map<String, SpecialOrderData>> skuWarehouseNameDataMap) {
        return processKeys(sskus, skuWarehouseNameDataMap, headers);
    }

    @Override
    protected void setQuantKeyRow(HashMap<ServiceOfferKey,
        Map<String, SpecialOrderData>> skuWarehouseNameDataMap,
                                  ExcelFile.Builder builder,
                                  ServiceOfferKey skuKey,
                                  int row,
                                  MasterData masterData,
                                  String header) {
        var specialOrderDataList = skuWarehouseNameDataMap.get(skuKey);
        var quantum = specialOrderDataList == null || specialOrderDataList.isEmpty() ?
            masterData == null ? null : masterData.getQuantumOfSupply() :
            specialOrderDataList.values().stream().findFirst().get().getShipmentQuantum();
        builder.setValue(row, header, quantum);
    }

    @Override
    public List<Header> getStaticHeaderList() {
        return headers;
    }

    @Override
    public List<String> getDynamicHeaderEndings() {
        return List.of(WAREHOUSE_FIRST_SUPPLY_CNT_ENDING, WAREHOUSE_ORDER_DATE_ENDING);
    }
}
