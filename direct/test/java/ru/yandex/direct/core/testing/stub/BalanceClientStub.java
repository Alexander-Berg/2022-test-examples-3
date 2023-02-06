package ru.yandex.direct.core.testing.stub;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.impl.DSL;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.BalanceXmlRpcClient;
import ru.yandex.direct.balance.client.model.request.CheckBindingRequest;
import ru.yandex.direct.balance.client.model.request.CreateClientRequest;
import ru.yandex.direct.balance.client.model.request.CreateInvoiceRequest;
import ru.yandex.direct.balance.client.model.request.CreateOrUpdateOrdersBatchRequest;
import ru.yandex.direct.balance.client.model.request.CreatePersonRequest;
import ru.yandex.direct.balance.client.model.request.CreateRequest2Item;
import ru.yandex.direct.balance.client.model.request.FindClientRequest;
import ru.yandex.direct.balance.client.model.request.GetBankRequest;
import ru.yandex.direct.balance.client.model.request.GetClientNdsRequest;
import ru.yandex.direct.balance.client.model.request.GetFirmCountryCurrencyRequest;
import ru.yandex.direct.balance.client.model.request.ListPaymentMethodsSimpleRequest;
import ru.yandex.direct.balance.client.model.request.ModulusReminderRequest;
import ru.yandex.direct.balance.client.model.request.PayRequestRequest;
import ru.yandex.direct.balance.client.model.request.TearOffPromocodeRequest;
import ru.yandex.direct.balance.client.model.request.createtransfermultiple.CreateTransferMultipleRequest;
import ru.yandex.direct.balance.client.model.response.BalanceBankDescription;
import ru.yandex.direct.balance.client.model.response.CheckBindingResponse;
import ru.yandex.direct.balance.client.model.response.ClientNdsItem;
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo;
import ru.yandex.direct.balance.client.model.response.CreateRequest2Response;
import ru.yandex.direct.balance.client.model.response.DirectDiscountItem;
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem;
import ru.yandex.direct.balance.client.model.response.FirmCountryCurrencyItem;
import ru.yandex.direct.balance.client.model.response.GetCardBindingURLResponse;
import ru.yandex.direct.balance.client.model.response.GetClientPersonsResponseItem;
import ru.yandex.direct.balance.client.model.response.GetOverdraftParamsResponse;
import ru.yandex.direct.balance.client.model.response.GetRequestChoicesResponse;
import ru.yandex.direct.balance.client.model.response.GetRequestChoicesResponseFull;
import ru.yandex.direct.balance.client.model.response.LinkedClientsItem;
import ru.yandex.direct.balance.client.model.response.ListPaymentMethodsSimpleResponseItem;
import ru.yandex.direct.balance.client.model.response.PartnerContractClientInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractCollateralInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractContractInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractInfo;
import ru.yandex.direct.balance.client.model.response.PartnerContractPersonInfo;
import ru.yandex.direct.balance.client.model.response.PayRequestResponse;
import ru.yandex.direct.balance.client.model.response.ProcessedInvoiceItem;
import ru.yandex.direct.dbschema.stubs.tables.records.BalanceClientsRecord;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.balance.client.model.response.GetClientPersonsResponseItem.NATURAL_PERSON_TYPE;
import static ru.yandex.direct.dbschema.stubs.Tables.BALANCE_CLIENTS;

public class BalanceClientStub extends BalanceClient {
    private final DatabaseWrapperProvider databaseWrapperProvider;

    public BalanceClientStub(DatabaseWrapperProvider databaseWrapperProvider) {
        super(mock(BalanceXmlRpcClient.class), mock(BalanceXmlRpcClient.class));
        this.databaseWrapperProvider = databaseWrapperProvider;
    }

    public long createClient() {
        BalanceClientsRecord clientsRecord = databaseWrapperProvider.get("stubs").getDslContext()
                .insertInto(BALANCE_CLIENTS)
                .set(BALANCE_CLIENTS.CLIENT_ID, DSL.defaultValue(Long.class))
                .returning(BALANCE_CLIENTS.CLIENT_ID)
                .fetchOne();
        return clientsRecord.getClientid();
    }

    @Override
    public List<PartnerContractInfo> getPartnerContracts(@Nullable Long clientId,
                                                         @Nullable String externalContractId,
                                                         java.time.Duration timeout) {
        return List.of(new PartnerContractInfo(
                new PartnerContractClientInfo(),
                singletonList(defaultPartnerContractCollateralInfo()),
                defaultPartnerContractContractInfo(),
                new PartnerContractPersonInfo()));
    }

    @Override
    public List<List<String>> createOrUpdateOrdersBatch(@NotNull CreateOrUpdateOrdersBatchRequest request) {
        return StreamEx.of(request.getItems())
                .map(r -> List.of("0", String.valueOf(r.getServiceOrderId())))
                .toList();
    }

    @NotNull
    @Override
    public List<FindClientResponseItem> findClient(FindClientRequest request) {
        return emptyList();
    }

    @Override
    public long createClient(CreateClientRequest request) {
        return Long.parseLong(request.getClientId());
    }

    @Override
    public long createClient(long operatorUid, CreateClientRequest request) {
        return Long.parseLong(request.getClientId());
    }

    @Override
    public CreateRequest2Response createRequest(Long operatorUid, Long clientId,
                                                CreateRequest2Item createRequest2Item, boolean forceUnmoderated,
                                                @Nullable String promocode, boolean denyPromocode) {
        return mock(CreateRequest2Response.class);
    }

    @Override
    public PayRequestResponse payRequest(PayRequestRequest request) {
        return mock(PayRequestResponse.class);
    }

    @Override
    public Long createInvoice(CreateInvoiceRequest request) {
        return mock(Long.class);
    }

    @Override
    public CheckBindingResponse checkBinding(CheckBindingRequest request) {
        return mock(CheckBindingResponse.class);
    }

    @Override
    public List<GetClientPersonsResponseItem> getClientPersons(Long clientId) {
        var item = new GetClientPersonsResponseItem();
        item.setId(clientId >> 2);
        item.setType(NATURAL_PERSON_TYPE);
        item.setHidden(0);
        item.setName("Stub person");
        return List.of(item);
    }

    @Override
    public Long createPerson(CreatePersonRequest request) {
        return mock(Long.class);
    }

    @Override
    public GetCardBindingURLResponse getCardBindingURL(Long operatorUid, String currencyCode,
                                                       @Nullable String returnPath, Integer serviceId,
                                                       boolean isMobileForm) {
        return mock(GetCardBindingURLResponse.class);
    }

    @Override
    public void createUserClientAssociation(Long operatorUid, Long clientId, Long representativeUid) {
    }

    @Override
    public void removeUserClientAssociation(Long operatorUid, Long clientId, Long representativeUid) {
    }

    @Override
    public void setAgencyLimitedRepSubclients(Long operatorUid, Long clientId, Long representativeUid,
                                              List<Long> subclientIds) {
    }

    @Override
    public void setOverdraftParams(Long personId, Integer serviceId, String paymentMethodCode,
                                   String isoCurrencyCode, BigDecimal clientLimit) {
    }

    @Override
    public ClientPassportInfo editPassport(Long operatorUid, Long clientUid, ClientPassportInfo passportInfo) {
        return mock(ClientPassportInfo.class);
    }

    @Override
    public GetRequestChoicesResponse getPaymentMethods(Long operatorUid, Long clientId,
                                                       List<CreateRequest2Item> orderItems) {
        return mock(GetRequestChoicesResponse.class);
    }

    @Override
    public GetRequestChoicesResponseFull getPaymentMethodsFull(Long operatorUid, Long clientId,
                                                               List<CreateRequest2Item> orderItems) {
        return mock(GetRequestChoicesResponseFull.class);
    }

    @Override
    public GetOverdraftParamsResponse getOverdraftParams(Integer serviceId, Long clientId) {
        return mock(GetOverdraftParamsResponse.class);
    }

    @Override
    public List<ClientPassportInfo> getClientRepresentativePassports(Long operatorUid, Long clientId) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<FirmCountryCurrencyItem> getFirmCountryCurrency(GetFirmCountryCurrencyRequest request) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<ClientNdsItem> getClientNds(GetClientNdsRequest request) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<ClientNdsItem> getClientNds(int serviceId, int clientId) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<DirectDiscountItem> getDirectDiscount(ModulusReminderRequest request) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<DirectDiscountItem> getDirectDiscount(int modulus, int reminder, Duration timeout) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<DirectDiscountItem> getDirectDiscount(int modulus, int reminder) {
        return emptyList();
    }

    @NotNull
    @Override
    public List<LinkedClientsItem> getLinkedClients(List<Integer> linkTypes) {
        return emptyList();
    }

    @Override
    public BalanceBankDescription getBank(String swift) {
        return mock(BalanceBankDescription.class);
    }

    @Override
    public BalanceBankDescription getBank(String swift, Duration timeout) {
        return mock(BalanceBankDescription.class);
    }

    @Override
    public BalanceBankDescription getBank(GetBankRequest request) {
        return mock(BalanceBankDescription.class);
    }

    @NotNull
    @Override
    public List<ProcessedInvoiceItem> tearOffPromocode(TearOffPromocodeRequest request) {
        return emptyList();
    }

    @NotNull
    @Override
    public ListPaymentMethodsSimpleResponseItem listPaymentMethodsSimple(ListPaymentMethodsSimpleRequest request) {
        return mock(ListPaymentMethodsSimpleResponseItem.class);
    }

    @NotNull
    @Override
    public ListPaymentMethodsSimpleResponseItem listPaymentMethods(ListPaymentMethodsSimpleRequest request) {
        return mock(ListPaymentMethodsSimpleResponseItem.class);
    }

    @NotNull
    @Override
    public ClientPassportInfo getPassportByUid(Long operatorUid, Long uid) {
        return mock(ClientPassportInfo.class);
    }

    @NotNull
    @Override
    public Set<Long> massCheckManagersExist(Collection<Long> managerUids) {
        return new HashSet<>(managerUids);
    }

    @Override
    public void createTransferMultiple(CreateTransferMultipleRequest request) {
    }

    public PartnerContractContractInfo defaultPartnerContractContractInfo() {
        var contractInfo = new PartnerContractContractInfo();
        contractInfo.setExternalContractId("2017/16");
        contractInfo.setServices(Set.of(7L));
        contractInfo.setStartDate(LocalDate.now());
        contractInfo.setDateOfSigning(LocalDate.now());
        return contractInfo;
    }

    public PartnerContractCollateralInfo defaultPartnerContractCollateralInfo() {
        var collateralInfo = new PartnerContractCollateralInfo();
        collateralInfo.setExternalCollateralId("007");
        collateralInfo.setCollateralTypeId(1070L);
        collateralInfo.setStartDate(LocalDate.now());
        collateralInfo.setDateOfSigning(LocalDate.now());
        collateralInfo.setPersonalDealBasePercent(Integer.toString(15));
        return collateralInfo;
    }
}
