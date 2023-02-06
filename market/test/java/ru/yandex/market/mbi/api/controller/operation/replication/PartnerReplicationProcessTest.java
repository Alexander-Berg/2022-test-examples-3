package ru.yandex.market.mbi.api.controller.operation.replication;

import java.util.OptionalLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType.FBS;

/**
 * Функциональный тест, проверяющий вызовы в mbi-api, которые делает камунда.
 */
class PartnerReplicationProcessTest extends FunctionalTest {

    private static final long UID = 10000;
    private static final long CLIENT_ID = 1000;
    private static final long MARKET_ID = 333;
    private static final long PARTNER_DONOR_ID = 10;
    private static final long PARTNER_REPLICA_ID = 20;
    private static final long BUSINESS_ID = 11;
    private static final int REGION_ID = 213;

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Autowired
    private NesuClient nesuClient;

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationProcess.testReplicationFbsToFby.before.csv",
            after = "PartnerReplicationProcess.testReplicationFbsToFby.after.csv"
    )
    void testFbsToFbyReplication() {
        Mockito.when(balanceService.getClient(CLIENT_ID)).thenReturn(new ClientInfo(CLIENT_ID, ClientType.OAO));
        var request = new ReplicatePartnerRequest();
        request.setPartnerDonorId(PARTNER_DONOR_ID);
        request.setAcceptorPlacementType(PartnerPlacementType.FBY);
        var response = getMbiOpenApiClient().replicatePartner(UID, request);
        getMbiOpenApiClient().requestPartnerFeedDefault(UID, response.getPartnerId());
        getMbiOpenApiClient().requestPartnerBalanceCopy(
                UID,
                new PartnerBalanceReplicateRequest()
                        .newPartnerId(response.getPartnerId())
                        .sourcePartnerId(PARTNER_DONOR_ID)
        );
        getMbiOpenApiClient().createPartnerApplicationRequest(
                UID,
                response.getPartnerId(),
                new PartnerApplicationRequest()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationProcess.testReplicationDbsToFby.before.csv",
            after = "PartnerReplicationProcess.testReplicationDbsToFby.after.csv"
    )
    void testDbsToFbyReplication() {
        Mockito.when(balanceService.getClient(CLIENT_ID)).thenReturn(new ClientInfo(CLIENT_ID, ClientType.OAO));
        var request = new ReplicatePartnerRequest();
        request.setPartnerDonorId(PARTNER_DONOR_ID);
        request.setAcceptorPlacementType(PartnerPlacementType.FBY);
        var response = getMbiOpenApiClient().replicatePartner(UID, request);
        getMbiOpenApiClient().requestPartnerFeedDefault(UID, response.getPartnerId());
        getMbiOpenApiClient().requestPartnerBalanceCopy(
                UID,
                new PartnerBalanceReplicateRequest()
                        .newPartnerId(response.getPartnerId())
                        .sourcePartnerId(PARTNER_DONOR_ID)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testDbsToFbsReplication.before.csv",
            after = "PartnerReplicationController.testDbsToFbsReplication.after.csv"
    )
    void testDbsToFbsReplication() {
        Mockito.when(balanceService.getClient(CLIENT_ID)).thenReturn(new ClientInfo(CLIENT_ID, ClientType.OOO));
        Mockito.when(marketIdGrpcService.getOrCreateMarketId(PARTNER_REPLICA_ID, true)).thenReturn(OptionalLong.of(MARKET_ID));

        // копирование параметров
        var request = new ReplicatePartnerRequest()
                .partnerDonorId(PARTNER_DONOR_ID)
                .replicaPartnerId(PARTNER_REPLICA_ID)
                .acceptorPlacementType(FBS);

        var response = getMbiOpenApiClient().replicatePartner(UID, request);

        // создание дефолтового фида
        getMbiOpenApiClient().requestPartnerFeedDefault(UID, response.getPartnerId());

        // копирование в баланс
        getMbiOpenApiClient().requestPartnerBalanceCopy(
                UID,
                new PartnerBalanceReplicateRequest()
                        .newPartnerId(response.getPartnerId())
                        .sourcePartnerId(PARTNER_DONOR_ID)
        );

        var partnerLegalInfoRequest = new PartnerLegalInfoRequest()
                .newPartnerId(response.getPartnerId())
                .sourcePartnerId(PARTNER_DONOR_ID);

        // копирование юр данных
        var partnerLegalInfoResponse = getMbiOpenApiClient()
                .requestPartnerLegalInfoCopy(UID, partnerLegalInfoRequest);

        // активация партнера
        getMbiOpenApiClient().activatePartner(PARTNER_REPLICA_ID, UID);

        Assertions.assertEquals(123L, partnerLegalInfoResponse.getPartnerApplicationId());
        Assertions.assertEquals(true, partnerLegalInfoResponse.getVatCopied());
        Assertions.assertEquals(true, partnerLegalInfoResponse.getReturnContactCopied());

        // Проверяем, что создаем партнера в Nesu
        verify(nesuClient).registerShop(RegisterShopDto.builder()
                .id(PARTNER_REPLICA_ID)
                .marketId(MARKET_ID)
                .businessId(BUSINESS_ID)
                .name("Какое-то имя")
                .role(ShopRole.DROPSHIP)
                .balanceClientId(CLIENT_ID)
                .balanceContractId(3601036L)
                .balancePersonId(14764595L)
                .regionId(REGION_ID)
                .build());
        verifyNoMoreInteractions(nesuClient);
    }
}
