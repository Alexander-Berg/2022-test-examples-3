package ru.yandex.chemodan.app.psbilling.core.mocks;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.balanceclient.BalanceClient;
import ru.yandex.chemodan.balanceclient.exception.BalanceErrorCodeException;
import ru.yandex.chemodan.balanceclient.model.request.CheckBindingRequest;
import ru.yandex.chemodan.balanceclient.model.request.CreateInvoiceRequest;
import ru.yandex.chemodan.balanceclient.model.request.CreateOfferRequest;
import ru.yandex.chemodan.balanceclient.model.request.CreatePersonRequest;
import ru.yandex.chemodan.balanceclient.model.request.CreateRequest2Item;
import ru.yandex.chemodan.balanceclient.model.request.CreateRequest2Request;
import ru.yandex.chemodan.balanceclient.model.request.FindClientRequest;
import ru.yandex.chemodan.balanceclient.model.request.GetBoundPaymentMethodsRequest;
import ru.yandex.chemodan.balanceclient.model.request.GetCardBindingURLRequest;
import ru.yandex.chemodan.balanceclient.model.request.GetClientActsRequest;
import ru.yandex.chemodan.balanceclient.model.request.GetClientContractsRequest;
import ru.yandex.chemodan.balanceclient.model.request.PayRequestRequest;
import ru.yandex.chemodan.balanceclient.model.response.CheckBindingResponse;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.chemodan.balanceclient.model.response.ClientActInfo;
import ru.yandex.chemodan.balanceclient.model.response.ClientPassportInfo;
import ru.yandex.chemodan.balanceclient.model.response.Contract;
import ru.yandex.chemodan.balanceclient.model.response.CreateOfferResponse;
import ru.yandex.chemodan.balanceclient.model.response.CreateRequest2Response;
import ru.yandex.chemodan.balanceclient.model.response.FindClientResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetBoundPaymentMethodsResponse;
import ru.yandex.chemodan.balanceclient.model.response.GetCardBindingURLResponse;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetClientPersonsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetOrdersInfoResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetRequestChoicesResponse;
import ru.yandex.chemodan.balanceclient.model.response.PayRequestResponse;
import ru.yandex.chemodan.balanceclient.model.response.PaymentMethod;
import ru.yandex.chemodan.balanceclient.model.response.PaymentMethodDetails;
import ru.yandex.chemodan.balanceclient.model.response.Person;
import ru.yandex.chemodan.balanceclient.model.response.PersonPaymentMethodDetails;
import ru.yandex.chemodan.balanceclient.model.response.RequestPaymentMethod;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.random.Random2;

public class BalanceClientStub extends BalanceClient {
    public static final long DEFAULT_CONTRACT_PERSON_ID = 555L;

    private final MapF<Long, ListF<GetClientContractsResponseItem>> clientsContracts = Cf.hashMap();
    private final MapF<Long, GetPartnerBalanceContractResponseItem> contractBalances = Cf.hashMap();
    private final MapF<Long, ListF<GetOrdersInfoResponseItem>> contractOrders = Cf.hashMap();
    private final MapF<Long, ListF<GetClientPersonsResponseItem>> clientPersons = Cf.hashMap();
    private final MapF<Long, ListF<ClientActInfo>> clientActs = Cf.hashMap();
    private final MapF<String, CreateRequest2Request> paymentRequests = Cf.hashMap();
    private final MapF<String, CheckRequestPaymentResponse> paymentRequestStatuses = Cf.hashMap();
    private final MapF<PassportUid, SetF<Card>> boundCards = Cf.hashMap();

    @Getter
    private final BalanceClient balanceClientMock;
    private final SetF<String> mockedMethods = Cf.hashSet();

    public BalanceClientStub() {
        super(null, null);
        balanceClientMock = Mockito.mock(BalanceClient.class);
    }

    public void reset() {
        Mockito.reset(balanceClientMock);
        clientsContracts.clear();
        contractBalances.clear();
        contractOrders.clear();
        clientPersons.clear();
        clientActs.clear();
        paymentRequests.clear();
        paymentRequestStatuses.clear();
        boundCards.clear();
        mockedMethods.clear();
    }

    @Override
    public List<ClientActInfo> getClientActs(GetClientActsRequest request) {
        List<ClientActInfo> mockResult = balanceClientMock.getClientActs(request);
        if (isMockitoOn("getClientActs")) {
            return mockResult;
        }
        return clientActs.getO(Long.parseLong(request.getClientId())).orElseGet(Cf::list);
    }

    @Override
    public GetBoundPaymentMethodsResponse[] getBoundPaymentMethods(GetBoundPaymentMethodsRequest request) {
        GetBoundPaymentMethodsResponse[] mockResult = balanceClientMock.getBoundPaymentMethods(request);
        if (isMockitoOn("getBoundPaymentMethods")) {
            return mockResult;
        }
        ListF<GetBoundPaymentMethodsResponse> responses = Cf.arrayList();
        Option<SetF<Card>> boundCardsO = boundCards.getO(PassportUid.cons(request.getOperatorUid()));
        if (boundCardsO.isPresent()) {
            for (Card card : boundCardsO.get()) {
                GetBoundPaymentMethodsResponse response = new GetBoundPaymentMethodsResponse();
                response.setPaymentMethodId(card.cardExternalId);
                response.setPaymentMethod(BalanceService.CARD_PAYMENT_METHOD);
                response.setExpired(card.expired);
                responses.add(response);
            }
        }
        return responses.toArray(new GetBoundPaymentMethodsResponse[0]);
    }

    @Override
    public CheckBindingResponse checkBinding(CheckBindingRequest request) {
        CheckBindingResponse mockResult = balanceClientMock.checkBinding(request);
        if (isMockitoOn("checkBinding")) {
            return mockResult;
        }
        CheckBindingResponse checkBindingResponse = new CheckBindingResponse();
        checkBindingResponse.withBindingResult("success");
        checkBindingResponse.withPaymentMethodId("card-xxx");

        return checkBindingResponse;
    }

    @Override
    public List<GetClientPersonsResponseItem> getClientPersons(Long clientId) {
        List<GetClientPersonsResponseItem> mockResult = balanceClientMock.getClientPersons(clientId);
        if (isMockitoOn("getClientPersons")) {
            return mockResult;
        }
        return clientPersons.getOrElse(clientId, Cf.list());
    }

    @Override
    public GetCardBindingURLResponse getCardBindingURL(GetCardBindingURLRequest request) {
        GetCardBindingURLResponse mockResult = balanceClientMock.getCardBindingURL(request);
        if (isMockitoOn("getCardBindingURL")) {
            return mockResult;
        }

        GetCardBindingURLResponse getCardBindingURLResponse = new GetCardBindingURLResponse();
        String purchaseToken = UUID.randomUUID().toString();
        getCardBindingURLResponse.withBindingUrl("https://trust-fake.yandex.ru" +
                "/web/binding?purchase_token=" + purchaseToken);
        getCardBindingURLResponse.withPurchaseToken(purchaseToken);
        return getCardBindingURLResponse;
    }

    public void createOrReplaceClientActs(Long clientId, List<ClientActInfo> acts) {
        this.clientActs.put(clientId, Cf.toList(acts));
    }

    @Override
    public GetClientContractsResponseItem[] getClientContracts(GetClientContractsRequest request) {
        GetClientContractsResponseItem[] mockResult = balanceClientMock.getClientContracts(request);
        if (isMockitoOn("getClientContracts")) {
            return mockResult;
        }
        ListF<GetClientContractsResponseItem> contracts =
                clientsContracts.getO(Long.parseLong(request.getClientId())).orElseGet(Cf::list);
        return contracts.toArray(new GetClientContractsResponseItem[contracts.size()]);
    }

    @Override
    public GetPartnerBalanceContractResponseItem[] getPartnerBalance(Integer serviceId, ListF<Long> contractIds) {
        GetPartnerBalanceContractResponseItem[] mockResult = balanceClientMock.getPartnerBalance(serviceId,
                contractIds);
        if (isMockitoOn("getPartnerBalance")) {
            return mockResult;
        }
        ListF<GetPartnerBalanceContractResponseItem> balanceContracts = contractBalances.entries()
                .filter(balanceContact -> contractIds.containsTs(balanceContact.get1())).map(Tuple2::get2);
        return balanceContracts.toArray(new GetPartnerBalanceContractResponseItem[balanceContracts.size()]);
    }

    @Override
    public CreateRequest2Response createRequest(Long operatorUid, Long clientId,
                                                CreateRequest2Item createRequest2Item, boolean forceUnmoderated,
                                                Option<String> invoiceType) {
        CreateRequest2Response mockResult = balanceClientMock.createRequest(operatorUid, clientId,
                createRequest2Item, forceUnmoderated,
                invoiceType);

        if (isMockitoOn("createRequest")) {
            return mockResult;
        }
        CreateRequest2Request request2Request = createRequest2Request(operatorUid, clientId, createRequest2Item,
                forceUnmoderated, invoiceType);
        Long requestId = Random2.R.nextLong();
        paymentRequests.put(requestId.toString(), request2Request);
        CreateRequest2Response requestResponse = new CreateRequest2Response();
        requestResponse.setRequestID(requestId);
        return requestResponse;
    }

    @Override
    public GetRequestChoicesResponse getPaymentMethods(Long operatorUid, long contractId, long requestId) {
        Contract contract = new Contract();
        contract.setId(contractId);
        Person person = new Person();
        person.setId(DEFAULT_CONTRACT_PERSON_ID);

        PaymentMethod paymentMethod = new PaymentMethod();

        PaymentMethodDetails payment = new PaymentMethodDetails();
        payment.setPaymentMethodCode(BalanceService.PAYMENT_METHOD_BANK);
        payment.setPaymentMethod(paymentMethod);

        PersonPaymentMethodDetails personPayment = new PersonPaymentMethodDetails();
        personPayment.setContract(contract);
        personPayment.setPerson(person);
        personPayment.setPaymentMethodDetails(Cf.list(payment));

        GetRequestChoicesResponse response = new GetRequestChoicesResponse();
        response.setPersonPaymentMethodDetails(Cf.list(personPayment));

        return response;
    }

    @Override
    public List<RequestPaymentMethod> getRequestPaymentMethodsForContract(PassportUid uid, String requestId,
                                                                          String contractId) {
        List<RequestPaymentMethod> mockResult =
                balanceClientMock.getRequestPaymentMethodsForContract(uid, requestId, contractId);
        if (isMockitoOn("getRequestPaymentMethodsForContract")) {
            return mockResult;
        }
        if (!paymentRequests.containsKeyTs(requestId)) {
            return Cf.list();
        }
        CreateRequest2Request paymentRequest = paymentRequests.getTs(requestId);

        GetClientContractsResponseItem contract = clientsContracts.getTs(paymentRequest.getClientId())
                .filter(GetClientContractsResponseItem::isActive)
                .first();

        String currency = contract.getCurrency();

        ListF<RequestPaymentMethod> paymentMethods = Cf.arrayList();
        RequestPaymentMethod unboundCardPaymentMethod = new RequestPaymentMethod();
        unboundCardPaymentMethod.setContractId(Option.of(contractId));
        unboundCardPaymentMethod.setCurrency(currency);
        unboundCardPaymentMethod.setPaymentMethodId(Option.empty());
        unboundCardPaymentMethod.setPaymentMethodType(BalanceService.CARD_PAYMENT_METHOD);
        paymentMethods.add(unboundCardPaymentMethod);

        Option<SetF<Card>> boundCardsO = boundCards.getO(uid);
        if (boundCardsO.isPresent()) {
            for (Card card : boundCardsO.get().filter(x -> x.currency.equals(currency))) {
                RequestPaymentMethod paymentMethod = new RequestPaymentMethod();
                paymentMethod.setContractId(Option.of(contractId));
                paymentMethod.setCurrency(currency);
                paymentMethod.setPaymentMethodId(Option.of(card.cardExternalId));
                paymentMethod.setPaymentMethodType(BalanceService.CARD_PAYMENT_METHOD);
                paymentMethods.add(paymentMethod);
            }
        }

        return paymentMethods;
    }

    @Override
    public PayRequestResponse payRequest(PayRequestRequest request) {
        PayRequestResponse mockResult = balanceClientMock.payRequest(request);
        if (isMockitoOn("payRequest")) {
            return mockResult;
        }
        if (!paymentRequests.containsKeyTs(request.getRequestId())) {
            throw new BalanceErrorCodeException(Cf.list(), String.format("No request %s", request.getRequestId()), "");
        }
        PayRequestResponse response = new PayRequestResponse();
        try {
            response.setPaymentUrl(String.format("https://trust-fake.yandex.ru/web/payment" +
                            "?purchase_token=%s&request_id=%s&method=%s",
                    "token",
                    URLEncoder.encode(request.getRequestId(), "UTF-8"),
                    URLEncoder.encode(request.getPaymentMethodId(), "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        response.setRequestId(request.getRequestId());
        response.setPurchaseToken(UUID.randomUUID().toString());
        return response;
    }

    @Override
    public List<ClientPassportInfo> getClientRepresentativePassports(Long clientId, Option<Long> operatorUid) {
        List<ClientPassportInfo> mockResult =
                balanceClientMock.getClientRepresentativePassports(clientId, operatorUid);
        if (isMockitoOn("getClientRepresentativePassports")) {
            return mockResult;
        }
        return Cf.list();
    }

    @Override
    public GetOrdersInfoResponseItem[] getOrdersInfo(String contractId) {
        GetOrdersInfoResponseItem[] mockResult = balanceClientMock.getOrdersInfo(contractId);
        if (isMockitoOn("getOrdersInfo")) {
            return mockResult;
        }
        ListF<GetOrdersInfoResponseItem> orders = contractOrders.getO(Long.valueOf(contractId)).getOrElse(Cf::list);
        return orders.toArray(new GetOrdersInfoResponseItem[orders.size()]);
    }

    public void createOrReplaceClientContract(Long clientId, GetClientContractsResponseItem contract) {
        ListF<GetClientContractsResponseItem> clientContracts = clientsContracts.getO(clientId).getOrElse(Cf::arrayList)
                .filter(clientContract -> !clientContract.getId().equals(contract.getId()));
        clientContracts = clientContracts.plus(contract);
        clientsContracts.put(clientId, clientContracts);
        contractOrders.put(contract.getId(), Cf.toList(contract.getServices()).map(this::createOrder));
    }

    @Override
    public CheckRequestPaymentResponse checkRequestPayment(Integer serviceId, String requestId, Option<String> transactionId,
                                                           Long operatorUid) {
        if (isMockitoOn("checkRequestPayment")) {
            return balanceClientMock.checkRequestPayment(serviceId, requestId, transactionId, operatorUid);
        }
        return paymentRequestStatuses.getTs(requestId);
    }

    @Override
    public long createInvoice(CreateInvoiceRequest request) {
        return Random2.R.nextLong();
    }

    @Override
    public List<FindClientResponseItem> findClient(FindClientRequest request) {
        if (isMockitoOn("findClient")) {
            return balanceClientMock.findClient(request);
        }
        return Cf.list();
    }

    @Override
    public Long createPerson(CreatePersonRequest request) {
        if (isMockitoOn("createPerson")) {
            return balanceClientMock.createPerson(request);
        }
        return 0L;
    }

    @Override
    public CreateOfferResponse createOffer(CreateOfferRequest request) {
        if (isMockitoOn("createOffer")) {
            return balanceClientMock.createOffer(request);
        }
        return new CreateOfferResponse();
    }

    public void addContractBalance(GetPartnerBalanceContractResponseItem contractBalance) {
        contractBalances.put(contractBalance.getContractId(), contractBalance);
    }

    public void addPaymentRequestStatusCheck(CheckRequestPaymentResponse paymentResponse) {
        paymentRequestStatuses.put(paymentResponse.getRequestId(), paymentResponse);
    }

    public void setClientPersons(long clientId, ListF<GetClientPersonsResponseItem> persons) {
        clientPersons.put(clientId, persons);
    }

    public void turnOnMockitoForMethod(String method) {
        mockedMethods.add(method);
    }

    public void addBoundCard(PassportUid uid, Card card) {
        if (boundCards.containsKeyTs(uid)) {
            boundCards.getTs(uid).removeIf(x -> x.cardExternalId.equals(card.cardExternalId));
            boundCards.getTs(uid).add(card);
        } else {
            boundCards.put(uid, Cf.hashSet(card));
        }
    }

    private boolean isMockitoOn(String method) {
        return mockedMethods.containsTs(method);
    }

    private GetOrdersInfoResponseItem createOrder(Integer serviceId) {
        GetOrdersInfoResponseItem order = new GetOrdersInfoResponseItem();
        order.setServiceId(serviceId);
        order.setGroupServiceOrderId(UUID.randomUUID().toString());
        order.setProductId(UUID.randomUUID().toString());
        order.setServiceOrderId(UUID.randomUUID().toString());
        return order;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private final String cardExternalId;
        private final String currency;
        private boolean expired = false;
    }
}
