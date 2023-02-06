package ru.yandex.market.partner.outlet;

import java.time.LocalDate;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.outlet.OutletLicenseType;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест для {@link ManageOutletInfoServantlet}
 *
 * @author stani on 03.08.18.
 */
@DbUnitDataSet(before = "ManageOutletInfoServantletFunctionalTest.before.csv")
class ManageOutletInfoServantletTest extends FunctionalTest {

    // License
    private static final LocalDate ORIGINAL_ISSUE_DATE = LocalDate.of(2001, 2, 21);
    private static final LocalDate ORIGINAL_EXPIRY_DATE = LocalDate.of(2019, 3, 31);
    private static final String ORIGINAL_LICENSE_NUMBER = "50РПА1234567";
    // LegalInfo
    private static final OrganizationType ORIGINAL_ORGANIZATION_TYPE = OrganizationType.OOO;
    private static final String ORIGINAL_ORGANIZATION_NAME = "SomeOrgName";
    private static final String ORIGINAL_REGISTRATION_NUMBER = "5077746887312";
    private static final String ORIGINAL_JURIDICAL_ADDRESS = "SomeJurAddr";
    private static final String ORIGINAL_FACT_ADDRESS = "SomeFactAddress";

    @Test
    void testShopManageOutletInfoNotFound() {
        ResponseEntity<String> response = updateOutletInfo(101L, 999L, "&drCost=400");
        assertEquals("wrong-outlet-id", getErrorCode(response));
    }

    @Test
    void testShopManageOutletInfoCreate() {
        final long outletId = createOutlet(101L, "&drCost=400");

        final ResponseEntity<String> outletInfo = getOutletInfo(101L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_shop_outlet_leniest.json"));
    }

    @Test
    @DbUnitDataSet(after = "ManageOutletInfoServantletFunctionalTest.after.csv")
    void testShopManageOutletInfoCreateAndUpdate() {
        updateOutletInfo(101L, 107L, "&drCost=400");
    }

    @Test
    @DisplayName("Проверка сохранения обновлений идентификатора точки продаж")
    void testUpdateShopOutletId() {
        long outletId = createOutlet(101L, "&drCost=400");
        updateOutletInfo(101L, outletId, "&shopOutletId=34567&drCost=400");
        ResponseEntity<String> outletInfo = getOutletInfo(101L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/update_shop_outlet_id.json"));
    }

    @Test
    @DbUnitDataSet(after = "testShopManageOutletInfoWithLicenseCreate.after.csv")
    void testShopManageOutletInfoWithLicenseCreate() {
        final long outletId = createOutlet(101L, "&lType=ALCOHOL&lNumber=50РПА1234567&lIssueDate=2001-02-21" +
                "&lExpiryDate=2019-03-31");

        final ResponseEntity<String> outletInfo = getOutletInfo(101L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/outlet_with_license_leniest.json"));
    }

    @ParameterizedTest
    @MethodSource("updateLicenseData")
    @DisplayName("Проверка сохранения обновлений, если обновлена только лицензия")
    void testShopManageOutletInfoWithLicenseUpdate(String licenseNumber, LocalDate issueDate, LocalDate expiryDate) {
        OutletLicenseType licenseType = OutletLicenseType.ALCOHOL;

        ResponseEntity<String> response = createOutletInfo(101L,
                formatLicenseParams(null, licenseType, ORIGINAL_LICENSE_NUMBER, ORIGINAL_ISSUE_DATE, ORIGINAL_EXPIRY_DATE) + "&drCost=400");
        JsonObject jsonObject = parseJson(response);
        long outletId = findResultId(jsonObject);
        long licenseId = findLicenseId(jsonObject);

        ResponseEntity<String> outletInfo = updateOutletInfo(101L, outletId,
                formatLicenseParams(licenseId, licenseType, licenseNumber, issueDate, expiryDate) + "&drCost=400");
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/outlet_check_license_data.json")
                .replace("\"{{id}}\"", Long.toString(licenseId))
                .replace("\"{{outletId}}\"", Long.toString(outletId))
                .replace("{{type}}", licenseType.name())
                .replace("{{number}}", licenseNumber)
                .replace("\"{{issueDate}}\"", Long.toString(issueDate.atStartOfDay(DateTimes.MOSCOW_TIME_ZONE).toInstant().getEpochSecond()))
                .replace("\"{{expiryDate}}\"", Long.toString(expiryDate.atStartOfDay(DateTimes.MOSCOW_TIME_ZONE).toInstant().getEpochSecond()))
        );
    }

    private static Stream<Arguments> updateLicenseData() {
        return Stream.of(
                Arguments.of("123", ORIGINAL_ISSUE_DATE, ORIGINAL_EXPIRY_DATE),
                Arguments.of(ORIGINAL_LICENSE_NUMBER, LocalDate.of(2002, 5, 1), ORIGINAL_EXPIRY_DATE),
                Arguments.of(ORIGINAL_LICENSE_NUMBER, ORIGINAL_ISSUE_DATE, LocalDate.of(2022, 3, 1)),
                Arguments.of("5555", LocalDate.of(2020, 2, 1), LocalDate.of(2022, 3, 1))
        );
    }

    private String formatLicenseParams(Long licenseId, OutletLicenseType licenseType, String licenseNumber,
                                       LocalDate issueDate, LocalDate expiryDate) {
        return "" +
                (licenseId != null ? ("&lId=" + licenseId) : "") +
                "&lType=" + licenseType +
                "&lNumber=" + licenseNumber +
                "&lIssueDate=" + issueDate +
                "&lExpiryDate=" + expiryDate;
    }

    @Test
    @DbUnitDataSet(
            before = "testShopManageOutletInfoWithLicenseUpdate.before.csv",
            after = "testShopManageOutletInfoWithLicenseUpdate.before.csv"
    )
    void testShopManageOutletInfoWithLicenseNoUpdate() {
        final ResponseEntity<String> response = updateOutletInfo(201L, 103L, "&drCost=400&lId=1&lType=ALCOHOL&lNumber=123321&lIssueDate=1982-06-25&lExpiryDate=2017-10-05");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DbUnitDataSet(
            before = "testShopManageOutletInfoWithLicenseDelete.before.csv",
            after = "testShopManageOutletInfoWithLicenseDelete.after.csv"
    )
    void testShopManageOutletInfoWithLicenseDelete() {
        final ResponseEntity<String> response = updateOutletInfo(201L, 103L, "&drCost=400");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testSupplierManageOutletInfoCreate() {
        final long outletId = createOutlet(201L, "&drCost=400");

        final ResponseEntity<String> outletInfo = getOutletInfo(201L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_supplier_outlet_leniest.json"));
    }

    @Test
    void testSupplierManageOutletInfoWithLegalInfoCreate() {
        final long outletId = createOutlet(201L, "&liCount=1&liOrgType=OOO&liOrgName=GazMas&liRegNumber=5077746887312" +
                "&liJurAddr=Default+city+1&liFactAddr=Default+city+2");

        ResponseEntity<String> outletInfo = getOutletInfo(201L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_supplier_outlet_with_legall_info_leniest.json"));
    }

    @Test
    @DbUnitDataSet(after = "testFmcgOutletWithLegalInfoCreate.after.csv")
    void testFmcgOutletWithLegalInfoCreate() {
        final ResponseEntity<String> response = createOutletInfo(301L,
                "&liCount=1&liOrgType=OOO&liRegNumber=5077746887312&liOrgName=FmcgOrgName&liJurAddr=JurAddr3&liFactAddr=FactAddr3&drCost=400");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("updateLegalInfoData")
    @DisplayName("Проверка сохранения обновлений, если обновлена только юридическая информация")
    void testShopManageOutletInfoWithLegalInfoUpdate(OrganizationType organizationType, String organizationName,
                                                     String registrationNumber, String juridicalAddress, String factAddress) {
        long outletId = createOutlet(101L, formatLegalInfoParams(ORIGINAL_ORGANIZATION_TYPE,
                ORIGINAL_ORGANIZATION_NAME, ORIGINAL_REGISTRATION_NUMBER,
                ORIGINAL_JURIDICAL_ADDRESS, ORIGINAL_FACT_ADDRESS));

        ResponseEntity<String> outletInfo = updateOutletInfo(101L, outletId,
                formatLegalInfoParams(organizationType, organizationName, registrationNumber, juridicalAddress, factAddress) + "&drCost=400");
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/outlet_check_legal_info_data.json")
                .replace("\"{{outletId}}\"", Long.toString(outletId))
                .replace("{{organizationType}}", organizationType.name())
                .replace("{{organizationName}}", organizationName)
                .replace("{{registrationNumber}}", registrationNumber)
                .replace("{{juridicalAddress}}", juridicalAddress)
                .replace("{{factAddress}}", factAddress)
        );

    }

    private static Stream<Arguments> updateLegalInfoData() {
        return Stream.of(
                Arguments.of(OrganizationType.OTHER, ORIGINAL_ORGANIZATION_NAME, ORIGINAL_REGISTRATION_NUMBER, ORIGINAL_JURIDICAL_ADDRESS, ORIGINAL_FACT_ADDRESS),
                Arguments.of(ORIGINAL_ORGANIZATION_TYPE, "newOrgName", ORIGINAL_REGISTRATION_NUMBER, ORIGINAL_JURIDICAL_ADDRESS, ORIGINAL_FACT_ADDRESS),
                Arguments.of(ORIGINAL_ORGANIZATION_TYPE, ORIGINAL_ORGANIZATION_NAME, "5001116887316", ORIGINAL_JURIDICAL_ADDRESS, ORIGINAL_FACT_ADDRESS),
                Arguments.of(ORIGINAL_ORGANIZATION_TYPE, ORIGINAL_ORGANIZATION_NAME, ORIGINAL_REGISTRATION_NUMBER, "newJurAddr", ORIGINAL_FACT_ADDRESS),
                Arguments.of(ORIGINAL_ORGANIZATION_TYPE, ORIGINAL_ORGANIZATION_NAME, ORIGINAL_REGISTRATION_NUMBER, ORIGINAL_JURIDICAL_ADDRESS, "newFactAddr"),
                Arguments.of(OrganizationType.OTHER, "newOrgName", "5001116887316", "newJurAddr", "newFactAddr")
        );
    }

    private String formatLegalInfoParams(OrganizationType organizationType, String organizationName,
                                         String registrationNumber, String juridicalAddress, String factAddress) {
        return "&liCount=1" +
                "&liOrgType=" + organizationType.name() +
                "&liOrgName=" + organizationName +
                "&liRegNumber=" + registrationNumber +
                "&liJurAddr=" + juridicalAddress +
                "&liFactAddr=" + factAddress;
    }

    @Test
    @DbUnitDataSet(after = "testFmcgOutletWithLegalInfoUpdate.after.csv")
    void testFmcgOutletWithLegalInfoUpdate() {
        final ResponseEntity<String> response = updateOutletInfo(301L, 105L, "&liCount=1&drCost=400");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testFmcgOutletWithLegalInfoGet() {
        final ResponseEntity<String> outletInfo = getOutletInfo(301L, 105L);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/get_fmcg_outlet_with_legal_info.json"));
    }

    @Test
    void testSupplierManageOutletInfoWithLegalInfoUpdate() {
        final ResponseEntity<String> response = updateOutletInfo(201L, 103L,
                "&drCost=400&liCount=1&liOrgType=OOO&liOrgName=Rosneft&liRegNumber=5077746887312&liJurAddr=Default+city+3&liFactAddr=Default+city+2");
        final JsonObject jsonObject = parseJson(response);
        final long outletId = findResultId(jsonObject);

        final ResponseEntity<String> outletInfo = getOutletInfo(201L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/update_supplier_outlet_with_legal_info_leniest.json")
                .replace("\"{{outletId}}\"", String.valueOf(outletId)));
    }

    @Test
    void testSupplierManageOutletInfoWithoutLegalInfoUpdate() {
        final long outletId = 104L;
        final ResponseEntity<String> response = updateOutletInfo(201L, outletId,
                "&liCount=1&liOrgType=OOO&liOrgName=Rosneft&liRegNumber=5077746887312&liJurAddr=Default+city+3&liFactAddr=Default+city+2&drCost=400");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ResponseEntity<String> outletInfo = getOutletInfo(201L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/update_supplier_outlet_with_legal_info_leniest.json")
                .replace("\"{{outletId}}\"", String.valueOf(outletId)));
    }

    @Test
    void testSupplierManageOutletInfoWithoutNullLegalInfoUpdate() {
        final long outletId = 103L;
        final ResponseEntity<String> response = updateOutletInfo(201L, outletId, "&liCount=0&drCost=400");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ResponseEntity<String> outletInfo = getOutletInfo(201L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/update_supplier_outlet_without_legal_info.json")
                .replace("\"{{outletId}}\"", String.valueOf(outletId)));
    }

    @Test
    void testSupplierManageOutletInfoWithLegalInfoUpdateInvalidRegNumber() {
        final ResponseEntity<String> response = updateOutletInfo(201L, 103L,
                "&liCount=1&liOrgType=OOO&liOrgName=Rosneft&liRegNumber=123456&liJurAddr=Default+city+3&liFactAddr=Default+city+2&drCost=400");
        assertEquals("invalid-legal-info-registration-number", getErrorCode(response));
    }

    @Test
    void testCreateDbsOutletWithNullPrice() {
        final long outletId = createOutlet(101L, "");

        final ResponseEntity<String> outletInfo = getOutletInfo(101L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_dbs_outlet_null_price.json"));
    }

    @Test
    void testDbsCreatePickupWithStoragePeriod() {
        final long outletId = createOutlet(101L, "&storagePeriod=200");

        final ResponseEntity<String> outletInfo = getOutletInfo(101L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_dbs_outlet_with_storage_period.json"));
    }

    @Test
    void testDbsCreatePickupWithDefaultStoragePeriod() {
        final long outletId = createOutlet(101L, "");

        final ResponseEntity<String> outletInfo = getOutletInfo(101L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_dbs_default_storage_period.json"));
    }

    @Test
    void testSupplierCreatePickupStoragePeriodDefaultValue() {
        final long outletId = createOutlet(201L, "");

        final ResponseEntity<String> outletInfo = getOutletInfo(201L, outletId);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/create_supplier_default_storage_peroid.json"));
    }

    @Test
    void testDbsGetPickupWithStoragePeriodNull() {
        final ResponseEntity<String> outletInfo = getOutletInfo(101L, 108);
        checkOutletResponse(outletInfo, readResource("/mvc/outlet/get_dbs_outlet_with_null_period.json"));
    }

    @Test
    @Description("Проверяем, что устанавливается новый срок хранения")
    @DbUnitDataSet(after = "testDsbUpdatePickupWithStoragePeriod.after.csv")
    void testDsbUpdatePickupWithStoragePeriod() {
        final ResponseEntity<String> response = updateOutletInfo(101L, 107, "&storagePeriod=100");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Description("Проверяем, что если в запросе нет срока хранения," +
            "в бд останется тот же срок хранения")
    @DbUnitDataSet(after = "testDsbUpdatePickupWithExistingStoragePeriod.after.csv")
    void testDsbUpdatePickupWithExistingStoragePeriod() {
        final ResponseEntity<String> response = updateOutletInfo(101L, 109, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private String getErrorCode(ResponseEntity<String> response) {
        final JsonObject jsonObject = parseJson(response);
        return jsonObject.get("errors").getAsJsonArray().get(0).getAsJsonObject().get("messageCode").getAsString();
    }

    private ResponseEntity<String> createOutletInfo(long campaignId, String additionalParams) {
        final String url = baseUrl + baseCreateRequest() + "&a=c" + additionalParams;
        return FunctionalTestHelper.get(url, campaignId);
    }

    private ResponseEntity<String> updateOutletInfo(long campaignId, long outletId, String additionalParams) {
        final String url = baseUrl + baseCreateRequest() + "&a=u" + "&outletId={outletId}" + additionalParams;
        return FunctionalTestHelper.get(url, campaignId, outletId);
    }

    private ResponseEntity<String> getOutletInfo(long campaignId, long outletId) {
        final String url = baseUrl + "/manageOutletInfo?id={campaignId}&outletId={outletId}&format=json";
        return FunctionalTestHelper.get(url, campaignId, outletId);
    }

    private String baseCreateRequest() {
        return "/manageOutletInfo?id={campaignId}&regionName=&name=Hermes&type=RETAIL" +
                "&country=Россия&regionId=10754&addrCity=Серпухов&addrStreet=Московское+ш.&addrNumber=51" +
                "&addrBuilding=&addrBlock=&addrKm=&addrEstate=&addrAdd=&drShipperId=120" +
                "&coords=37.404672,54.938731&drWorkInHolidays=1&emails=&drDateSwitchHour=24" +
                "&drMinDeliveryDays=4&drMaxDeliveryDays=4&schCount=2&schStartDay0=MONDAY&schEndDay0=FRIDAY" +
                "&schStartTime0=10:00&schEndTime0=19:00&schStartDay1=SATURDAY&schEndDay1=SUNDAY" +
                "&schStartTime1=11:30&schEndTime1=17:00&phCount=1&phCountry0=8&phCity0=800&phNumber0=775-6275" +
                "&phExt0=&phComments0=&phType0=PHONE&_user_id=527861308&format=json";
    }

    private JsonObject parseJson(final ResponseEntity<String> response) {
        final String body = Preconditions.checkNotNull(response.getBody());
        final JsonElement jsonElement = JsonTestUtil.parseJson(body);
        return jsonElement.getAsJsonObject();
    }

    private long findResultId(final JsonObject jsonObject) {
        return jsonObject.get("result")
                .getAsJsonArray().get(0)
                .getAsJsonObject().get("id").getAsLong();
    }

    private long findLicenseId(final JsonObject jsonObject) {
        return jsonObject.get("result")
                .getAsJsonArray().get(0)
                .getAsJsonObject().get("licenses")
                .getAsJsonArray().get(0)
                .getAsJsonObject().get("id").getAsLong();
    }

    private void checkOutletResponse(final ResponseEntity<String> outletInfo, final String expectedJson) {
        final String result = new JSONObject(outletInfo.getBody()).getJSONArray("result").toString();
        JSONAssert.assertEquals(expectedJson, result, JSONCompareMode.LENIENT);
    }

    private String readResource(final String resourceFileName) {
        return StringTestUtil.getString(ManageOutletInfoServantlet.class, resourceFileName);
    }

    private long createOutlet(long campaignId, String additionalParams) {
        ResponseEntity<String> response = createOutletInfo(campaignId, additionalParams);
        JsonObject jsonObject = parseJson(response);
        return findResultId(jsonObject);
    }
}
