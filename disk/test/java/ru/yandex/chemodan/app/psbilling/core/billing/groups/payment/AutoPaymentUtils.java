package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import java.math.BigDecimal;
import java.util.Currency;

import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceCalculator;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.inside.passport.PassportUid;

public class AutoPaymentUtils {
    public final Currency rub = Currency.getInstance("RUB");
    public final PassportUid uid = PassportUid.MAX_VALUE;

    @Autowired
    private ClientBalanceCalculator clientBalanceCalculator;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private PsBillingGroupsFactory psBillingGroupsFactory;
    @Autowired
    private PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    private PsBillingProductsFactory psBillingProductsFactory;
    @Autowired
    private PsBillingTransactionsFactory psBillingTransactionsFactory;
    @Autowired
    private PaymentFactory paymentFactory;
    @Autowired
    private BalanceClientStub balanceClientStub;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private GroupProductDao groupProductDao;
    @Autowired
    private CardDao cardDao;
    @Autowired
    protected DirectoryClient directoryClient;

    public GroupService createCorrectDataForResurrection(long clientId) {
        GroupService groupService = createDataForAutoPay(clientId);
        return voidBalanceAndDisableService(clientId, groupService);
    }

    public GroupService createCorrectDataForResurrection(long clientId, GroupProduct product) {
        GroupService groupService = createDataForAutoPay(clientId, product);
        return voidBalanceAndDisableService(clientId, groupService);
    }

    public GroupService createDataForAutoPay(long clientId) {
        GroupProduct product = createProduct(GroupPaymentType.PREPAID, 100);
        return createDataForAutoPay(clientId, product);
    }

    public GroupService createDataForAutoPay(long clientId, GroupProduct product) {
        Currency currency = product.getPriceCurrency();
        Group group = createGroup(clientId, true);
        mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        Option<CardEntity> primaryCardO = cardDao.findB2BPrimary(group.getPaymentInfo().get().getPassportUid());
        CardEntity card = primaryCardO.isPresent() ? primaryCardO.get() : insertPrimaryCard("card-x123" + clientId);
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), currency.getCurrencyCode()));
        ClientBalanceEntity balance = psBillingBalanceFactory.createBalance(clientId, currency, 1);
        createContract(clientId);
        GroupService service = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        return service;
    }


    @NotNull
    public GroupService voidBalanceAndDisableService(long clientId, GroupService groupService) {
        Currency currency = groupProductDao.findById(groupService.getGroupProductId()).getPriceCurrency();
        ClientBalanceEntity balance = clientBalanceDao.find(clientId, currency).get();
        clientBalanceDao.updateBalance(balance.getId(), BigDecimal.ZERO);
        clientBalanceCalculator.updateVoidDate(groupService);
        psBillingTransactionsFactory.disable(groupService);
        return groupServiceDao.findById(groupService.getId());
    }


    public GroupProduct createProduct(GroupPaymentType type, int price) {
        return psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(type).pricePerUserInMonth(BigDecimal.valueOf(price)));
    }


    public ClientBalanceEntity createBalance(Long clientId, double amount) {
        return psBillingBalanceFactory.createBalance(clientId, rub, amount);
    }

    public Group createGroup(Long clientId, boolean autoPayEnabled) {
        return psBillingGroupsFactory.createGroup(x ->
                x.paymentInfo(new BalancePaymentInfo(clientId, uid, autoPayEnabled)));
    }

    public void createContract(Long clientId) {
        psBillingBalanceFactory.createContract(clientId, 2L, rub);
    }

    public CardEntity insertPrimaryCard(String cardId) {
        return insertCard(cardId, CardPurpose.B2B_PRIMARY);
    }

    public CardEntity insertCard(String cardId, CardPurpose purpose) {
        return paymentFactory.insertCard(uid, cardId, purpose);
    }

    public void mockOrgsWhereUserIsAdmin(ListF<String> orgs) {
        Mockito.when(directoryClient
                        .organizationsWhereUserIsAdmin(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(orgs);
    }
}
