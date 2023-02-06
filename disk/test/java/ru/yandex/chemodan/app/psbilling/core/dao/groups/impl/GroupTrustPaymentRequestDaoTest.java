package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.Money;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentGroupServiceInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.inside.passport.PassportUid;

public class GroupTrustPaymentRequestDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupTrustPaymentRequestDao dao;

    @Autowired
    private CardDao cardDao;

    @Test
    public void money_backCompatibility() {
        Long clientId = 123L;
        GroupTrustPaymentRequest inserted = dao.insert(createBuilder(clientId).build());

        jdbcTemplate.update("update group_trust_payment_requests " +
                "set amount = null, currency = null " +
                "where id = ?", inserted.getId());
        GroupTrustPaymentRequest data = dao.findById(inserted.getId());
        AssertHelper.assertEquals(data.getMoney(), Option.empty());
    }
    @Test
    public void transactionEmpty() {
        val insertData = createBuilder(123L)
                .transactionId(Option.empty())
                .build();

        GroupTrustPaymentRequest inserted = dao.insert(insertData);

        ru.yandex.misc.test.Assert.none(inserted.getTransactionId());
    }

    @Test
    public void create_mandatoryFields() {
        Long clientId = 123L;
        GroupTrustPaymentRequest inserted = dao.insert(createBuilder(clientId).build());
        validateInsertedData(inserted, clientId);
    }

    @Test
    public void create_allFields() {
        CardEntity card = paymentFactory.insertCard(PassportUid.cons(12345678), "card-id", CardPurpose.B2B_PRIMARY);

        Long clientId = 123L;
        Double paymentCoefficient = 1.0;
        Group group = psBillingGroupsFactory.createGroup(clientId);

        GroupTrustPaymentGroupServiceInfo.Product product =
                new GroupTrustPaymentGroupServiceInfo.Product(psBillingProductsFactory.createGroupProduct().getCode());
        GroupTrustPaymentGroupServiceInfo.ActivateServiceInfo serviceInfo =
                new GroupTrustPaymentGroupServiceInfo.ActivateServiceInfo(group.getId(),
                        new GroupTrustPaymentGroupServiceInfo.Product[]{product});
        GroupTrustPaymentGroupServiceInfo servicesInfo =
                new GroupTrustPaymentGroupServiceInfo(new GroupTrustPaymentGroupServiceInfo.ActivateServiceInfo[]{serviceInfo});

        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder = createBuilder(clientId);
        Money money = new Money(BigDecimal.ONE, rub);
        Option<String> error = Option.of("error");
        builder.paymentCoefficient(Option.of(paymentCoefficient))
                .cardId(Option.of(card.getId()))
                .groupServicesInfo(Option.of(servicesInfo))
                .error(error)
                .money(money);

        GroupTrustPaymentRequest inserted = dao.insert(builder.build());
        validateInsertedData(inserted, clientId);

        AssertHelper.assertEquals(inserted.getGroupServicesInfo().get(), servicesInfo);
        AssertHelper.assertEquals(inserted.getCardId().get(), card.getId());
        AssertHelper.assertEquals(inserted.getPaymentPeriodCoefficient().get(), paymentCoefficient);
        AssertHelper.assertEquals(inserted.getError(), error);
    }

    @Test
    public void findByRequestId() {
        Long clientId = 123L;
        Group group = psBillingGroupsFactory.createGroup(clientId);

        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder = createBuilder(clientId);
        builder.requestId("r1");

        GroupTrustPaymentRequest inserted = dao.insert(builder.requestId("r1").build());
        dao.insert(builder.requestId("r2").build());
        dao.insert(builder.requestId("r3").build());

        Option<GroupTrustPaymentRequest> found = dao.findByRequestId("r1");
        Assert.assertTrue(found.isPresent());
        Assert.assertEquals(inserted, found.get());
    }

    @Test
    public void updateStatusIfInInit() {
        Long clientId = 123L;
        Group group = psBillingGroupsFactory.createGroup(clientId);

        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder = createBuilder(clientId);

        GroupTrustPaymentRequest init =
                dao.insert(builder.requestId("r1").status(PaymentRequestStatus.INIT).build());
        GroupTrustPaymentRequest success =
                dao.insert(builder.requestId("r2").status(PaymentRequestStatus.SUCCESS).build());

        Option<GroupTrustPaymentRequest> initToSuccess =
                dao.updateStatusIfInInit(init.withStatus(PaymentRequestStatus.SUCCESS));
        Assert.assertTrue(initToSuccess.isPresent());
        Assert.assertEquals(PaymentRequestStatus.SUCCESS, initToSuccess.get().getStatus());

        Option<GroupTrustPaymentRequest> successToCancel =
                dao.updateStatusIfInInit(success.withStatus(PaymentRequestStatus.CANCELLED));
        Assert.assertFalse(successToCancel.isPresent());
    }

    @Test
    public void findInitPaymentsOlderThan() {
        Long clientId = 123L;
        Group group = psBillingGroupsFactory.createGroup(clientId);

        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder = createBuilder(clientId);

        GroupTrustPaymentRequest initOld =
                dao.insert(builder.requestId("r1").status(PaymentRequestStatus.INIT).build());
        DateUtils.shiftTime(Duration.standardHours(1));
        dao.insert(builder.requestId("r2").status(PaymentRequestStatus.INIT).build());


        ListF<GroupTrustPaymentRequest> found = dao.findInitPaymentsOlderThan(Instant.now(), 1000,
                Option.empty());
        Assert.assertEquals(1, found.length());
        Assert.assertEquals(initOld, found.get(0));
    }

    @Test
    public void findRecentPayments() {
        Long clientId1 = 123L;
        Long clientId2 = 321L;

        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder1 = createBuilder(clientId1);
        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder2 = createBuilder(clientId2);
        dao.insert(builder1.requestId("r1_1").status(PaymentRequestStatus.INIT).build());
        dao.insert(builder2.requestId("r2_1").status(PaymentRequestStatus.INIT).build());

        DateUtils.shiftTime(Duration.standardHours(1));
        Instant time = Instant.now();
        dao.insert(builder1.requestId("r1_2").status(PaymentRequestStatus.SUCCESS).build());


        ListF<GroupTrustPaymentRequest> found = dao.findRecentPayments(clientId1, Option.of(time));
        Assert.assertEquals(1, found.length());
        Assert.assertEquals("r1_2", found.get(0).getRequestId());

        found = dao.findRecentPayments(clientId2, Option.empty());
        Assert.assertEquals(1, found.length());
        Assert.assertEquals("r2_1", found.get(0).getRequestId());
    }

    @Test
    public void updateTransactionIdIfNull(){
        GroupTrustPaymentRequestDao.InsertData insertData = createBuilder(123L)
                .transactionId(Option.empty())
                .build();

        GroupTrustPaymentRequest inserted = dao.insert(insertData);

        ru.yandex.misc.test.Assert.none(inserted.getTransactionId());

        String transactionId = "transactionId";

        Option<GroupTrustPaymentRequest> afterUpdate = dao.updateTransactionIdIfNull(inserted.getId(),
                transactionId);

        ru.yandex.misc.test.Assert.some(afterUpdate);
        ru.yandex.misc.test.Assert.some(transactionId, afterUpdate.get().getTransactionId());

        Option<GroupTrustPaymentRequest> emptyEntity = dao.updateTransactionIdIfNull(inserted.getId(),
                transactionId + "wrong");

        ru.yandex.misc.test.Assert.none(emptyEntity);
        ru.yandex.misc.test.Assert.some(transactionId, dao.findById(inserted.getId()).getTransactionId());
    }

    @Test
    public void getRecentAutoPaymentsSum() {
        getRecentAutoPaymentsSumCore(false);
    }

    @Test
    public void getRecentAutoResurrectionPaymentsSum() {
        getRecentAutoPaymentsSumCore(true);
    }

    private void getRecentAutoPaymentsSumCore(boolean createGroupServiceInfo) {
        Long clientId1 = 123L;
        Instant oldTime = Instant.now();
        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder = createBuilder(clientId1)
                .paymentInitiationType(PaymentInitiationType.AUTO);

        if (createGroupServiceInfo) {
            builder.groupServicesInfo(Option.of(new GroupTrustPaymentGroupServiceInfo()));
        }

        // spoilers
        dao.insert(createBuilder(clientId1).requestId("old_user_init").status(PaymentRequestStatus.INIT)
                .paymentInitiationType(PaymentInitiationType.USER).money(Money.rub(1)).build());
        dao.insert(createBuilder(clientId1).requestId("old_repayment_success")
                .paymentInitiationType(PaymentInitiationType.AUTO)
                .groupServicesInfo(createGroupServiceInfo ? Option.empty() :
                        Option.of(new GroupTrustPaymentGroupServiceInfo()))
                .status(PaymentRequestStatus.SUCCESS).money(Money.rub(1)).build());

        //data to find
        GroupTrustPaymentRequest old_init =
                dao.insert(builder.requestId("old_init").status(PaymentRequestStatus.INIT).money(Money.rub(1)).build());
        dao.insert(builder.requestId("old_success").status(PaymentRequestStatus.SUCCESS).money(Money.rub(3)).build());

        DateUtils.shiftTime(Duration.standardHours(1));
        dao.insert(builder.requestId("now_canceled").status(PaymentRequestStatus.CANCELLED).money(Money.rub(5)).build());
        dao.insert(builder.requestId("now_success").status(PaymentRequestStatus.SUCCESS).money(Money.rub(7)).build());

        BiFunction<Instant, PaymentRequestStatus, BigDecimal> testMethod =
                createGroupServiceInfo ? dao::getRecentResurrectPaymentsSum : dao::getRecentAutoPaymentsSum;

        AssertHelper.assertEquals(testMethod.apply(oldTime.minus(1), PaymentRequestStatus.SUCCESS), 3 + 7);
        AssertHelper.assertEquals(testMethod.apply(oldTime.minus(1), PaymentRequestStatus.INIT), 1);
        AssertHelper.assertEquals(testMethod.apply(oldTime.minus(1), PaymentRequestStatus.CANCELLED), 5);

        AssertHelper.assertEquals(testMethod.apply(oldTime, PaymentRequestStatus.SUCCESS), 7);
        AssertHelper.assertEquals(testMethod.apply(oldTime, PaymentRequestStatus.INIT), 0);
        AssertHelper.assertEquals(testMethod.apply(oldTime, PaymentRequestStatus.CANCELLED), 5);

        AssertHelper.assertEquals(testMethod.apply(Instant.now().plus(1), PaymentRequestStatus.SUCCESS), 0);
        AssertHelper.assertEquals(testMethod.apply(Instant.now().plus(1), PaymentRequestStatus.INIT), 0);
        AssertHelper.assertEquals(testMethod.apply(Instant.now().plus(1), PaymentRequestStatus.CANCELLED)
                , 0);
    }

    private void validateInsertedData(GroupTrustPaymentRequest inserted, Long clientId) {
        Assert.assertEquals(clientId, inserted.getClientId());
        Assert.assertEquals("requestId", inserted.getRequestId());
        Assert.assertEquals("uid", inserted.getOperatorUid());
        Assert.assertEquals(PaymentRequestStatus.SUCCESS, inserted.getStatus());
        Assert.assertEquals(Option.of("transactionId"), inserted.getTransactionId());
        Assert.assertEquals(PaymentInitiationType.USER, inserted.getPaymentInitiationType());
        AssertHelper.assertEquals(inserted.getMoney().get(), new Money(BigDecimal.ONE, rub));
    }

    private GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder createBuilder(Long clientId) {
        return GroupTrustPaymentRequestDao.InsertData.builder()
                .requestId("requestId")
                .operatorUid("uid")
                .status(PaymentRequestStatus.SUCCESS)
                .transactionId(Option.of("transactionId"))
                .paymentInitiationType(PaymentInitiationType.USER)
                .money(new Money(BigDecimal.ONE, rub))
                .clientId(clientId);
    }
}

