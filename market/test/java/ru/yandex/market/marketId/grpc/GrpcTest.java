package ru.yandex.market.marketId.grpc;

import java.io.IOException;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.id.GetByIdRequest;
import ru.yandex.market.id.GetByRegistrationNumberRequest;
import ru.yandex.market.id.GetByRegistrationNumberResponse;
import ru.yandex.market.id.GetOrCreateMarketIdRequest;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.LinkMarketIdRequest;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.partner.error.info.model.ErrorCode;
import ru.yandex.market.partner.error.info.model.ErrorInfo;
import ru.yandex.market.partner.error.info.util.ErrorInfoUtil;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class GrpcTest extends GrpcFunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String CONTACT_TABLE = "market_id.contact";
    private static final String MARKET_ACCOUNT_TABLE = "market_id.market_account";
    private static final String PARTNER_TABLE = "market_id.partner";
    private static final String CLIENT_ACL_TABLE = "market_id.client_acl";
    private static final String LEGAL_INFO_TABLE = "market_id.legal_info";


    static LegalInfo legalInfo = LegalInfo.newBuilder()
            .setLegalName("LegalNameTest")
            .setType("SHOP")
            .setRegistrationNumber("regNumber")
            .setLegalAddress("legal address")
            .setPhysicalAddress("Physical address")
            .build();

    @Test
    @DisplayName("Получение по ИД")
    @DbUnitDataSet(before = "../Test.before.csv")
    void testGetById() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetByIdRequest request = GetByIdRequest.newBuilder().setMarketId(1).build();
        MarketAccount marketAccount = stub.getById(request);
        LegalInfo legalInfo = marketAccount.getLegalInfo();
        assertNotNull(legalInfo);
        assertEquals("MarketIDOne", legalInfo.getLegalName());
        assertEquals("Type1", legalInfo.getType());

    }

    @Test
    @DisplayName("Получение по ИД, маркетИД не найден")
    void testGetByIdNotFound() throws IOException {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetByIdRequest request = GetByIdRequest.newBuilder().setMarketId(10).build();
        StatusRuntimeException sre = assertThrows(StatusRuntimeException.class, () -> stub.getById(request));
        assertEquals(Status.Code.INVALID_ARGUMENT, sre.getStatus().getCode());
        String errorJson = sre.getTrailers().get(Metadata.Key.of("errorInfo", Metadata.ASCII_STRING_MARSHALLER));
        ErrorInfo errorInfo = ErrorInfoUtil.fromJson(errorJson);
        assertEquals(ErrorCode.BAD_PARAM, errorInfo.getCode());

    }

    @Test
    @DisplayName("Ничего не было ранее. Создаем два одинаковых запроса. Ответ одинаковый")
    void testCreateOrUpdateMarketId() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(2000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(legalInfo)
                .build();
        int contactRows = getRowsCount(CONTACT_TABLE);
        int marketAccountRows = getRowsCount(MARKET_ACCOUNT_TABLE);
        int partnerRows = getRowsCount(PARTNER_TABLE);
        int clientAclRows = getRowsCount(CLIENT_ACL_TABLE);
        int legalInfoRows = getRowsCount(LEGAL_INFO_TABLE);
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        MarketAccount marketAccount2 = stub.getOrCreateMarketId(request);
        assertEquals(marketAccount.getMarketId(), marketAccount2.getMarketId());
        assertEquals(contactRows + 1, getRowsCount(CONTACT_TABLE));
        assertEquals(marketAccountRows + 1, getRowsCount(MARKET_ACCOUNT_TABLE));
        assertEquals(partnerRows + 1, getRowsCount(PARTNER_TABLE));
        assertEquals(clientAclRows + 1, getRowsCount(CLIENT_ACL_TABLE));
        assertEquals(legalInfoRows + 5, getRowsCount(LEGAL_INFO_TABLE));
    }

    @Test
    @DisplayName("Ничего не было ранее. Создаем два разных маркет ИД")
    void testCreateOrUpdateMarketIdTwiceDifferent() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(2000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(LegalInfo.newBuilder()
                        .setLegalName("LegalNameTest")
                        .setRegistrationNumber("regNumber")
                        .build())
                .build();
        GetOrCreateMarketIdRequest request2 = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(4000)
                .setPartnerType("SHOP")
                .setUid(400)
                .setLegalInfo(LegalInfo.newBuilder()
                        .setLegalName("LegalNameTest")
                        .setRegistrationNumber("regNumber2")
                        .build())
                .build();
        int contactRows = getRowsCount(CONTACT_TABLE);
        int marketAccountRows = getRowsCount(MARKET_ACCOUNT_TABLE);
        int partnerRows = getRowsCount(PARTNER_TABLE);
        int clientAclRows = getRowsCount(CLIENT_ACL_TABLE);
        int legalInfoRows = getRowsCount(LEGAL_INFO_TABLE);
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        MarketAccount marketAccount2 = stub.getOrCreateMarketId(request2);
        assertNotEquals(marketAccount.getMarketId(), marketAccount2.getMarketId());
        assertEquals(contactRows + 2, getRowsCount(CONTACT_TABLE));
        assertEquals(marketAccountRows + 2, getRowsCount(MARKET_ACCOUNT_TABLE));
        assertEquals(partnerRows + 2, getRowsCount(PARTNER_TABLE));
        assertEquals(clientAclRows + 2, getRowsCount(CLIENT_ACL_TABLE));
        assertEquals(legalInfoRows + 4, getRowsCount(LEGAL_INFO_TABLE));
    }

    @Test
    @DisplayName("Партнер и маркет ИД уже есть. Надо только создать и связать клиента")
    @DbUnitDataSet(before = "../Test.before.csv")
    void testCreateOrUpdateMarketIdWhenPartnerExists() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(1000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(legalInfo)
                .build();
        int contactRows = getRowsCount(CONTACT_TABLE);
        int marketAccountRows = getRowsCount(MARKET_ACCOUNT_TABLE);
        int partnerRows = getRowsCount(PARTNER_TABLE);
        int clientAclRows = getRowsCount(CLIENT_ACL_TABLE);
        int legalInfoRows = getRowsCount(LEGAL_INFO_TABLE);
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        assertEquals(contactRows + 1, getRowsCount(CONTACT_TABLE));
        assertEquals(marketAccountRows, getRowsCount(MARKET_ACCOUNT_TABLE));
        assertEquals(partnerRows, getRowsCount(PARTNER_TABLE));
        assertEquals(clientAclRows + 1, getRowsCount(CLIENT_ACL_TABLE));
        assertEquals(legalInfoRows, getRowsCount(LEGAL_INFO_TABLE));
    }

    @Test
    @DisplayName("Партнер, клиент и маркет ИД уже есть.  Надо только создать и связать контакт")
    @DbUnitDataSet(before = "../Test.before.csv")
    void testCreateOrUpdateMarketIdWhenPartnerAndClientExists() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(1000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(legalInfo)
                .build();
        int contactRows = getRowsCount(CONTACT_TABLE);
        int marketAccountRows = getRowsCount(MARKET_ACCOUNT_TABLE);
        int partnerRows = getRowsCount(PARTNER_TABLE);
        int clientAclRows = getRowsCount(CLIENT_ACL_TABLE);
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        assertEquals(contactRows + 1, getRowsCount(CONTACT_TABLE));
        assertEquals(marketAccountRows, getRowsCount(MARKET_ACCOUNT_TABLE));
        assertEquals(partnerRows, getRowsCount(PARTNER_TABLE));
        assertEquals(clientAclRows + 1, getRowsCount(CLIENT_ACL_TABLE));
    }

    @Test
    @DisplayName("Партнер, уже есть. передан огрн, который уже есть в базе")
    @DbUnitDataSet(before = "../Test.before.csv", after = "LinkOnCreate.after.csv")
    void testCreateOrUpdateMarketIdWithDifferentOgrn() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(1000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(LegalInfo.newBuilder().setRegistrationNumber("7654321").build())
                .build();
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        assertEquals(5, marketAccount.getMarketId());
    }

    @Test
    @DisplayName("Партнер, уже есть. передан огрн, которого нет в базе")
    @DbUnitDataSet(before = "../Test.before.csv", after = "GetOrCreateMarketIdWithNewRegNumber.after.csv")
    void testCreateOrUpdateMarketIdWithNewOgrn() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(1000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(LegalInfo.newBuilder().setRegistrationNumber("12345").build())
                .build();
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        assertEquals(marketAccount.getMarketId(), 2000000);
    }

    @Test
    @DisplayName("Партнер, клиент, контакт и маркет ИД уже есть.  Надо связать")
    @DbUnitDataSet(before = "../Test.before.csv")
    void testCreateOrUpdateMarketIdWhenPartnerAndClientAndContactExists() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(1000)
                .setPartnerType("SHOP")
                .setUid(101)
                .setLegalInfo(legalInfo)
                .build();
        int contactRows = getRowsCount(CONTACT_TABLE);
        int marketAccountRows = getRowsCount(MARKET_ACCOUNT_TABLE);
        int partnerRows = getRowsCount(PARTNER_TABLE);
        int clientAclRows = getRowsCount(CLIENT_ACL_TABLE);
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        assertEquals(contactRows, getRowsCount(CONTACT_TABLE));
        assertEquals(marketAccountRows, getRowsCount(MARKET_ACCOUNT_TABLE));
        assertEquals(partnerRows, getRowsCount(PARTNER_TABLE));
        assertEquals(clientAclRows + 1, getRowsCount(CLIENT_ACL_TABLE));
    }


    @Test
    @DbUnitDataSet(before = "../Test.before.csv", after = "../Test.before.csv")
    @DisplayName("Проверить что ничего не создаем, если данные уже есть")
    void testCreateOrUpdateMarketIdWhenAlreadyExists() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(1000)
                .setPartnerType("SHOP")
                .setUid(100)
                .setLegalInfo(legalInfo)
                .build();
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
    }

    @Test
    @DisplayName("Связать партнера с уже существующим маркет ИД")
    @DbUnitDataSet(before = "../Test.before.csv", after = "LinkPartnerOk.after.csv")
    void linkPartnerWithExistingMarketId() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkMarketIdRequest request = LinkMarketIdRequest.newBuilder()
                .setMarketId(1)
                .setPartnerId(3333)
                .setPartnerType("SHOP")
                .setUid(100)
                .build();
        MarketAccount marketAccount = stub.linkMarketIdRequest(request);
    }

    @Test
    @DisplayName("Связать партнера с маркет ИД, если уже связан")
    @DbUnitDataSet(before = "../Test.before.csv", after = "LinkPartnerOk.after.csv")
    void linkPartnerWithExistingMarketIdAlreadyLinked() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkMarketIdRequest request = LinkMarketIdRequest.newBuilder()
                .setMarketId(1)
                .setPartnerId(3333)
                .setPartnerType("SHOP")
                .setUid(100)
                .build();
        MarketAccount marketAccount = stub.linkMarketIdRequest(request);
        MarketAccount marketAccount2 = stub.linkMarketIdRequest(request);
    }

    @Test
    @DisplayName("Связать партнера с маркет ИД, у партнера другой маркетИД")
    @DbUnitDataSet(before = "../Test.before.csv", after = "LinkToAnother.after.csv")
    void linkPartnerWithWrongMarketId() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkMarketIdRequest request = LinkMarketIdRequest.newBuilder()
                .setMarketId(1)
                .setPartnerId(1001)
                .setPartnerType("SHOP")
                .setUid(100)
                .build();
        stub.linkMarketIdRequest(request);
    }

    @Test
    @DisplayName("Связать партнера с маркет ИД, у пользователя нет доступа к маркет ИД")
    @DbUnitDataSet(before = "../Test.before.csv", after = "LinkToAnother.after.csv")
    void linkPartnerWithNoAccess() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkMarketIdRequest request = LinkMarketIdRequest.newBuilder()
                .setMarketId(1)
                .setPartnerId(1001)
                .setPartnerType("SHOP")
                .setUid(102)
                .build();
        stub.linkMarketIdRequest(request);
    }

    @Test
    @DisplayName("Создание маркетИД с уже существующим огрн")
    @DbUnitDataSet(before = "../Test.before.csv")
    void createMarketIdWithExistingRegNumber() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(2000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(LegalInfo.newBuilder()
                        .setRegistrationNumber("7654321")
                        .build())
                .build();
        MarketAccount marketAccount = stub.getOrCreateMarketId(request);
        assertEquals(5, marketAccount.getMarketId());
    }

    @Test
    @DisplayName("Создание маркетИД без указания огрн")
    @DbUnitDataSet(before = "../Test.before.csv")
    void createMarketIdWithoutRegNumber() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetOrCreateMarketIdRequest request = GetOrCreateMarketIdRequest.newBuilder()
                .setPartnerId(2000)
                .setPartnerType("SHOP")
                .setUid(300)
                .setLegalInfo(LegalInfo.newBuilder().build())
                .build();
        StatusRuntimeException sre = assertThrows(StatusRuntimeException.class,
                () -> stub.getOrCreateMarketId(request));
        assertEquals(Status.Code.INTERNAL, sre.getStatus().getCode());
    }

    @Test
    @DisplayName("Получение маркет ИД по огрн. Успешно")
    @DbUnitDataSet(before = "../Test.before.csv")
    void getMarketIdByRegNumberSuccess() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetByRegistrationNumberRequest request = GetByRegistrationNumberRequest.newBuilder()
                .setRegistrationNumber("7654321")
                .build();
        GetByRegistrationNumberResponse response = stub.getByRegistrationNumber(request);
        assertTrue(response.getSuccess());
        assertEquals(5, response.getMarketId());
    }

    @Test
    @DisplayName("Получение маркет ИД по огрн. Не найден")
    @DbUnitDataSet(before = "../Test.before.csv")
    void getMarketIdByRegNumberNotFound() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        GetByRegistrationNumberRequest request = GetByRegistrationNumberRequest.newBuilder()
                .setRegistrationNumber("7654")
                .build();
        GetByRegistrationNumberResponse response = stub.getByRegistrationNumber(request);
        assertFalse(response.getSuccess());
    }

    int getRowsCount(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }
}
