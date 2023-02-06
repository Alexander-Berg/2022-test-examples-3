package ru.yandex.market.checkerxservice.testdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbTestData {
    public static class TokenAndRegionData {
        public String userEsiaToken;
        public Integer regionCode;
    }

    private static DbTestData dbTestData;
    // <offerId + uid, guids>
    private Map<String, String> offerToGuidsMap = new HashMap<>();
    // <uid, Pair<esiaToken, regionId>>
    private Map<Long, TokenAndRegionData> uidToEsiaTokenMap = new HashMap<>();
    // <BarCode, Esklp>
    private Map<String, String> barcodeToEsklpMap = new HashMap<>();
    // Костыль для ReadingDao
    // Каждая строка - это список offerId, переданных в запрос ReadingDao
    private Map<String, List<String>> offerIdsMap = new HashMap<>();

    private DbTestData() {
        loadTestData();
    }

    public static DbTestData getInstance() {
        if (dbTestData == null) {
            dbTestData = new DbTestData();
        }
        return dbTestData;
    }

    private void loadTestData() {
        // Для хранения данных stub-классов
        fillOfferIdsList();
        fillBarCodeToEsklpMap();
    }

    public void putOfferToGuid(String offerId, Long uid, String guids) {
        offerToGuidsMap.put(offerId + "_" + uid, guids);
    }

    public long getNewOfferToGuidsId() {
        return offerToGuidsMap.size()+1L;
    }

    public String getGuidsByOfferId(String offerId, Long uid) {
        return offerToGuidsMap.get(offerId + "_" + uid);
    }

    public void putUidToEsiaToken(Long uid, String userEsiaToken, Integer regionCode) {
        TokenAndRegionData value = new TokenAndRegionData();
        value.userEsiaToken = userEsiaToken;
        value.regionCode = regionCode;
        uidToEsiaTokenMap.put(uid, value);
    }

    public long getNewuidToEsiaTokenId() {
        return uidToEsiaTokenMap.size()+1L;
    }

    public TokenAndRegionData getTokenByUid(Long uid) {
        return uidToEsiaTokenMap.get(uid);
    }

    public String getEsklpByBarcode(String barcode) {
        return barcodeToEsklpMap.get(barcode);
    }

    private void fillOfferIdsList() {
        offerIdsMap.put("'wNf7Z9Gsmf3_aFQwBQf_og'", List.of("wNf7Z9Gsmf3_aFQwBQf_og"));
    }

    private void fillBarCodeToEsklpMap() {
        barcodeToEsklpMap.put("40333222555777", "21.20.23.113-000001-1-00003-2000000725253");
        barcodeToEsklpMap.put("40333222555888", "21.20.10.146-000004-1-00019-2000000957287");
        barcodeToEsklpMap.put("40333222555999", "21.20.10.132-000014-1-00080-2000000509069");
    }

    public Map<String, List<String>> getOfferIdsMap() {
        return offerIdsMap;
    }

    public void clear() {
        uidToEsiaTokenMap.clear();
        offerToGuidsMap.clear();
    }
}
