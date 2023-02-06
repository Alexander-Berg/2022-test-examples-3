package ru.yandex.market.mboc.app.proto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountryUtil;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MasterDataProto.SearchMskuMasterDataRequest;
import ru.yandex.market.mdm.http.MasterDataProto.SearchMskuMasterDataResponse;
import ru.yandex.market.mdm.http.MasterDataService;
import ru.yandex.market.mdm.http.MdmCommon;

/**
 * @author moskovkin@yandex-team.ru
 * @since 01.07.19
 */
public class MasterDataServiceMock implements MasterDataService {
    private final Map<MdmCommon.ShopSkuKey, MdmCommon.SskuMasterData> sskuMasterData = new HashMap<>();
    private final NavigableMap<Long, MdmCommon.ShopSkuKey> sskuMasterDataLogId = new TreeMap<>();
    private final AtomicLong logIdCounter = new AtomicLong(0);

    private final Map<MdmCommon.ShopSkuKey, MasterDataProto.OperationInfo> saveErrors = new HashMap<>();
    private final Map<Integer, List<MasterDataProto.OperationInfo>> iteratedErrors = new HashMap<>();
    private final List<MdmCommon.ShopSkuKey> logbrokerTopicStub = new ArrayList<>();
    private int saveIteration = 0;

    public Map<MdmCommon.ShopSkuKey, MdmCommon.SskuMasterData> getSskuMasterData() {
        return Collections.unmodifiableMap(sskuMasterData);
    }

    public List<MdmCommon.SskuMasterData> getConvertedToInternalMD(SupplierConverterService converter) {
        return sskuMasterData.values()
            .stream()
            .map(md -> {
                ShopSkuKey externalKey = new ShopSkuKey(md.getSupplierId(), md.getShopSku());
                ShopSkuKey internalKey = converter.convertRealToInternal(externalKey);
                return md.toBuilder().setSupplierId(internalKey.getSupplierId()).setShopSku(internalKey.getShopSku())
                    .build();
            }).collect(Collectors.toUnmodifiableList());
    }

    public void setSaveErrors(List<MasterDataProto.OperationInfo> errors) {
        saveErrors.clear();
        errors.forEach(oi -> saveErrors.put(oi.getKey(), oi));
    }

    public void setSaveErrorsOnIteration(int iterationToFailOn, List<MasterDataProto.OperationInfo> errors) {
        iteratedErrors.put(iterationToFailOn, errors);
    }

    @Override
    public MasterDataProto.SearchSskuMasterDataResponse searchSskuMasterData(
        MasterDataProto.SearchSskuMasterDataRequest request
    ) {
        List<MdmCommon.SskuMasterData> found = sskuMasterData.entrySet().stream()
            .filter(e -> request.getShopSkuKeysList().contains(e.getKey()))
            .map(e -> e.getValue())
            .collect(Collectors.toList());

        MasterDataProto.SearchSskuMasterDataResponse result = MasterDataProto.SearchSskuMasterDataResponse.newBuilder()
            .addAllSskuMasterData(found)
            .build();

        return result;
    }

    @Override
    public MasterDataProto.SaveSskuMasterDataResponse saveSskuMasterData(
        MasterDataProto.SaveSskuMasterDataRequest request
    ) {
        // Nth iteration failure simulation
        if (!iteratedErrors.isEmpty()) {
            if (iteratedErrors.containsKey(saveIteration)) {
                setSaveErrors(iteratedErrors.get(saveIteration));
            } else {
                saveErrors.clear();
            }
            ++saveIteration;
        }

        MasterDataProto.SaveSskuMasterDataResponse.Builder response = MasterDataProto.SaveSskuMasterDataResponse
            .newBuilder();

        for (MdmCommon.SskuMasterData md : request.getSskuMasterDataList()) {
            MdmCommon.ShopSkuKey key = MdmCommon.ShopSkuKey.newBuilder()
                .setSupplierId(md.getSupplierId())
                .setShopSku(md.getShopSku())
                .build();

            if (saveErrors.get(key) != null) {
                response.addResults(saveErrors.get(key));
                continue;
            }

            if (!(request.hasValidateOnly() && request.getValidateOnly())) {
                sskuMasterData.put(key, md);
                sskuMasterDataLogId.put(logIdCounter.incrementAndGet(), key);
            }

            response.addResults(MasterDataProto.OperationInfo.newBuilder()
                .setStatus(MasterDataProto.OperationStatus.OK)
                .setKey(key)
                .build()
            );
        }
        return response.build();
    }

    @Override
    public MasterDataProto.NoCountriesCutoffsStatisticsResponse noCountriesCutoffsStatistics(
        MasterDataProto.Empty empty
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MasterDataProto.SendSskuMappingUpdatedEventResponse sendSskuMappingUpdatedEvent(
        MasterDataProto.SendSskuMappingUpdatedEventRequest request
    ) {
        MasterDataProto.SendSskuMappingUpdatedEventResponse.Builder response =
            MasterDataProto.SendSskuMappingUpdatedEventResponse.newBuilder();

        for (MdmCommon.ShopSkuKey shopSkuKey : request.getShopSkuKeyList()) {
            logbrokerTopicStub.add(shopSkuKey);
            MasterDataProto.OperationInfo info = MasterDataProto.OperationInfo.newBuilder()
                .setKey(shopSkuKey)
                .setStatus(MasterDataProto.OperationStatus.OK)
                .build();
            response.addResult(info);
        }
        return response.build();
    }

    @Override
    public SearchMskuMasterDataResponse searchMskuMasterData(SearchMskuMasterDataRequest searchMskuMasterDataRequest) {
        throw new UnsupportedOperationException("searchMskuMasterData is not implemented");
    }

    @Override
    public MasterDataProto.SearchManufacturerCountryResponse searchManufacturerCountry(
        MasterDataProto.SearchManufacturerCountryRequest request) {

        long fromSequenceId = request.getFromSequenceId();
        long count = request.getCount();

        List<Long> seqIds = sskuMasterDataLogId.navigableKeySet()
            .tailSet(fromSequenceId, false)
            .stream()
            .limit(count)
            .collect(Collectors.toList());
        var response = MasterDataProto.SearchManufacturerCountryResponse.newBuilder();

        if (seqIds.isEmpty()) {
            response.setLastSequenceId(0);
            return response.build();
        }

        response.setLastSequenceId(Collections.max(seqIds));
        for (Long seqId : seqIds) {
            MdmCommon.ShopSkuKey key = sskuMasterDataLogId.get(seqId);
            MdmCommon.SskuMasterData masterData = this.sskuMasterData.get(key);
            var info = MasterDataProto.ManufacturerCountryInfo.newBuilder();
            info.setShopSkuKey(key);

            masterData.getManufacturerCountriesList()
                .stream()
                .map(GeobaseCountryUtil::countryByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(c -> MasterDataProto.ManufacturerCountry.newBuilder()
                    .setRuName(c.getCanonicalName())
                    .setGeoId(c.getGeoId())
                )
                .forEach(info::addManufacturerCountry);
            response.addManufacturerCountryInfo(info);
        }
        return response.build();
    }

    public ImmutableList<MdmCommon.ShopSkuKey> getLogbrokerTopicContent() {
        return ImmutableList.copyOf(logbrokerTopicStub);
    }

    @Override
    public MasterDataProto.SearchSskuMasterDataResponse search1PSskuSilverData(
        MasterDataProto.SearchSskuMasterDataRequest request
    ) {
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
}
