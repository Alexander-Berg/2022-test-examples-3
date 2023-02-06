package ru.yandex.market.marketId.grpc.acl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.id.ContactModificationResponse;
import ru.yandex.market.id.LinkContactRequest;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.id.SyncContactsRequest;
import ru.yandex.market.id.UnlinkContactRequest;
import ru.yandex.market.id.UpdateContactAccessesRequest;
import ru.yandex.market.marketId.grpc.GrpcFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AclTest extends GrpcFunctionalTest {

    //Sync Contacts tests

    @Test
    @DisplayName("Синхронизация доступов. Ничего не изменилось.")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAcl.before.csv")
    void updateAclAllTheSame() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);

        SyncContactsRequest request = SyncContactsRequest.newBuilder()
                .setMarketId(1L)
                .addAllUids(List.of(100L, 101L))
                .build();

        ContactModificationResponse response = stub.syncContactsRequest(request);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("Возвращаем false при отсутствии МаркетИД")
    void returnFalseOnMarketIdAbsent() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);

        SyncContactsRequest request = SyncContactsRequest.newBuilder()
                .setMarketId(1L)
                .addAllUids(List.of(100L, 101L))
                .build();
        ContactModificationResponse response = stub.syncContactsRequest(request);

        assertFalse(response.getSuccess());

        assertEquals("Market ID not found", response.getMessage());
    }

    @Test
    @DisplayName("Вернуть false, если передали пустой лист")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv")
    void returnFalseOnEmptyList() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);

        SyncContactsRequest request = SyncContactsRequest.newBuilder()
                .setMarketId(1L)
                .build();
        ContactModificationResponse response = stub.syncContactsRequest(request);

        assertFalse(response.getSuccess());

        assertEquals("List is empty", response.getMessage());
    }

    @Test
    @DisplayName("Добавление нового пользователя")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAclAddNewUser.after.csv")
    void updateAclAddNewUser() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);

        SyncContactsRequest request = SyncContactsRequest.newBuilder()
                .setMarketId(1L)
                .addAllUids(List.of(100L, 101L, 103L))
                .build();

        ContactModificationResponse response = stub.syncContactsRequest(request);
        assertTrue(response.getSuccess());
    }

    //Link Contact tests

    @Test
    @DisplayName("Добавление пользователя к маркетИД")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAclAddNewUser.after.csv")
    void testLinkNewUser() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkContactRequest request = LinkContactRequest.newBuilder()
                .setUid(103L)
                .setMarketId(1L)
                .build();
        ContactModificationResponse response = stub.linkContactRequest(request);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("Добавление пользователя к маркетИД. МаркетИД не найден")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAcl.before.csv")
    void testLinkNewUserMarketIdNotFound() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkContactRequest request = LinkContactRequest.newBuilder()
                .setUid(103L)
                .setMarketId(2L)
                .build();
        ContactModificationResponse response = stub.linkContactRequest(request);
        assertFalse(response.getSuccess());
        assertEquals("Market ID not found", response.getMessage());
    }

    @Test
    @DisplayName("Добавление пользователя к маркетИД. Пользователь уже привязан")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAcl.before.csv")
    void testLinkNewUserAlreadyLinked() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        LinkContactRequest request = LinkContactRequest.newBuilder()
                .setUid(100L)
                .setMarketId(1L)
                .build();
        ContactModificationResponse response = stub.linkContactRequest(request);
        assertTrue(response.getSuccess());
    }


    //Unlink contact tests

    @Test
    @DisplayName("Отвязать пользователя от маркетИД")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAclUnlinkUser.after.csv")
    void testUnlinkUser() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UnlinkContactRequest request = UnlinkContactRequest.newBuilder()
                .setUid(101L)
                .setMarketId(1L)
                .build();
        ContactModificationResponse response = stub.unlinkContactRequest(request);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("Отвязать пользователя от маркетИД. МаркетИД не найден")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAcl.before.csv")
    void testUnlinkUserMarketIdNotFound() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UnlinkContactRequest request = UnlinkContactRequest.newBuilder()
                .setUid(101L)
                .setMarketId(2L)
                .build();
        ContactModificationResponse response = stub.unlinkContactRequest(request);
        assertFalse(response.getSuccess());
        assertEquals("Market ID not found", response.getMessage());
    }

    @Test
    @DisplayName("Отвязать пользователя от маркетИД. Пользователь не найден")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAcl.before.csv")
    void testUnlinkUserUidNotFound() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UnlinkContactRequest request = UnlinkContactRequest.newBuilder()
                .setUid(103L)
                .setMarketId(1L)
                .build();
        ContactModificationResponse response = stub.unlinkContactRequest(request);
        assertFalse(response.getSuccess());
        assertEquals("Uid not found", response.getMessage());
    }

    @Test
    @DisplayName("Отвязать пользователя от маркетИД. Пользователь уже отвязан")
    @DbUnitDataSet(before = "csv/UpdateAcl.before.csv", after = "csv/UpdateAcl.before.csv")
    void testUnlinkNewUserAlreadyLinked() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UnlinkContactRequest request = UnlinkContactRequest.newBuilder()
                .setUid(102L)
                .setMarketId(1L)
                .build();
        ContactModificationResponse response = stub.unlinkContactRequest(request);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("Связать пользователя с 2 маркетИД")
    @DbUnitDataSet(before = "csv/UpdateContactAccesses.before.csv",
            after = "csv/UpdateContactAccesses-add.after.csv")
    void testupdateContact2New() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UpdateContactAccessesRequest request = UpdateContactAccessesRequest.newBuilder()
                .setUid(500)
                .addAllPartners(
                        Stream.of(MarketIdPartner.newBuilder().setPartnerId(1000).setPartnerType("SHOP").build(),
                                MarketIdPartner.newBuilder().setPartnerId(3000).setPartnerType("SHOP").build())
                                .collect(Collectors.toList()))
                .build();
        ContactModificationResponse response = stub.updateConatctAccesses(request);
        assertTrue(response.getSuccess());
    }


    @Test
    @DisplayName("Связать пользователя с 2 маркетИД и отвязать от старого")
    @DbUnitDataSet(before = "csv/UpdateContactAccesses.before.csv",
            after = "csv/UpdateContactAccesses-add-and-del.after.csv")
    void testupdateContactAddAndRemove() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UpdateContactAccessesRequest request = UpdateContactAccessesRequest.newBuilder()
                .setUid(100)
                .addAllPartners(
                        Stream.of(MarketIdPartner.newBuilder().setPartnerId(1000).setPartnerType("SHOP").build(),
                                MarketIdPartner.newBuilder().setPartnerId(5000).setPartnerType("SHOP").build())
                                .collect(Collectors.toList()))
                .build();
        ContactModificationResponse response = stub.updateConatctAccesses(request);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("Удалить пользователю связку с одним из маркетИД")
    @DbUnitDataSet(before = "csv/UpdateContactAccesses.before.csv",
            after = "csv/UpdateContactAccesses-del.after.csv")
    void testupdateContactRemove() {
        MarketIdServiceGrpc.MarketIdServiceBlockingStub stub
                = MarketIdServiceGrpc.newBlockingStub(channel);
        UpdateContactAccessesRequest request = UpdateContactAccessesRequest.newBuilder()
                .setUid(100)
                .addAllPartners(
                        Stream.of(MarketIdPartner.newBuilder().setPartnerId(1000).setPartnerType("SHOP").build())
                                .collect(Collectors.toList()))
                .build();
        ContactModificationResponse response = stub.updateConatctAccesses(request);
        assertTrue(response.getSuccess());
    }

}
