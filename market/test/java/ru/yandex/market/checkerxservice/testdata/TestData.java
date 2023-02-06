package ru.yandex.market.checkerxservice.testdata;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataGuidByEsklpRequest;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataGuidByEsklpResponse;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataGuidByMnnRequest;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataGuidByMnnResponse;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataStatusRequest;
import ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataStatusResponse;
import ru.yandex.market.checkerxservice.chekservice.controllers.checkerx.DrugItemEsklp;
import ru.yandex.market.checkerxservice.chekservice.controllers.checkerx.DrugItemMnn;
import ru.yandex.market.checkerxservice.chekservice.enums.PrescriptionStatus;
import ru.yandex.market.checkerxservice.chekservice.model.ReportStubDto;
import ru.yandex.market.checkerxservice.utils.JsonUtils;

import static ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataStatusResponse.OrderPrescription;
import static ru.yandex.market.checkerxservice.chekservice.client.medicata.model.MedicataStatusResponse.Prescription;

public class TestData {
    private static TestData testData;
    // <itemId, item>
    private Map<Long, DrugItemMnn> requestDrugMnnByIdMap = new HashMap<>();
    // <itemId, item>
    private Map<Long, DrugItemMnn> responseDrugMnnByIdMap = new HashMap<>();
    // <itemId, item>
    private Map<Long, DrugItemEsklp> requestDrugEsklpByIdMap = new HashMap<>();
    // <itemId, item>
    private Map<Long, DrugItemEsklp> responseDrugEsklpByIdMap = new HashMap<>();
    // <"request_name", request>
    private Map<String, MedicataGuidByMnnRequest> medicataGuidMnnRequestMap = new HashMap<>();
    // <request.toString(), response>
    private Map<String, MedicataGuidByMnnResponse> medicataGuidMnnResponseMap = new HashMap<>();
    // <"request_name", request>
    private Map<String, MedicataGuidByEsklpRequest> medicataGuidEsklpRequestMap = new HashMap<>();
    // <request.toString(), response>
    private Map<String, MedicataGuidByEsklpResponse> medicataGuidEsklpResponseMap = new HashMap<>();
    // <"request_name", request>
    private Map<String, MedicataStatusRequest> medicataStatusRequestMap = new HashMap<>();
    // <request.toString(), response>
    private Map<String, MedicataStatusResponse> medicataStatusResponseMap = new HashMap<>();
    // <offerId, reportData>
    private Map<String, ReportData> reportDataByOfferIdMap = new HashMap<>();
    // <modelId, reportData>
    private Map<Long, ReportData> reportDataByModelIdMap = new HashMap<>();
    // <guid, prescriptionId>
    private Map<String, String> guidToPrescriptionIdMap = new HashMap<>();

    private TestData() {
        loadTestData();
    }

    public static TestData getInstance() {
        if (testData == null) {
            testData = new TestData();
        }
        return testData;
    }

    private void loadTestData() {
        fillDrugItemMnnMap(getDrugData());
        fillGuidToPrescriptionIdMap(getGuidToPrescriptionIdMapData());
        fillMedicataGuidRequestMap(getMedicataGuidRequestData());
        fillMedicataGuidResponseMap(getMedicataGuidResponseData());
        fillMedicataStatusMap(getMedicataStatusData());
        fillReportDataMap(getReportData());
    }

    public MedicataStatusResponse getMedicataStatusResponse(String request) {
        return medicataStatusResponseMap.get(request);
    }

    public DrugItemMnn getRequestMnnItem(Long itemId) {
        return requestDrugMnnByIdMap.get(itemId);
    }

    public MedicataGuidByMnnRequest getMedicataGuidMnnRequest(String requestName) {
        return medicataGuidMnnRequestMap.get(requestName);
    }

    public Set<String> getMedicataMnnRequestNames() {
        return medicataGuidMnnRequestMap.keySet();
    }

    public MedicataGuidByMnnResponse getMedicataGuidMnnRespose(String request) {
        return medicataGuidMnnResponseMap.get(request);
    }

    public MedicataGuidByEsklpResponse getMedicataGuidEsklpRespose(String request) {
        return medicataGuidEsklpResponseMap.get(request);
    }

    public ReportData getReportDataByOfferId(String offerId) {
        return reportDataByOfferIdMap.get(offerId);
    }

    public ReportData getReportDataByModelId(Long modelId) {
        return reportDataByModelIdMap.get(modelId);
    }

    private void fillDrugItemMnnMap(Object[][] data) {
        for (Object[] el : data) {
            DrugItemMnn mnnRequestItem = new DrugItemMnn((Long) el[0], (String) el[1], (String) el[2],
                    (Double) el[3], (String) el[4], null, null);
            requestDrugMnnByIdMap.put(mnnRequestItem.getItemId(), mnnRequestItem);
            DrugItemMnn mnnResponseItem = new DrugItemMnn((Long) el[0], (String) el[1], (String) el[2],
                    (Double) el[3], (String) el[4], (String) el[6], buildMnnGuidList((String) el[7]));
            responseDrugMnnByIdMap.put(mnnResponseItem.getItemId(), mnnResponseItem);

            DrugItemEsklp esklpRequestItem = new DrugItemEsklp((Long) el[0], (String) el[1], (String) el[2],
                    (Double) el[3], (String) el[5], null, null);
            requestDrugEsklpByIdMap.put(esklpRequestItem.getItemId(), esklpRequestItem);
            DrugItemEsklp responseItem = new DrugItemEsklp((Long) el[0], (String) el[1], (String) el[2],
                    (Double) el[3], (String) el[5], (String) el[6], buildEsklpGuidList((String) el[7]));
            responseDrugEsklpByIdMap.put(responseItem.getItemId(), responseItem);
        }
    }

    private void fillMedicataGuidRequestMap(Object[][] data) {
        for (Object[] el : data) {
            MedicataGuidByMnnRequest mnnRequest = new MedicataGuidByMnnRequest(
                    getItemsByIds(castToList(el[1]), requestDrugMnnByIdMap), (String) el[2], (Integer) el[3]);
            medicataGuidMnnRequestMap.put((String) el[0], mnnRequest);

            MedicataGuidByEsklpRequest esklpRequest = new MedicataGuidByEsklpRequest(
                    getItemsByIds(castToList(el[1]), requestDrugEsklpByIdMap), (String) el[2], (Integer) el[3]);
            medicataGuidEsklpRequestMap.put((String) el[0], esklpRequest);
        }
    }

    private void fillMedicataGuidResponseMap(Object[][] data) {
        for (Object[] el : data) {
            MedicataGuidByMnnResponse mnnResponse = new MedicataGuidByMnnResponse(
                    getItemsByIds(castToList(el[1]), responseDrugMnnByIdMap));
            MedicataGuidByEsklpResponse esklpResponse = new MedicataGuidByEsklpResponse(
                    getItemsByIds(castToList(el[1]), responseDrugEsklpByIdMap));
            try {
                medicataGuidMnnResponseMap.put(
                        JsonUtils.toJson(medicataGuidMnnRequestMap.get(((String) el[0]).trim())), mnnResponse);
                medicataGuidEsklpResponseMap.put(
                        JsonUtils.toJson(medicataGuidEsklpRequestMap.get(((String) el[0]).trim())), esklpResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fillMedicataStatusMap(Object[][] data) {
        for (Object[] el : data) {
            //data{ List<String> ids, Integer regionId, String tempUserId }
            MedicataStatusRequest statusRequest = new MedicataStatusRequest(castToList(el[1]),
                    Stream.of(6).map(PrescriptionStatus::getByIdOrUnknown)
                            .collect(Collectors.toList()),
                    (Integer) el[2], (String) el[3]);
            medicataStatusRequestMap.put((String) el[0], statusRequest);

            MedicataStatusResponse statusResponse = new MedicataStatusResponse(
                    ((List<String>) castToList(el[1])).stream()
                            .map(guid -> new OrderPrescription(guid,
                                    List.of(new Prescription(
                                            guidToPrescriptionIdMap.get(guid), PrescriptionStatus.getByIdOrUnknown(6)
                                    ))
                            )).collect(Collectors.toList()) );
            try {
                medicataStatusResponseMap.put(
                        JsonUtils.toJson(medicataStatusRequestMap.get(((String) el[0]).trim())), statusResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ReportData extends ReportStubDto {
        private Long modelId;

        public ReportData(String offerId, Long modelId, String barCode) {
            super(offerId, barCode);
            this.modelId = modelId;
        }

        public Long getModelId() {
            return modelId;
        }
    }

    private void fillReportDataMap(Object[][] data) {
        for (Object[] el : data) {
            ReportData reportData = new ReportData((String) el[0], (Long) el[1], (String) el[2]);
            reportDataByOfferIdMap.put((String) el[0], reportData);

            if (reportData.modelId != null) {
                reportDataByModelIdMap.put(reportData.modelId, reportData);
            }
        }
    }

    private void fillGuidToPrescriptionIdMap(Object[][] data) {
        for (Object[] el : data) {
            guidToPrescriptionIdMap.put((String) el[0], (String) el[1]);
        }
    }

    private List<DrugItemEsklp.Guid> buildEsklpGuidList(String guidStr) {
        return guidStr == null || guidStr.isEmpty()
                ? Collections.emptyList()
                : Arrays.stream(guidStr.split(",")).map(DrugItemEsklp.Guid::new).collect(Collectors.toList());
    }

    private List<DrugItemMnn.Guid> buildMnnGuidList(String guidStr) {
        return guidStr == null || guidStr.isEmpty()
                ? Collections.emptyList()
                : Arrays.stream(guidStr.split(",")).map(DrugItemMnn.Guid::new).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <T extends List<?>> T castToList(Object obj) {
        return (T) obj;
    }

    private <T> List<T> getItemsByIds(List<Long> ids, Map<Long, T> map) {
        return ids.stream().map(map::get).collect(Collectors.toList());
    }

    //TODO: брать все данные из csv
    private Object[][] getDrugData() {
        Object[][] data = {
                {1L, "Интерферон альфа-2b р-р д/ин. 1000000МЕ",
                        "Интерферон альфа-2b р-р д/ин. 1000000МЕ", 12d, "GD000344",
                        "21.20.10.146-000004-1-00019-2000000957287", "Ok",
                        "6cd471ae-7903-42e3-9d96-50994ab6619c,eb113642-f9dc-4e9c-a743-6bf12a704195"},
                {2L, "Золототысячника трава сырье растит. пор.",
                        "Золототысячника трава сырье растит. пор.", 2d, "GD007223",
                        "21.20.23.113-000001-1-00003-2000000725253", "NoRX", ""},
                {3L, "Ассубекс табл. покр. плен. оболочкой 10 мг",
                        "Ривароксабан табл. покр. плен. оболочкой 10 мг", 1d, "GD013896",
                        "21.20.10.132-000014-1-00080-2000000509069", "Ok",
                        "9a2b2882-a125-48e2-baea-b30d9279d21e,0672a357-76f6-490a-b921-e915fab6d1d2," +
                                "3ce7da30-4704-4de7-ad34-d72de1f0a219"}};
        return data;
    }

    private Object[][] getMedicataGuidRequestData() {
        Object[][] data = {
                {"req_guid_1", List.of(1L,2L), "b3ec53e1-c7d1-4829-ba2d-ce01f00e4a26", null},
                {"req_guid_2", List.of(3L), "b3ec53e1-c7d1-4829-ba2d-ce01f00e4a26", 213}
        };
        return data;
    }

    private Object[][] getMedicataGuidResponseData() {
        Object[][] data = {{"req_guid_1", List.of(1L,2L)}, {"req_guid_2", List.of(3L)}};
        return data;
    }

    private Object[][] getMedicataStatusData() {
        //data{ String reqName, List<String> ids, Integer regionId, String tempUserId }
        Object[][] data = {
                {"req_status_1",
                        List.of("6cd471ae-7903-42e3-9d96-50994ab6619c",
                                "eb113642-f9dc-4e9c-a743-6bf12a704195"),
                        213, "b3ec53e1-c7d1-4829-ba2d-ce01f00e4a26"}
        };
        return data;
    }

    private Object[][] getReportData() {
        Object[][] data = {
                {"wNf7Z9Gsmf3_aFQwAQf_og", 265151516L, "40333222555777"},
                {"wNf7Z9Gsmf3_aFQwBQf_og", 265151517L, "40333222555888"},
                {"wNf7Z9Gsmf3_aFQwCQf_og", null, "40333222555999"}
        };
        return data;
    }

    private Object[][] getGuidToPrescriptionIdMapData() {
        Object[][] data = {
                {"6cd471ae-7903-42e3-9d96-50994ab6619c", "d5be4c38-b079-4d34-b387-6eafd33b535f"},
                {"eb113642-f9dc-4e9c-a743-6bf12a704195", "d6be4c38-b079-4d34-b387-6eafd33b535g"}
        };
        return data;
    }

    private Object[][] getMedicataStubData() {
        Object[][] data = {
                {"21.20.10.146-000004-1-00019-2000000957287", "GD000344",
                        List.of("6cd471ae-7903-42e3-9d96-50994ab6619c", "eb113642-f9dc-4e9c-a743-6bf12a704195")},
                {"21.20.23.113-000001-1-00003-2000000725253", "GD007223", Collections.emptyList()},
                {"21.20.10.132-000014-1-00080-2000000509069", "GD013896",
                        List.of("9a2b2882-a125-48e2-baea-b30d9279d21e","0672a357-76f6-490a-b921-e915fab6d1d2",
                                "3ce7da30-4704-4de7-ad34-d72de1f0a219")}
        };
        return data;
    }
}
