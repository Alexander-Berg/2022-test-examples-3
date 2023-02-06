package ru.yandex.market.marketId.grpc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "../Test.before.csv")
@DisplayName("Тест получения МаркетИД по партнеру")
class GetByPartnerFunctionalTest extends GrpcFunctionalTest {

    private MarketIdServiceGrpc.MarketIdServiceBlockingStub stub;

    @BeforeEach
    void init() {
        stub = MarketIdServiceGrpc.newBlockingStub(channel);
    }


    @Test
    @DisplayName("Партнер не указан")
    void testNoPartner() {
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder().build();
        GetByPartnerResponse response = stub.getByPartner(request);
        assertFalse(response.getSuccess());
        assertEquals("Partner is required", response.getMessage());
    }

    @Test
    @DisplayName("ИД партнера не указан")
    void testNoPartneId() {
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder()
                .setPartner(
                        MarketIdPartner
                                .newBuilder()
                                .setPartnerType("SHOP")
                                .build()
                ).build();
        GetByPartnerResponse response = stub.getByPartner(request);
        assertFalse(response.getSuccess());
        assertEquals("Field partnerId is not set", response.getMessage());
    }

    @Test
    @DisplayName("Тип партнера не указан")
    void testNoPartnerType() {
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder()
                .setPartner(
                        MarketIdPartner
                                .newBuilder()
                                .setPartnerId(1L)
                                .build()
                ).build();
        GetByPartnerResponse response = stub.getByPartner(request);
        assertFalse(response.getSuccess());
        assertEquals("Field partnerType is not set", response.getMessage());
    }

    @Test
    @DisplayName("Партнер не найден")
    void testPartnerNotFound() {
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder()
                .setPartner(
                        MarketIdPartner
                                .newBuilder()
                                .setPartnerId(1L)
                                .setPartnerType("SHOP")
                                .build()
                ).build();
        GetByPartnerResponse response = stub.getByPartner(request);
        assertFalse(response.getSuccess());
        assertEquals("Partner not found", response.getMessage());
    }

    @Test
    @DisplayName("Партнер найден, вернули маркетИД")
    void testSuccess() {
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder()
                .setPartner(
                        MarketIdPartner
                                .newBuilder()
                                .setPartnerId(1000L)
                                .setPartnerType("SHOP")
                                .build()
                ).build();
        GetByPartnerResponse response = stub.getByPartner(request);
        assertTrue(response.getSuccess());
        assertEquals("", response.getMessage());
        assertEquals(1L, response.getMarketId().getMarketId());
        assertEquals("regNumber",response.getMarketId().getLegalInfo().getRegistrationNumber());
        assertEquals(Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                Instant.ofEpochSecond(response.getMarketId().getLastUpdatedTs().getSeconds()).truncatedTo(ChronoUnit.DAYS));
        assertEquals(0, response.getRelativePartnerCount());
    }

    @Test
    @DisplayName("Партнер найден, со связанными партнерами")
    void testSuccessWithRelatives() {
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder()
                .setNeedRelatives(true)
                .setPartner(
                        MarketIdPartner
                                .newBuilder()
                                .setPartnerId(1000L)
                                .setPartnerType("SHOP")
                                .build()
                ).build();
        GetByPartnerResponse response = stub.getByPartner(request);
        assertTrue(response.getSuccess());
        assertEquals("", response.getMessage());
        assertEquals(1L, response.getMarketId().getMarketId());
        assertEquals("regNumber",response.getMarketId().getLegalInfo().getRegistrationNumber());
        assertEquals(Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                Instant.ofEpochSecond(response.getMarketId().getLastUpdatedTs().getSeconds()).truncatedTo(ChronoUnit.DAYS));
        assertEquals(2, response.getRelativePartnerCount());
    }
}
