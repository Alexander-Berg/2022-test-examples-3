package ru.yandex.market.mbi.api.controller.supplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.supplier.MemCachedSupplierService;
import ru.yandex.market.core.supplier.SupplierBasicAttributes;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.client.entity.supplier.SupplierBaseDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты на {@link SupplierCloneController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierCloneControllerFunctionalTest.csv")
@ExtendWith(MockitoExtension.class)
@Disabled("Контроллер просто надо удалить https://st.yandex-team.ru/MBI-84642")
class SupplierCloneControllerFunctionalTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 1000L;
    private static final long UID = 10L;
    private static final long CLIENT_ID = 1000L;
    private static final SupplierBaseDTO CLONED_SUPPLIER_DTO = new SupplierBaseDTO(
            "new-supplier-name",
            "new-supplier-domain"
    );
    @Autowired
    private CheckouterClient checkouterClient;
    @Mock
    private CheckouterShopApi shopApi;
    @Autowired
    private MemCachedSupplierService memCachedSupplierService;
    @Autowired
    private PrepayRequestDao prepayRequestDao;
    @Autowired
    private AboPublicRestClient aboPublicRestClient;
    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;
    @Autowired
    private NesuClient nesuClient;

    static Stream<Arguments> canNotCloneNot3pSupplierArgs() {
        return Stream.of(
                Arguments.of(123L, SupplierType.FIRST_PARTY),
                Arguments.of(124L, SupplierType.REAL_SUPPLIER)
        );
    }

    @BeforeEach
    void mock() {
        when(balanceService.getClient(eq(CLIENT_ID))).thenReturn(new ClientInfo(CLIENT_ID, ClientType.PHYSICAL));
        when(checkouterClient.shops()).thenReturn(shopApi);
        when(aboPublicRestClient
                .getSelfCheckScenarios(anyLong(), any(PlacementType.class), any(OrderProcessMethod.class)))
                .thenReturn(new ArrayList<>());
    }

    @Test
    @DisplayName("Поставщик не найден")
    void supplierNotFound() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.cloneSupplier(
                        100500L,
                        CLONED_SUPPLIER_DTO,
                        UID
                )
        );
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<error>\n" +
                        "    <message>Supplier not found: 100500</message>\n" +
                        "</error>",
                exception.getResponseBodyAsString()
        );
        verifyNoInteractions(partnerNotificationClient, checkouterClient);
    }

    @Test
    @DisplayName("Поставщик без заявки")
    void supplierWithoutPrepayRequest() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.cloneSupplier(
                        1001L,
                        CLONED_SUPPLIER_DTO,
                        UID
                )
        );
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<error>\n" +
                        "    <message>Prepay request not found: 1001</message>\n" +
                        "</error>",
                exception.getResponseBodyAsString()
        );
        verifyNoInteractions(partnerNotificationClient, checkouterClient);
    }

    @Test
    @DisplayName("Заявка поставщика в редактируемом статусе")
    void supplierCloneWithEditableApplication() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.cloneSupplier(
                        1002L,
                        CLONED_SUPPLIER_DTO,
                        UID
                )
        );
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<error>\n" +
                        "    <message>Can not clone application from status: IN_PROGRESS, permitted statuses: " +
                        "[COMPLETED]</message>\n" +
                        "</error>",
                exception.getResponseBodyAsString()
        );
        verifyNoInteractions(partnerNotificationClient, checkouterClient);
    }

    @Test
    @DisplayName("Корректный сценарий клонирования поставщика типа fulfillment")
    @DbUnitDataSet(before = "cloneFulfillmentSupplier.before.csv", after = "cloneFulfillmentSupplier.after.csv")
    void cloneFulfillmentSupplier() {
        cloneSupplier();
    }

    @Test
    @DisplayName("Корректный сценарий клонирования поставщика типа Dropship")
    @DbUnitDataSet(before = "cloneDropshipSupplier.before.csv", after = "cloneDropshipSupplier.after.csv")
    void cloneDropshipSupplier() {
        mockGetOrCreateMarketId(1L);
        mockLinkMarketId(1L);
        cloneSupplier();
        verify(nesuClient, times(1)).registerShop(any(RegisterShopDto.class));
    }

    @Test
    @DisplayName("Корректный сценарий клонирования поставщика типа Crossdock")
    @DbUnitDataSet(before = "cloneCrossdockSupplier.before.csv", after = "cloneCrossdockSupplier.after.csv")
    void cloneCrossdockSupplier() {
        cloneSupplier();
    }

    private void cloneSupplier() {
        PrepayRequest originalRequest = prepayRequestDao.findRequestsByIds(Collections.singleton(122349L)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        SupplierBaseDTO response = mbiApiClient.cloneSupplier(
                SUPPLIER_ID,
                CLONED_SUPPLIER_DTO,
                UID
        );
        assertThat(response).isEqualTo(new SupplierBaseDTO(1L, "new-supplier-name", "new-supplier-domain"));
        Collection<PrepayRequest> requests = prepayRequestDao.findRequestsByIds(Collections.singleton(122349L));
        assertThat(requests).hasSize(2);
        PrepayRequest prepayRequest = requests.stream()
                .filter(r -> r.getDatasourceId() == 1L)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Request was not cloned"));
        MatcherAssert.assertThat(
                prepayRequest,
                SupplierPrepayRequestMatcher.equals(originalRequest)
        );
        assertThat(prepayRequest.getDatasourceId()).isEqualTo(1L);
        assertThat(prepayRequest.getStartDate()).isNotNull();
        assertThat(prepayRequest.getUpdatedAt()).isNotNull();
        assertThat(prepayRequest.getCreatedAt()).isNotNull();
        var reqCaptor = verifySentNotificationType(partnerNotificationClient, 1, 1534753826L);
        assertThat(reqCaptor.getValue().getData()).contains(
                "campaign-id>1<",
                "prepay-request-id>" + prepayRequest.getId() + "<",
                "supplier-name>" + response.getName() + "<",
                "organization-name>" + prepayRequest.getOrganizationName() + "<"
        );
        verify(memCachedSupplierService, times(4)).cleanCache(
                argThat(arg -> arg.getCampaignId() == 1L
                        && arg.getDatasourceId() == 1L
                        && arg.getClientId() == 1000L
                        && arg.getInfo().equals(SupplierBasicAttributes.of(
                                "new-supplier-name",
                        "new-supplier-domain"
                        ))
                        && arg.getPrepayRequestId() == 122349L
                ));
        verify(shopApi).updateShopData(
                eq(1L),
                argThat(arg -> arg.getClientId() == 43527064L
                        && arg.getCampaignId() == 1L
                        && arg.getInn().equals("9717045604")
                        && arg.getPhoneNumber().equals("+7 4956680647")
                        && arg.getSandboxClass() == PaymentClass.YANDEX
                        && arg.getPrepayType() == PrepayType.YANDEX_MARKET
                ));
    }

    @ParameterizedTest
    @MethodSource("canNotCloneNot3pSupplierArgs")
    @DisplayName("Проверить, что разрешается клонировать только 3P поставщиков")
    @DbUnitDataSet(before = "canNotCloneNot3pSupplier.before.csv")
    void canNotCloneNot3pSupplier(long supplierId, SupplierType supplierType) {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.cloneSupplier(
                        supplierId,
                        CLONED_SUPPLIER_DTO,
                        UID
                )
        );
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        MbiAsserts.assertXmlEquals(
                String.format(
                        //language=xml
                        "<error>\n" +
                                "    <message>Can not clone supplier with type: %s, permitted types: " +
                                "[THIRD_PARTY]</message>\n" +
                                "</error>",
                        supplierType.name()
                ),
                exception.getResponseBodyAsString()
        );
        verifyNoInteractions(partnerNotificationClient, checkouterClient);
    }

    @Test
    @DbUnitDataSet(after = "cloneSupplierWithoutReturnContact.after.csv")
    @DisplayName("Клонирование поставщика, не имеющего контакта для осуществления возвратов")
    void cloneSupplierWithoutReturnContact() {
        SupplierBaseDTO response = mbiApiClient.cloneSupplier(
                SUPPLIER_ID,
                CLONED_SUPPLIER_DTO,
                UID
        );
        assertThat(response).isEqualTo(new SupplierBaseDTO(1L, "new-supplier-name", "new-supplier-domain"));
    }

    private void mockLinkMarketId(long marketId) {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return true;
        }).when(marketIdServiceImplBase).linkMarketIdRequest(any(), any());
    }

    private void mockGetOrCreateMarketId(long marketId) {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return marketId;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
    }
}
