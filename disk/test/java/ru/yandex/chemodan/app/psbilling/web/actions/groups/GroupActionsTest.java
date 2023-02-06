package ru.yandex.chemodan.app.psbilling.web.actions.groups;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.TrustCardBindingDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupPartnerDao;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.SentEmailInfoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardBindingStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.TrustCardBinding;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.OptionalFeaturesManager;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.B2BTariffAcquisitionEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.chemodan.app.psbilling.web.PsBillingWebTestConfig;
import ru.yandex.chemodan.app.psbilling.web.exceptions.AccessDeniedException;
import ru.yandex.chemodan.app.psbilling.web.model.GroupPojo;
import ru.yandex.chemodan.app.psbilling.web.model.GroupProductPojo;
import ru.yandex.chemodan.app.psbilling.web.model.GroupProductSetPojo;
import ru.yandex.chemodan.app.psbilling.web.model.GroupServicesPojo;
import ru.yandex.chemodan.app.psbilling.web.model.GroupStatusApi;
import ru.yandex.chemodan.app.psbilling.web.model.GroupTypeApi;
import ru.yandex.chemodan.app.psbilling.web.model.GroupsByPayerResponsePojo;
import ru.yandex.chemodan.app.psbilling.web.model.PaymentCardPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PaymentCardsPojo;
import ru.yandex.chemodan.app.psbilling.web.model.TrustCardBindingFormPojo;
import ru.yandex.chemodan.balanceclient.model.response.CheckBindingResponse;
import ru.yandex.chemodan.balanceclient.model.response.GetBoundPaymentMethodsResponse;
import ru.yandex.chemodan.balanceclient.model.response.GetCardBindingURLResponse;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryUsersInfoResponse;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.chemodan.util.exception.AccessForbiddenException;
import ru.yandex.chemodan.util.exception.NotFoundException;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

@ContextConfiguration(classes = {
        PsBillingWebTestConfig.class,
        GroupActionsTest.BalanceMockConfiguration.class
})
public class GroupActionsTest extends AbstractPsBillingCoreTest {
    private static final PassportUid PASSPORT_UID = PassportUid.MAX_VALUE;

    @Autowired
    private GroupActions groupActions;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private BalanceClientStub balanceClientStub;
    @Autowired
    private PsBillingTransactionsFactory psBillingTransactionsFactory;
    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private OptionalFeaturesManager optionalFeaturesManager;
    @Autowired
    private UserProductManager userProductManager;
    @Autowired
    private CardDao cardDao;
    @Autowired
    private TrustCardBindingDao trustCardBindingDao;
    @Autowired
    private PaymentFactory paymentFactory;
    @Autowired
    private EmailHelper emailHelper;
    @Autowired
    private SentEmailInfoDao sentEmailInfoDao;
    @Autowired
    private GroupPartnerDao groupPartnerDao;

    @Autowired
    private DirectoryClient directoryClient;

    private static final String OPTIONAL_TEST_FEATURE_AVAILABLE = "test_feature_available";

    private Group group;
    private GroupProduct groupProduct1;
    private GroupProduct groupProduct2;

    @Test
    public void testEnableOptionalFeature() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1111L, PASSPORT_UID);
        group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(paymentInfo).type(GroupType.ORGANIZATION).ownerUid(PASSPORT_UID)
        );

        groupActions.enableOptionalFeature(
                GroupTypeApi.ORGANIZATION,
                group.getExternalId(),
                OPTIONAL_TEST_FEATURE_AVAILABLE
        );
        Mockito.verify(optionalFeaturesManager).enable(
                argThat(arg -> arg.getId().equals(group.getId())), eq(PASSPORT_UID), eq(OPTIONAL_TEST_FEATURE_AVAILABLE)
        );
    }

    @Test
    public void testDisableOptionalFeature() {
        testEnableOptionalFeature();
        groupActions.disableOptionalFeature(
                GroupTypeApi.ORGANIZATION,
                group.getExternalId(),
                OPTIONAL_TEST_FEATURE_AVAILABLE
        );
        Mockito.verify(optionalFeaturesManager).disable(
                argThat(arg -> arg.getId().equals(group.getId())), eq(PASSPORT_UID), eq(OPTIONAL_TEST_FEATURE_AVAILABLE)
        );
    }

    @Test
    public void getOrganizationsByPayer() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1L, PASSPORT_UID);
        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo).externalId("1"));
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo).externalId("2"));
        Group group3 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo).externalId("3"));
        psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(2L, PassportUid.cons(321)))
                .externalId("4"));

        GroupService groupService1 = psBillingGroupsFactory.createGroupService(group1, groupProduct1);
        GroupService groupService2 = psBillingGroupsFactory.createGroupService(group2, groupProduct1);
        GroupService groupService3 = psBillingGroupsFactory.createGroupService(group3, groupProduct2);

        GroupsByPayerResponsePojo pojo = groupActions.getGroupsByPayer(PASSPORT_UID.toUidOrZero(), "ru");

        Assert.sizeIs(3, pojo.getGroups());
        assertGroupAndPojo(pojo.getGroups().filter(x -> x.getGroupId() == 1).first(), group1, groupService1,
                groupProduct1);
        assertGroupAndPojo(pojo.getGroups().filter(x -> x.getGroupId() == 2).first(), group2, groupService2,
                groupProduct1);
        assertGroupAndPojo(pojo.getGroups().filter(x -> x.getGroupId() == 3).first(), group3, groupService3,
                groupProduct2);

        pojo = groupActions.getGroupsByPayer(PassportUid.MIN_VALUE.toUidOrZero(), "ru");
        Assert.isEmpty(pojo.getGroups());
    }

    @Test
    public void clientBalanceNotUpdatesOnGroupCheckBecauseOfTransactions() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1L, PASSPORT_UID);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo).externalId("1"));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct1.getCode()), 1);
        psBillingBalanceFactory.createContractBalance(psBillingBalanceFactory.createContract(1L, 2L,
                        groupProduct1.getPriceCurrency()),
                x -> x.withClientPaymentsSum(BigDecimal.TEN));

        GroupPojo answer = groupActions.getGroup(PASSPORT_UID.toUidOrZero(), GroupTypeApi.ORGANIZATION, "1");
        Assert.equals(1, answer.getBalanceAmounts().length());
        BigDecimal amount = answer.getBalanceAmounts().get(0).getAmount();
        Assert.equals(BigDecimal.valueOf(10), amount);

        DateUtils.shiftTime(Duration.standardHours(12));
        answer = groupActions.getGroup(PASSPORT_UID.toUidOrZero(), GroupTypeApi.ORGANIZATION, "1");
        amount = answer.getBalanceAmounts().get(0).getAmount();
        Assert.equals(BigDecimal.valueOf(10), amount); // not changed cause of no transactions calculation
    }

    @Test
    public void getGroupServicesWithDisabledDate() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1L, PASSPORT_UID);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo).externalId("1"));
        Instant disabledDate = Instant.now();
        PsBillingTransactionsFactory.Service service =
                new PsBillingTransactionsFactory.Service(groupProduct1.getCode()).disabledAt(disabledDate);
        psBillingTransactionsFactory.createService(group, service, 1);

        GroupServicesPojo answer = groupActions.getGroupServices(PASSPORT_UID.toUidOrZero(),
                GroupTypeApi.ORGANIZATION, "1", Option.empty(), Option.of("disabled"));
        Assert.equals(1, answer.getItems().size());
        Option<Instant> actualDisabledDate = answer.getItems().get(0).getActualDisabledAt();
        Assert.assertTrue(actualDisabledDate.isPresent());
        Assert.equals(disabledDate, actualDisabledDate.get());
    }

    @Test
    public void getGroupServicesWithFreeServices() {
        Group group = psBillingGroupsFactory.createGroup();
        PsBillingTransactionsFactory.Service service =
                new PsBillingTransactionsFactory.Service(groupProduct1.getCode()).skipTransactionsExport(true);
        psBillingTransactionsFactory.createService(group, service, 1);

        GroupProduct freeProduct = psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.ZERO));
        psBillingGroupsFactory.createGroupService(group, freeProduct);

        GroupProduct freeProduct2 = psBillingProductsFactory.createGroupProduct(x -> x.skipTransactionsExport(true));
        psBillingGroupsFactory.createGroupService(group, freeProduct2);

        GroupServicesPojo answer = groupActions.getGroupServices(PASSPORT_UID.toUidOrZero(),
                GroupTypeApi.ORGANIZATION, group.getExternalId(), Option.empty(), Option.empty());
        Assert.equals(3, answer.getItems().size());
        Assert.assertTrue(answer.getItems().get(0).getIsFree());
        Assert.assertTrue(answer.getItems().get(1).getIsFree());
        Assert.assertTrue(answer.getItems().get(2).getIsFree());
    }

    @Test
    public void getGroupProductsIncognito() {
        psBillingGroupsFactory.createGroup();
        ProductLineEntity productLine = psBillingProductsFactory.createProductLine("set");
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(groupProduct.getId()));

        GroupProductSetPojo set = groupActions.getGroupProductSet(
                PassportUidOrZero.zero(),
                "set",
                Option.empty(),
                Option.empty(),
                null,
                false,
                Cf.list(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty());
        Assert.equals(1, set.getItems().size());
        GroupProductPojo productPojo = set.getItems().get(0);
        Assert.equals(groupProduct.getCode(), productPojo.getProductId());
        UserProduct userProduct = userProductManager.findById(groupProduct.getUserProductId());
        Assert.equals(userProduct.getCodeFamily(), productPojo.getCodeFamily());
    }

    @Test
    public void getGroupProductsForPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup();
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");
        ProductLineEntity productLine = psBillingProductsFactory.createProductLine("set");
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(groupProduct.getId()));

        GroupProductSetPojo set = groupActions.getGroupProductSet(
                PassportUidOrZero.fromUid(1234),
                "set",
                Option.of(GroupTypeApi.fromCoreEnum(group.getType())),
                Option.of(group.getExternalId()),
                null,
                false,
                Cf.list(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty());
        Assert.isEmpty(set.getItems());
    }

    @Test
    public void shouldSendTariffAcquisitionEmail() {
        Group group = setupSendTariffAcquisitionEmail(uid);

        // Check email for the first time
        groupActions.getGroupProductSet(
                PassportUidOrZero.fromUid(uid),
                "set",
                Option.of(GroupTypeApi.fromCoreEnum(group.getType())),
                Option.of(group.getExternalId()),
                null,
                true,
                Cf.list(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty());

        String emailKey = "b2b_tariff_acquisition";
        emailHelper.checkSingleLocalizedEmailArgs(B2BTariffAcquisitionEmailTask.class, "b2b_tariff_acquisition", Cf.list());
        bazingaTaskManagerStub.executeTasks();
        mailSenderMockConfig.verifyEmailSent();
        // manually add record about email sending, because it doesn't happen with test sender
        sentEmailInfoDao.createOrUpdate(SentEmailInfoDao.InsertData.builder()
                .emailTemplateKey(emailKey)
                .uid(uid.toString())
                .build());

        bazingaTaskManagerStub.clearTasks();
        mailSenderMockConfig.reset();
        groupActions.getGroupProductSet(
                PassportUidOrZero.fromUid(uid),
                "set",
                Option.of(GroupTypeApi.fromCoreEnum(group.getType())),
                Option.of(group.getExternalId()),
                null,
                true,
                Cf.list(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty());
        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BTariffAcquisitionEmailTask.class), 1);
        bazingaTaskManagerStub.executeTasks();
        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void shouldNotSendTariffAcquisitionEmailWhenUserIsNotFree() {
        Group group = setupSendTariffAcquisitionEmail(uid);
        Mockito.when(directoryClient
                        .usersInOrganization(Mockito.any(), Mockito.anyBoolean(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(Cf.set(uid.getUid()));

        groupActions.getGroupProductSet(
                PassportUidOrZero.fromUid(uid),
                "set",
                Option.of(GroupTypeApi.fromCoreEnum(group.getType())),
                Option.of(group.getExternalId()),
                null,
                true,
                Cf.list(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty());

        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BTariffAcquisitionEmailTask.class), 1);
        // User bought product before task was executed
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(group, groupProduct);
        UUID priceId = psBillingProductsFactory.createUserProductPrices(
                groupProduct.getUserProduct().getId(), Function.identityF()).getId();
        psBillingUsersFactory.createUserService(groupProduct.getUserProduct().getId(),
                x -> x.uid(uid.toString()).userProductPriceId(Option.of(priceId)));

        bazingaTaskManagerStub.executeTasks();
        // Check that no email was sent
        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void shouldNotSendTariffAcquisitionEmailIfNotFromLanding() {
        Group group = setupSendTariffAcquisitionEmail(uid);

        groupActions.getGroupProductSet(
                PassportUidOrZero.fromUid(uid),
                "set",
                Option.of(GroupTypeApi.fromCoreEnum(group.getType())),
                Option.of(group.getExternalId()),
                null,
                false,
                Cf.list(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty());

        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(SendLocalizedEmailTask.class), 0);
    }

    private Group setupSendTariffAcquisitionEmail(PassportUid uid) {
        String groupExternalId = UUID.randomUUID().toString();
        Group group = psBillingGroupsFactory.createGroup(x -> x.ownerUid(uid).externalId(groupExternalId));
        ProductLineEntity productLine = psBillingProductsFactory.createProductLine("set");
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(groupProduct.getId()));
        featureFlags.getB2bPleaseComeBackEmailEnabled().setValue("true");
        return group;
    }

    @Test
    public void getGroupWithNotActualStatus() {
        long clientId = 1L;
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(clientId, uid.toUid())));

        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, 2L, Currency.getInstance("RUB")),
                x -> x.withActSum(BigDecimal.valueOf(100))
                        .withFirstDebtAmount(BigDecimal.valueOf(100))
                        .withFirstDebtPaymentTermDT(DateUtils.futureDate()));

        GroupPojo groupFromHandler = groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId());

        group = groupDao.findById(group.getId());
        Assert.equals(GroupStatus.ACTIVE, group.getStatus());
        Assert.equals(GroupStatusApi.PAYMENT_REQUIRED, groupFromHandler.getStatus());

        bazingaTaskManagerStub.executeTasks();

        group = groupDao.findById(group.getId());
        Assert.equals(GroupStatus.PAYMENT_REQUIRED, group.getStatus());
    }

    @Test
    public void getGroupPaymentCards_byOtherUid() {
        mockBalanceGetBoundPaymentMethods(Cf.list(UUID.randomUUID().toString()), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, PassportUid.cons(2222))));

        Assert.assertThrows(() -> groupActions.getGroupPaymentCards(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), AccessForbiddenException.class);
    }

    @Test
    public void getGroupPaymentCards_noPaymentInfo() {
        mockBalanceGetBoundPaymentMethods(Cf.list(UUID.randomUUID().toString()), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId());
        Assert.isEmpty(paymentCards.getItems());
    }

    @Test
    public void getGroupPaymentCards_noSavedCards() {
        String cardBalanceId = UUID.randomUUID().toString();
        mockBalanceGetBoundPaymentMethods(Cf.list(cardBalanceId), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId());

        Assert.equals(paymentCards.getItems().size(), 1);
        PaymentCardPojo paymentCard = paymentCards.getItems().get(0);
        Assert.equals(paymentCard.getCardId(), cardBalanceId);
        Assert.assertFalse(paymentCard.isPrimary());
        validateCardBalanceData(paymentCard);
    }

    @Test
    public void getGroupPaymentCards_noBalanceCards() {
        String cardBalanceId = UUID.randomUUID().toString();
        String cardBalanceId2 = UUID.randomUUID().toString();

        mockBalanceGetBoundPaymentMethods(Cf.list(), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));

        CardEntity card1 = createCard(uid.toUid(), CardPurpose.B2B_PRIMARY,
                cardBalanceId, CardStatus.ACTIVE);
        CardEntity card2 = createCard(uid.toUid(), CardPurpose.B2B,
                cardBalanceId2, CardStatus.EXPIRED);

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId());
        Assert.assertTrue(paymentCards.getItems().isEmpty());

        card1 = cardDao.findById(card1.getId());
        card2 = cardDao.findById(card2.getId());
        Assert.equals(CardStatus.DISABLED, card1.getStatus());
        Assert.equals(CardStatus.DISABLED, card2.getStatus());
        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void getGroupPaymentCards_expiredBalanceCards() {
        String cardBalanceId = UUID.randomUUID().toString();
        String cardBalanceId2 = UUID.randomUUID().toString();
        String cardBalanceId3 = UUID.randomUUID().toString();

        mockBalanceGetBoundPaymentMethods(Cf.list(), Cf.list(cardBalanceId, cardBalanceId2, cardBalanceId3));

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));

        CardEntity card1 = createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardBalanceId, CardStatus.ACTIVE);
        CardEntity card2 = createCard(uid.toUid(), CardPurpose.B2B, cardBalanceId2, CardStatus.DISABLED);

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId());
        Assert.assertTrue(paymentCards.getItems().isEmpty());

        card1 = cardDao.findById(card1.getId());
        card2 = cardDao.findById(card2.getId());
        Assert.equals(CardStatus.EXPIRED, card1.getStatus());
        Assert.equals(CardStatus.EXPIRED, card2.getStatus());
        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void getGroupPaymentCards_disabledSavedCard() {
        String cardBalanceId = UUID.randomUUID().toString();
        mockBalanceGetBoundPaymentMethods(Cf.list(cardBalanceId), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        CardEntity card = createCard(uid.toUid(), CardPurpose.B2B_PRIMARY,
                cardBalanceId, CardStatus.DISABLED);

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId());
        Assert.equals(paymentCards.getItems().size(), 1);
        PaymentCardPojo paymentCard = paymentCards.getItems().get(0);
        Assert.equals(paymentCard.getCardId(), cardBalanceId);
        Assert.assertTrue(paymentCard.isPrimary());
        validateCardBalanceData(paymentCard);

        card = cardDao.findById(card.getId());
        Assert.equals(CardStatus.ACTIVE, card.getStatus());
    }

    @Test
    public void getGroupPaymentCards_expiredSavedCard() {
        String cardBalanceId = UUID.randomUUID().toString();
        mockBalanceGetBoundPaymentMethods(Cf.list(cardBalanceId), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        CardEntity card = createCard(uid.toUid(), CardPurpose.B2B_PRIMARY,
                cardBalanceId, CardStatus.EXPIRED);

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId());
        Assert.equals(paymentCards.getItems().size(), 1);
        PaymentCardPojo paymentCard = paymentCards.getItems().get(0);
        Assert.equals(paymentCard.getCardId(), cardBalanceId);
        Assert.assertTrue(paymentCard.isPrimary());
        validateCardBalanceData(paymentCard);

        card = cardDao.findById(card.getId());
        Assert.equals(CardStatus.ACTIVE, card.getStatus());
    }

    @Test
    public void getGroupPaymentCards_activeSavedCard() {
        String cardBalanceId = UUID.randomUUID().toString();

        mockBalanceGetBoundPaymentMethods(Cf.list(cardBalanceId), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        CardEntity card = createCard(uid.toUid(), CardPurpose.B2B_PRIMARY,
                cardBalanceId, CardStatus.ACTIVE);

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId());

        Assert.equals(paymentCards.getItems().size(), 1);
        PaymentCardPojo paymentCard = paymentCards.getItems().get(0);
        Assert.equals(paymentCard.getCardId(), cardBalanceId);
        Assert.assertTrue(paymentCard.isPrimary());
        validateCardBalanceData(paymentCard);

        card = cardDao.findById(card.getId());
        Assert.equals(CardStatus.ACTIVE, card.getStatus());
    }

    @Test
    public void getGroupPaymentCards_multipleCards() {
        String cardBalanceId = UUID.randomUUID().toString();
        String cardBalanceId2 = UUID.randomUUID().toString();
        String cardBalanceId3 = UUID.randomUUID().toString();

        mockBalanceGetBoundPaymentMethods(Cf.list(cardBalanceId, cardBalanceId2, cardBalanceId3), Cf.list());

        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        CardEntity card1 = createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardBalanceId, CardStatus.ACTIVE);
        CardEntity card2 = createCard(uid.toUid(), CardPurpose.B2B, cardBalanceId2, CardStatus.EXPIRED);
        CardEntity card3 = createCard(uid.toUid(), CardPurpose.B2B, cardBalanceId3, CardStatus.DISABLED);

        PaymentCardsPojo paymentCards = groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId());
        Assert.equals(paymentCards.getItems().size(), 3);

        card1 = cardDao.findById(card1.getId());
        card2 = cardDao.findById(card2.getId());
        card3 = cardDao.findById(card3.getId());
        Assert.equals(CardStatus.ACTIVE, card1.getStatus());
        Assert.equals(CardStatus.ACTIVE, card2.getStatus());
        Assert.equals(CardStatus.ACTIVE, card3.getStatus());
    }

    @Test
    public void getGroupPaymentCards_partnerClient() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");

        Assert.assertThrows(() -> groupActions.getGroupPaymentCards(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void enableAutoBilling_byOtherUid() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, PassportUid.cons(2222))));

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), AccessForbiddenException.class);
    }

    @Test
    public void enableAutoBilling_emptyPaymentInfo() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void enableAutoBilling_noActivePrimaryCard() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.DISABLED);
        createCard(uid.toUid(), CardPurpose.B2B, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void enableAutoBilling_hasActivePostpaidService() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(postpaidProduct.getCode()), 1);

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void enableAutoBilling_hasActiveFreePostpaidService() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        GroupProduct postpaidProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.POSTPAID).
                        pricePerUserInMonth(BigDecimal.valueOf(0)).
                        hidden(true).
                        skipTransactionsExport(true));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(postpaidProduct.getCode()), 1);

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION, group.getExternalId());

        Group afterEnable = groupDao.findById(group.getId());
        Assert.assertTrue(afterEnable.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void enableAutoBilling_oneGroup() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION, group.getExternalId());

        Group afterEnable = groupDao.findById(group.getId());
        Assert.assertTrue(afterEnable.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertTrue(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());
    }

    @Test
    public void enableAutoBilling_enableAlreadyEnabled() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertTrue(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION, group.getExternalId());

        Group afterEnable = groupDao.findById(group.getId());
        Assert.assertTrue(afterEnable.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertTrue(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());
    }

    @Test
    public void enableAutoBilling_multipleGroups() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        Assert.assertFalse(group1.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(group2.getPaymentInfo().get().isB2bAutoBillingEnabled());

        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(), CardStatus.ACTIVE);

        groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION, group1.getExternalId());

        Group group1AfterEnable = groupDao.findById(group1.getId());
        Assert.assertTrue(group1AfterEnable.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Group group2AfterEnable = groupDao.findById(group2.getId());
        Assert.assertTrue(group2AfterEnable.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void enableAutoBilling_partnerClient() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");

        Assert.assertThrows(() -> groupActions.enableAutobilling(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void disableAutoBilling_byOtherUid() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, PassportUid.cons(2222))));

        Assert.assertThrows(() -> groupActions.disableAutobilling(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), AccessForbiddenException.class);
    }

    @Test
    public void disableAutoBilling_emptyPaymentInfo() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        Assert.assertThrows(() -> groupActions.disableAutobilling(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void disableAutoBilling_success() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertTrue(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());

        groupActions.disableAutobilling(uid, GroupTypeApi.ORGANIZATION, group.getExternalId());

        Group afterDisable = groupDao.findById(group.getId());
        Assert.assertFalse(afterDisable.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());
    }

    @Test
    public void disableAutoBilling_disableAlreadyDisabled() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());

        groupActions.disableAutobilling(uid, GroupTypeApi.ORGANIZATION, group.getExternalId());

        Group afterDisable = groupDao.findById(group.getId());
        Assert.assertFalse(afterDisable.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, group.getExternalId()).isAutoBillingEnabled());
    }

    @Test
    public void disableAutoBilling_multipleGroups() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));
        Assert.assertTrue(group1.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertTrue(group2.getPaymentInfo().get().isB2bAutoBillingEnabled());

        groupActions.disableAutobilling(uid, GroupTypeApi.ORGANIZATION, group1.getExternalId());

        Group group1AfterDisable = groupDao.findById(group1.getId());
        Assert.assertFalse(group1AfterDisable.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Group group2AfterDisable = groupDao.findById(group2.getId());
        Assert.assertFalse(group2AfterDisable.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void disableAutoBilling_partnerClient() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");

        Assert.assertThrows(() -> groupActions.disableAutobilling(uid,
                GroupTypeApi.ORGANIZATION, group.getExternalId()), A3ExceptionWithStatus.class);
    }

    @Test
    public void setCardPurpose_unsupportedPurpose() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid(), true)));

        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId(), cardExternalId, "unknown_purpose"), A3ExceptionWithStatus.class);
    }

    @Test
    public void setCardPurpose_unknownGroup() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);

        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION,
                "unknown_group", cardExternalId, "b2b_primary"), NotFoundException.class);
    }

    @Test
    public void setCardPurpose_emptyPaymentInfo() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId(), cardExternalId, "b2b_primary"), A3ExceptionWithStatus.class);
    }

    @Test
    public void setCardPurpose_byOtherUid() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, PassportUid.cons(2222))));

        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId(), cardExternalId, "b2b_primary"), AccessForbiddenException.class);
    }

    @Test
    public void setCardPurpose_alreadyPrimary() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION, group.getExternalId(),
                cardExternalId, "b2b_primary");

        ListF<CardEntity> cards = cardDao.findCardsByUid(uid.toUid());
        Assert.equals(cards.size(), 1);
        Assert.equals(cards.first().getPurpose(), CardPurpose.B2B_PRIMARY);

        // при выставлении/смене главной карты включаем автосписание
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void setCardPurpose_unknownCardId() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        String cardExternalId1 = UUID.randomUUID().toString();
        String cardExternalId2 = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId1, CardStatus.ACTIVE);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION, group.getExternalId(),
                cardExternalId2, "b2b_primary");

        Option<CardEntity> card1 = cardDao.findByExternalId(uid.toUid(), cardExternalId1);
        Assert.assertTrue(card1.isPresent());
        Assert.equals(card1.get().getPurpose(), CardPurpose.B2B);

        Option<CardEntity> card2 = cardDao.findByExternalId(uid.toUid(), cardExternalId2);
        Assert.assertTrue(card2.isPresent());
        Assert.equals(card2.get().getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.equals(card2.get().getStatus(), CardStatus.ACTIVE);

        // при выставлении/смене главной карты включаем автосписание
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void setCardPurpose_knownCardNotPrimary() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        String cardExternalId1 = UUID.randomUUID().toString();
        String cardExternalId2 = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId1, CardStatus.ACTIVE);
        createCard(uid.toUid(), CardPurpose.B2B, cardExternalId2, CardStatus.ACTIVE);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION, group.getExternalId(),
                cardExternalId2, "b2b_primary");

        Option<CardEntity> card1 = cardDao.findByExternalId(uid.toUid(), cardExternalId1);
        Assert.assertTrue(card1.isPresent());
        Assert.equals(card1.get().getPurpose(), CardPurpose.B2B);

        Option<CardEntity> card2 = cardDao.findByExternalId(uid.toUid(), cardExternalId2);
        Assert.assertTrue(card2.isPresent());
        Assert.equals(card2.get().getPurpose(), CardPurpose.B2B_PRIMARY);

        // при выставлении/смене главной карты включаем автосписание
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void setCardPurpose_b2b() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION, group.getExternalId(),
                cardExternalId, "b2b");

        Option<CardEntity> card = cardDao.findByExternalId(uid.toUid(), cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(card.get().getPurpose(), CardPurpose.B2B);

        // при выставлении purpose, отличной от b2b_primary, не включаем автосписание
        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void setCardPurpose_partnerClient() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");

        String cardExternalId = UUID.randomUUID().toString();
        createCard(uid.toUid(), CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.ACTIVE);

        Assert.assertThrows(() -> groupActions.setCardPurpose(uid,
                        GroupTypeApi.ORGANIZATION, group.getExternalId(),
                        cardExternalId, "b2b"),
                A3ExceptionWithStatus.class);
    }

    @Test
    public void getCardBindingForm_emptyPaymentInfo() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        Assert.assertThrows(() -> groupActions.getCardBindingForm(uid, GroupTypeApi.ORGANIZATION,
                        group.getExternalId(), "RUB", Option.empty(), Option.empty()),
                A3ExceptionWithStatus.class);
    }

    @Test
    public void getCardBindingForm_byOtherUid() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, PassportUid.cons(2222))));

        Assert.assertThrows(() -> groupActions.getCardBindingForm(uid, GroupTypeApi.ORGANIZATION,
                        group.getExternalId(), "RUB", Option.empty(), Option.empty()),
                AccessForbiddenException.class);
    }

    @Test
    public void getCardBindingForm_successRub() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        String transactionId = UUID.randomUUID().toString();
        mockBalanceGetCardBindingURL(transactionId);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        TrustCardBindingFormPojo response = groupActions.getCardBindingForm(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId(), "RUB", Option.empty(), Option.empty());
        Assert.assertFalse(response.getUrl().isEmpty());
        Assert.assertFalse(response.getBindingId().isEmpty());

        validateTrustCardBinding(transactionId);
    }

    @Test
    public void getCardBindingForm_successUsd() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        String transactionId = UUID.randomUUID().toString();
        mockBalanceGetCardBindingURL(transactionId);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        TrustCardBindingFormPojo response = groupActions.getCardBindingForm(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId(), "USD", Option.empty(), Option.empty());
        Assert.assertFalse(response.getUrl().isEmpty());
        Assert.assertFalse(response.getBindingId().isEmpty());

        validateTrustCardBinding(transactionId);
    }

    @Test
    public void getCardBindingForm_balanceError() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);

        Mockito.when(balanceClientStub.getBalanceClientMock().getCardBindingURL(Mockito.any()))
                .thenThrow(new RuntimeException());

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));

        Assert.assertThrows(() -> groupActions.getCardBindingForm(uid, GroupTypeApi.ORGANIZATION,
                group.getExternalId(), "RUB", Option.empty(), Option.empty()), RuntimeException.class);

        ListF<TrustCardBinding> bindings =
                trustCardBindingDao.findInitBindings(Instant.now().plus(Duration.standardMinutes(1)),
                        1, Option.empty());
        Assert.assertTrue(bindings.isEmpty());
    }

    @Test
    public void getCardBindingForm_partnerClient() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid.toUid())));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");

        Assert.assertThrows(() -> groupActions.getCardBindingForm(uid,
                        GroupTypeApi.ORGANIZATION, group.getExternalId(),
                        "RUB", Option.empty(), Option.empty()),
                A3ExceptionWithStatus.class);
    }

    @Test
    public void checkTrustCardBindingNotification_noBindingId() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);

        char[] bindingId = binding.getId().toString().toCharArray();
        bindingId[0] = (bindingId[0] == '1' ? '2' : '1'); // чтобы точно была другая строка
        groupActions.checkTrustCardBindingNotification(new String(bindingId));

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.INIT, bindingAfterCheck.getStatus());
        Assert.assertTrue(bindingAfterCheck.getCardId().isEmpty());
    }

    @Test
    public void checkTrustCardBindingNotification_noTransactionId() {
        PassportUid uid = PassportUid.cons(1111);
        TrustCardBinding binding = insertTrustCardBinding(uid, Option.empty(), CardBindingStatus.INIT);

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.INIT, bindingAfterCheck.getStatus());
        Assert.assertTrue(bindingAfterCheck.getCardId().isEmpty());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingInErrorStatus() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.ERROR);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid)));

        String cardExternalId = UUID.randomUUID().toString();
        mockCheckBinding(cardExternalId);

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        // привязка была в статусе ERROR - при повторной проверке меняем данные
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        Option<CardEntity> card = cardDao.findByExternalId(uid, cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(CardStatus.ACTIVE, card.get().getStatus());
        Assert.equals(CardPurpose.B2B_PRIMARY, card.get().getPurpose());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.equals(card.get().getId(), bindingAfterCheck.getCardId().get());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingInSuccessStatus() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.SUCCESS);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid)));

        mockCheckBindingFailure();

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        // привязка уже была в статусе SUCCESS - при повторной проверке ничего не меняем
        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.assertTrue(bindingAfterCheck.getError().isEmpty());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingError() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);
        mockCheckBindingFailure();

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.ERROR, bindingAfterCheck.getStatus());
        Assert.assertTrue(bindingAfterCheck.getCardId().isEmpty());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingNoCardId() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);
        mockCheckBinding("");

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.INIT, bindingAfterCheck.getStatus());
        Assert.assertTrue(bindingAfterCheck.getCardId().isEmpty());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingSuccess_autoPayWasDisabled() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);

        String cardExternalId = UUID.randomUUID().toString();
        mockCheckBinding(cardExternalId);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid)));

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        Option<CardEntity> card = cardDao.findByExternalId(uid, cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(CardStatus.ACTIVE, card.get().getStatus());
        Assert.equals(CardPurpose.B2B_PRIMARY, card.get().getPurpose());

        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.equals(card.get().getId(), bindingAfterCheck.getCardId().get());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingSuccess_autoPayWasDisabled_cantEnableDueToTimeout() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);

        String cardExternalId = UUID.randomUUID().toString();
        mockCheckBinding(cardExternalId);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid)));

        DateUtils.shiftTime(Duration.standardMinutes(2000));

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        Option<CardEntity> card = cardDao.findByExternalId(uid, cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(CardStatus.ACTIVE, card.get().getStatus());
        Assert.equals(CardPurpose.B2B, card.get().getPurpose());

        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.equals(card.get().getId(), bindingAfterCheck.getCardId().get());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingSuccess_autoPayWasEnabled() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);

        String cardExternalId = UUID.randomUUID().toString();
        mockCheckBinding(cardExternalId);

        createCard(uid, CardPurpose.B2B_PRIMARY, UUID.randomUUID().toString(),
                CardStatus.ACTIVE);

        psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid, true)));

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        Option<CardEntity> card = cardDao.findByExternalId(uid, cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(CardStatus.ACTIVE, card.get().getStatus());
        Assert.equals(CardPurpose.B2B, card.get().getPurpose());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.equals(card.get().getId(), bindingAfterCheck.getCardId().get());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingSuccessExistingCard() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);
        String cardExternalId = UUID.randomUUID().toString();
        mockCheckBinding(cardExternalId);
        createCard(uid, CardPurpose.B2B_PRIMARY, cardExternalId, CardStatus.DISABLED);

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        Option<CardEntity> card = cardDao.findByExternalId(uid, cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(CardStatus.ACTIVE, card.get().getStatus());
        Assert.equals(CardPurpose.B2B_PRIMARY, card.get().getPurpose());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.equals(card.get().getId(), bindingAfterCheck.getCardId().get());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingInProgress() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);
        mockCheckBindingInProgress();

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.INIT, bindingAfterCheck.getStatus());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingInProgress_timeout() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);
        mockCheckBindingInProgress();

        DateUtils.shiftTime(Duration.standardMinutes(61));

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.ERROR, bindingAfterCheck.getStatus());
    }

    @Test
    public void checkTrustCardBindingNotification_bindingSuccessWithNewCardId() {
        PassportUid uid = PassportUid.cons(1111);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        TrustCardBinding binding = insertTrustCardBinding(uid, transactionId, CardBindingStatus.INIT);
        String cardExternalId = UUID.randomUUID().toString();
        mockCheckBinding("new_card", cardExternalId);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(1L, uid)));

        groupActions.checkTrustCardBindingNotification(binding.getId().toString());

        Option<CardEntity> card = cardDao.findByExternalId(uid, cardExternalId);
        Assert.assertTrue(card.isPresent());
        Assert.equals(CardStatus.ACTIVE, card.get().getStatus());
        Assert.equals(CardPurpose.B2B_PRIMARY, card.get().getPurpose());

        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        TrustCardBinding bindingAfterCheck = trustCardBindingDao.findById(binding.getId());
        Assert.equals(CardBindingStatus.SUCCESS, bindingAfterCheck.getStatus());
        Assert.equals(card.get().getId(), bindingAfterCheck.getCardId().get());

        Mockito.verify(balanceClientStub.getBalanceClientMock(), Mockito.times(2))
                .checkBinding(Mockito.any());
    }

    @Test
    public void testCheckPermission() {
        featureFlags.getHotFixGroupValidationEnabled().setValue(Boolean.TRUE.toString());
        final String orgId = "BAD_ID";
        final PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);
        psBillingProductsFactory.createGroupProduct(x -> x.code("product_code"));

        Mockito
                .when(directoryClient.getUserInfo(eq(uid.toUid()), eq(orgId)))
                .thenReturn(Option.of(new DirectoryUsersInfoResponse(uid.getUid(), false)));


        Assert.assertThrows(() -> groupActions.getGroup(uid, GroupTypeApi.ORGANIZATION, orgId),
                AccessDeniedException.class);
        Assert.assertThrows(() -> groupActions.getGroupPaymentCards(uid, GroupTypeApi.ORGANIZATION, orgId),
                AccessDeniedException.class);
        Assert.assertThrows(() -> groupActions.setCardPurpose(uid, GroupTypeApi.ORGANIZATION, orgId, "", ""),
                AccessDeniedException.class);
        Assert.assertThrows(() -> groupActions.enableAutobilling(uid, GroupTypeApi.ORGANIZATION, orgId),
                AccessDeniedException.class);
        Assert.assertThrows(() -> groupActions.disableAutobilling(uid, GroupTypeApi.ORGANIZATION, orgId),
                AccessDeniedException.class);
        Assert.assertThrows(() -> groupActions.getCardBindingForm(uid, GroupTypeApi.ORGANIZATION, orgId, "RUB",
                        Option.empty(), Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> groupActions.getProductInfo(uid, Option.of(GroupTypeApi.ORGANIZATION),
                        Option.of(orgId), Option.empty(), ""),
                AccessDeniedException.class);
        // no exception if no group
        groupActions.getProductInfo(uid, Option.empty(), Option.empty(), Option.empty(), "product_code");
        Assert.assertThrows(() -> groupActions.getGroupServices(uid, GroupTypeApi.ORGANIZATION, orgId, Option.empty()
                        , Option.empty()),
                AccessDeniedException.class);
    }


    private void assertGroupAndPojo(
            GroupsByPayerResponsePojo.GroupsByPayerPojo pojo,
            Group group,
            GroupService groupService,
            GroupProduct groupProduct
    ) {
        Assert.equals(group.getExternalId(), pojo.getGroupId().toString());
        Assert.sizeIs(1, pojo.getServices());
        Assert.equals(groupService.getId(), pojo.getServices().first().getId());
        Assert.equals("Тестовый ключ", pojo.getServices().first().getProductName().get());
        Assert.equals(groupProduct.getPricePerUserInMonth(), pojo.getServices().first().getPricePerUserInMonth());
        Assert.equals(groupProduct.getPriceCurrency().toString(), pojo.getServices().first().getCurrency());
    }

    private void mockBalanceGetBoundPaymentMethods(ListF<String> activeCardBalanceIds,
                                                   ListF<String> expiredCardBalanceIds) {
        balanceClientStub.turnOnMockitoForMethod("getBoundPaymentMethods");

        String cardAccount = "1234****5678";
        String cardHolder = "holder";
        String cardExpirationYear = "2030";
        String cardExpirationMonth = "06";
        String cardPaymentSystem = "MasterCard";

        ListF<GetBoundPaymentMethodsResponse> responses = new ArrayListF<>();

        for (String cardId : activeCardBalanceIds) {
            GetBoundPaymentMethodsResponse cardResponseItem = new GetBoundPaymentMethodsResponse();
            cardResponseItem.setPaymentMethod("card");
            cardResponseItem.setPaymentMethodId(cardId);
            cardResponseItem.setCurrency("RUB");
            cardResponseItem.setAccount(cardAccount);
            cardResponseItem.setHolder(cardHolder);
            cardResponseItem.setExpirationYear(cardExpirationYear);
            cardResponseItem.setExpirationMonth(cardExpirationMonth);
            cardResponseItem.setPaymentSystem(cardPaymentSystem);
            cardResponseItem.setExpired(false);
            responses.add(cardResponseItem);
        }

        for (String cardId : expiredCardBalanceIds) {
            GetBoundPaymentMethodsResponse cardResponseItem = new GetBoundPaymentMethodsResponse();
            cardResponseItem.setPaymentMethod("card");
            cardResponseItem.setPaymentMethodId(cardId);
            cardResponseItem.setCurrency("RUB");
            cardResponseItem.setAccount(cardAccount);
            cardResponseItem.setHolder(cardHolder);
            cardResponseItem.setExpirationYear(cardExpirationYear);
            cardResponseItem.setExpirationMonth(cardExpirationMonth);
            cardResponseItem.setPaymentSystem(cardPaymentSystem);
            cardResponseItem.setExpired(true);
            responses.add(cardResponseItem);
        }

        GetBoundPaymentMethodsResponse[] mockResponses = new GetBoundPaymentMethodsResponse[responses.size()];
        responses.toArray(mockResponses);

        Mockito.when(balanceClientStub.getBalanceClientMock().getBoundPaymentMethods(Mockito.any())).
                thenReturn(mockResponses);
    }

    private void validateCardBalanceData(PaymentCardPojo paymentCard) {
        Assert.equals(paymentCard.getAccount(), "1234****5678");
        Assert.equals(paymentCard.getPaymentSystem(), "MasterCard");
        Assert.equals(paymentCard.getCardHolder(), "holder");
        Assert.equals(paymentCard.getExpirationYear(), "2030");
        Assert.equals(paymentCard.getExpirationMonth(), "06");
    }

    private CardEntity createCard(PassportUid uid, CardPurpose purpose,
                                  String cardId, CardStatus status) {
        return paymentFactory.insertCard(uid, purpose,
                c -> c.status(status).externalId(cardId));
    }

    private void mockBalanceGetCardBindingURL(String transactionId) {
        balanceClientStub.turnOnMockitoForMethod("getCardBindingURL");

        GetCardBindingURLResponse mockResponse = new GetCardBindingURLResponse()
                .withBindingUrl("binding_url").withPurchaseToken(transactionId);

        Mockito.when(balanceClientStub.getBalanceClientMock().getCardBindingURL(Mockito.any()))
                .thenReturn(mockResponse);
    }

    private void validateTrustCardBinding(String transactionId) {
        Option<TrustCardBinding> binding = trustCardBindingDao.findByTransactionId(transactionId);
        Assert.assertTrue(binding.isPresent());
        Assert.equals(transactionId, binding.get().getTransactionId().get());
    }

    private TrustCardBinding insertTrustCardBinding(PassportUid uid,
                                                    Option<String> transactionId,
                                                    CardBindingStatus status) {
        return trustCardBindingDao.insert(TrustCardBindingDao.InsertData.builder()
                .operatorUid(uid)
                .transactionId(transactionId)
                .status(status).build());
    }

    private void mockCheckBinding(String cardExternalId) {
        balanceClientStub.turnOnMockitoForMethod("checkBinding");
        CheckBindingResponse mockResponse = new CheckBindingResponse().
                withBindingResult("success").withPaymentMethodId(cardExternalId);
        Mockito.when(balanceClientStub.getBalanceClientMock().checkBinding(Mockito.any())).
                thenReturn(mockResponse);
    }

    private void mockCheckBinding(String cardExternalId1, String cardExternalId2) {
        balanceClientStub.turnOnMockitoForMethod("checkBinding");
        CheckBindingResponse mockResponse1 = new CheckBindingResponse().
                withBindingResult("success").withPaymentMethodId(cardExternalId1);
        CheckBindingResponse mockResponse2 = new CheckBindingResponse().
                withBindingResult("success").withPaymentMethodId(cardExternalId2);
        Mockito.when(balanceClientStub.getBalanceClientMock().checkBinding(Mockito.any())).
                thenReturn(mockResponse1, mockResponse2);
    }

    private void mockCheckBindingFailure() {
        balanceClientStub.turnOnMockitoForMethod("checkBinding");
        CheckBindingResponse mockResponse = new CheckBindingResponse().
                withBindingResult("error");
        Mockito.when(balanceClientStub.getBalanceClientMock().checkBinding(Mockito.any())).
                thenReturn(mockResponse);
    }

    private void mockCheckBindingInProgress() {
        balanceClientStub.turnOnMockitoForMethod("checkBinding");
        CheckBindingResponse mockResponse = new CheckBindingResponse().
                withBindingResult("in_progress");
        Mockito.when(balanceClientStub.getBalanceClientMock().checkBinding(Mockito.any())).
                thenReturn(mockResponse);
    }

    @Before
    public void setup() {
        Mockito.reset(directoryClient);
        groupProduct1 = psBillingProductsFactory.createGroupProduct();
        groupProduct2 = psBillingProductsFactory.createGroupProduct();
        psBillingTransactionsFactory.setupMocks();
        balanceClientStub.turnOnMockitoForMethod("getRequestPaymentMethodsForContract");
        featureFlags.getHotFixGroupValidationEnabled().resetValue();
    }

    @Configuration
    public static class BalanceMockConfiguration {

        @Primary
        @Bean
        public OptionalFeaturesManager groupFeaturesService() {
            return Mockito.mock(OptionalFeaturesManager.class);
        }
    }
}
