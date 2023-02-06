package ru.yandex.chemodan.app.psbilling.core.dao.cards.impl;

import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.TrustCardBindingDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardBindingStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.TrustCardBinding;
import ru.yandex.chemodan.app.psbilling.core.util.BatchFetchingUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;

public class TrustCardBindingDaoTest extends AbstractPsBillingCoreTest {
    private static final Logger logger = LoggerFactory.getLogger(TrustCardBindingDaoTest.class);

    @Autowired
    private TrustCardBindingDao dao;
    @Autowired
    protected PaymentFactory paymentFactory;

    @Test
    public void insertAndSetTransactionId() {
        PassportUid uid = PassportUid.cons(123);
        String transactionId = UUID.randomUUID().toString();
        TrustCardBinding binding = insertBinding(uid, CardBindingStatus.INIT,
                Option.empty());

        dao.setTransactionId(binding.getId(), transactionId);

        binding = dao.findById(binding.getId());
        Assert.assertEquals(binding.getOperatorUid(), uid);
        Assert.assertEquals(binding.getStatus(), CardBindingStatus.INIT);
        Assert.assertTrue(binding.getTransactionId().isPresent());
        Assert.assertEquals(binding.getTransactionId().get(), transactionId);
        Assert.assertTrue(binding.getCardId().isEmpty());
        Assert.assertTrue(binding.getError().isEmpty());
    }

    @Test
    public void updateStatus_statusInit() {
        PassportUid uid = PassportUid.cons(123);
        Option<String> error = Option.of("error");
        TrustCardBinding binding = insertBinding(uid, CardBindingStatus.INIT,
                Option.empty());

        CardEntity card = paymentFactory.insertCard(uid, UUID.randomUUID().toString(),
                CardPurpose.B2B_PRIMARY);

        dao.updateStatusIfInNotSuccess(binding.withCardId(Option.of(card.getId()))
                .withStatus(CardBindingStatus.ERROR).withError(error));

        binding = dao.findById(binding.getId());
        Assert.assertEquals(binding.getOperatorUid(), uid);
        Assert.assertEquals(binding.getStatus(), CardBindingStatus.ERROR);
        Assert.assertEquals(binding.getCardId().get(), card.getId());
        Assert.assertEquals(binding.getError(), error);
    }

    @Test
    public void updateStatus_statusError() {
        PassportUid uid = PassportUid.cons(123);
        Option<String> error = Option.of("error");
        TrustCardBinding binding = insertBinding(uid, CardBindingStatus.ERROR,
                Option.empty());

        CardEntity card = paymentFactory.insertCard(uid, UUID.randomUUID().toString(),
                CardPurpose.B2B_PRIMARY);

        dao.updateStatusIfInNotSuccess(binding.withCardId(Option.of(card.getId()))
                .withStatus(CardBindingStatus.ERROR).withError(error));

        binding = dao.findById(binding.getId());
        Assert.assertEquals(binding.getOperatorUid(), uid);
        Assert.assertEquals(binding.getStatus(), CardBindingStatus.ERROR);
        Assert.assertEquals(binding.getCardId().get(), card.getId());
        Assert.assertEquals(binding.getError(), error);
    }

    @Test
    public void updateStatus_statusSuccess() {
        PassportUid uid = PassportUid.cons(123);
        Option<UUID> cardId = Option.of(UUID.randomUUID());
        Option<String> error = Option.of("error");
        TrustCardBinding binding = insertBinding(uid, CardBindingStatus.SUCCESS,
                Option.empty());

        dao.updateStatusIfInNotSuccess(binding.withCardId(cardId)
                .withStatus(CardBindingStatus.ERROR).withError(error));

        binding = dao.findById(binding.getId());
        Assert.assertEquals(binding.getOperatorUid(), uid);
        Assert.assertEquals(binding.getStatus(), CardBindingStatus.SUCCESS);
        Assert.assertTrue(binding.getCardId().isEmpty());
        Assert.assertTrue(binding.getError().isEmpty());
    }

    @Test
    public void findByTransactionId() {
        PassportUid uid = PassportUid.cons(123);
        Option<String> transactionId = Option.of(UUID.randomUUID().toString());
        insertBinding(uid, CardBindingStatus.SUCCESS, transactionId);

        Option<TrustCardBinding> binding = dao.findByTransactionId(transactionId.get());

        Assert.assertTrue(binding.isPresent());
        Assert.assertEquals(binding.get().getOperatorUid(), uid);
        Assert.assertEquals(binding.get().getStatus(), CardBindingStatus.SUCCESS);
        Assert.assertEquals(binding.get().getTransactionId(), transactionId);
    }

    @Test
    public void findInitBindings() {
        TrustCardBinding binding = insertBinding(uid, CardBindingStatus.INIT, Option.empty());

        DateUtils.shiftTime(Duration.standardSeconds(45));
        TrustCardBinding binding2 = insertBinding(uid, CardBindingStatus.INIT, Option.empty());
        insertBinding(uid, CardBindingStatus.SUCCESS, Option.empty());
        insertBinding(uid, CardBindingStatus.ERROR, Option.empty());

        DateUtils.shiftTime(Duration.standardSeconds(45));
        insertBinding(uid, CardBindingStatus.INIT, Option.empty());

        Instant createdBefore = Instant.now().minus(Duration.standardSeconds(30));

        ListF<TrustCardBinding> bindings =
                BatchFetchingUtils.collectBatchedEntities(
                        (batchSize, id) -> dao
                                .findInitBindings(createdBefore, batchSize, id),
                        TrustCardBinding::getId,
                        1,
                        logger
                );

        AssertHelper.assertSize(bindings, 2);
        AssertHelper.assertContains(bindings, binding);
        AssertHelper.assertContains(bindings, binding2);
    }

    @Test
    public void countStaleRecords() {
        insertBinding(PassportUid.cons(2), CardBindingStatus.INIT, Option.empty());
        insertBinding(PassportUid.cons(3), CardBindingStatus.INIT, Option.empty());
        insertBinding(PassportUid.cons(4), CardBindingStatus.ERROR, Option.empty());

        DateUtils.shiftTime(Duration.standardHours(49));
        insertBinding(PassportUid.cons(5), CardBindingStatus.INIT, Option.empty());

        DateUtils.shiftTime(Duration.standardHours(3));
        int staleRecordsCount = dao.countStaleRecords(Duration.standardHours(4));
        Assert.assertEquals(2, staleRecordsCount);
    }

    private TrustCardBinding insertBinding(PassportUid operatorUid,
                                           CardBindingStatus status,
                                           Option<String> transactionId) {
        return dao.insert(TrustCardBindingDao.InsertData.builder()
                .operatorUid(operatorUid)
                .status(status)
                .transactionId(transactionId).build());
    }
}

