package ru.yandex.chemodan.app.psbilling.core.dao.cards.impl;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.inside.passport.PassportUid;

public class CardDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    private CardDao dao;

    @Test
    public void create() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardPurpose purpose = CardPurpose.B2B_PRIMARY;
        CardDao.InsertData data = createBuilder(uid, externalId, purpose).build();

        CardEntity card = dao.insert(data);

        Assert.assertEquals(card.getUid(), uid);
        Assert.assertEquals(card.getExternalId(), externalId);
        Assert.assertEquals(card.getPurpose(), purpose);
        Assert.assertEquals(card.getStatus(), data.getStatus());
    }

    @Test
    public void insertOrUpdateStatus() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardPurpose purpose = CardPurpose.B2B_PRIMARY;
        CardDao.InsertData.InsertDataBuilder builder = createBuilder(uid, externalId, purpose);

        CardEntity card = dao.insert(builder.status(CardStatus.DISABLED).build());
        Assert.assertEquals(card.getStatus(), CardStatus.DISABLED);

        dao.insertOrUpdateStatus(builder.status(CardStatus.ACTIVE).build());

        CardEntity updatedCard = dao.findById(card.getId());
        Assert.assertEquals(updatedCard.getStatus(), CardStatus.ACTIVE);
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_noSuchCard() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2B, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_expiredCard() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        paymentFactory.insertCard(uid, CardPurpose.B2B,
                c -> c.externalId(externalId).status(CardStatus.EXPIRED));

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2B, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_disabledPrimaryCard_noOtherPrimary() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(externalId).status(CardStatus.DISABLED));

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2B_PRIMARY, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_disabledPrimaryCard_hasOtherActivePrimary() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(externalId).status(CardStatus.DISABLED));
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(UUID.randomUUID().toString())
                        .status(CardStatus.ACTIVE));

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2B, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_disabledPrimaryCard_hasOtherDisabledPrimary() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(externalId).status(CardStatus.DISABLED));
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(UUID.randomUUID().toString())
                        .status(CardStatus.DISABLED));

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2B_PRIMARY, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_disabledB2cCard_hasOtherActivePrimary() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        paymentFactory.insertCard(uid, CardPurpose.B2C,
                c -> c.externalId(externalId).status(CardStatus.DISABLED));
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(UUID.randomUUID().toString())
                        .status(CardStatus.ACTIVE));

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2C, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void insertOrUpdateStatusWithCheckingOtherPrimary_alreadyActive() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                c -> c.externalId(externalId).status(CardStatus.ACTIVE));

        CardEntity card = dao.insertOrUpdateStatusWithCheckingOtherPrimary(
                CardDao.InsertData.builder()
                        .uid(uid)
                        .purpose(CardPurpose.B2B)
                        .externalId(externalId)
                        .status(CardStatus.ACTIVE).build());

        Assert.assertEquals(uid, card.getUid());
        Assert.assertEquals(CardPurpose.B2B_PRIMARY, card.getPurpose());
        Assert.assertEquals(CardStatus.ACTIVE, card.getStatus());
        Assert.assertEquals(externalId, card.getExternalId());
    }

    @Test
    public void findByExternalId() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardPurpose purpose = CardPurpose.B2B_PRIMARY;
        String externalId2 = UUID.randomUUID().toString();
        CardPurpose purpose2 = CardPurpose.B2B;
        CardDao.InsertData data = createBuilder(uid, externalId, purpose).build();
        dao.insert(data);
        CardDao.InsertData data2 = createBuilder(uid, externalId2, purpose2).build();
        dao.insert(data2);

        Option<CardEntity> card = dao.findByExternalId(uid, externalId);

        Assert.assertTrue(card.isPresent());
        Assert.assertEquals(card.get().getUid(), uid);
        Assert.assertEquals(card.get().getPurpose(), purpose);
        Assert.assertEquals(card.get().getExternalId(), externalId);
    }

    @Test
    public void findByPurpose() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        String externalId2 = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId, CardPurpose.B2B_PRIMARY).build();
        dao.insert(data);
        CardDao.InsertData data2 = createBuilder(uid, externalId2, CardPurpose.B2B).build();
        dao.insert(data2);

        ListF<CardEntity> cards = dao.findByPurpose(uid, CardPurpose.B2B);

        Assert.assertEquals(cards.size(), 1);
        CardEntity card = cards.first();
        Assert.assertEquals(card.getUid(), uid);
        Assert.assertEquals(card.getPurpose(), CardPurpose.B2B);
        Assert.assertEquals(card.getExternalId(), externalId2);
    }

    @Test
    public void findB2BPrimary() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = "card-x123";
        paymentFactory.insertCard(uid, externalId, CardPurpose.B2B_PRIMARY);
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY, x -> x.status(CardStatus.DISABLED));
        paymentFactory.insertCard(uid, CardPurpose.B2B, x -> x);

        Option<CardEntity> card = dao.findB2BPrimary(uid);

        Assert.assertTrue(card.isPresent());
        AssertHelper.assertEquals(card.get().getStatus(), CardStatus.ACTIVE);
        AssertHelper.assertEquals(card.get().getUid(), uid);
        AssertHelper.assertEquals(card.get().getPurpose(), CardPurpose.B2B_PRIMARY);
        AssertHelper.assertEquals(card.get().getExternalId(), externalId);
    }

    @Test
    public void findCardsByUid() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        String externalId2 = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId, CardPurpose.B2B_PRIMARY).build();
        dao.insert(data);
        CardDao.InsertData data2 = createBuilder(uid, externalId2, CardPurpose.B2B).build();
        dao.insert(data2);
        CardDao.InsertData dataOtherUid = createBuilder(PassportUid.cons(456), externalId2, CardPurpose.B2B).build();
        dao.insert(dataOtherUid);

        ListF<CardEntity> card = dao.findCardsByUid(uid);

        Assert.assertEquals(card.size(), 2);
        Assert.assertEquals(card.get(0).getUid(), uid);
        Assert.assertEquals(card.get(1).getUid(), uid);
    }

    @Test
    public void updateStatus() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId, CardPurpose.B2B_PRIMARY).build();
        CardEntity card = dao.insert(data);

        dao.updateStatus(card.getId(), CardStatus.DISABLED);

        Option<CardEntity> updatedCard = dao.findByIdO(card.getId());
        Assert.assertTrue(updatedCard.isPresent());
        Assert.assertEquals(updatedCard.get().getStatus(), CardStatus.DISABLED);
    }

    @Test
    public void setB2BPrimaryCard() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        String externalId2 = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId, CardPurpose.B2B_PRIMARY).build();
        CardEntity card = dao.insert(data);
        CardDao.InsertData data2 = createBuilder(uid, externalId2, CardPurpose.B2B).build();
        CardEntity card2 = dao.insert(data2);

        dao.setB2BPrimaryCard(uid, externalId2);

        Option<CardEntity> updatedCard = dao.findByIdO(card.getId());
        Option<CardEntity> updatedCard2 = dao.findByIdO(card2.getId());
        Assert.assertTrue(updatedCard.isPresent());
        Assert.assertEquals(updatedCard.get().getPurpose(), CardPurpose.B2B);
        Assert.assertTrue(updatedCard2.isPresent());
        Assert.assertEquals(updatedCard2.get().getPurpose(), CardPurpose.B2B_PRIMARY);

        Option<CardEntity> b2bPrimaryCard = dao.findB2BPrimary(uid);
        Assert.assertEquals(b2bPrimaryCard, updatedCard2);
    }

    @Test
    public void setB2BPrimaryCard_disabledCard() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        String externalId2 = UUID.randomUUID().toString();

        CardEntity card = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.externalId(externalId));
        CardEntity card2 = paymentFactory.insertCard(uid, CardPurpose.B2B,
                x -> x.status(CardStatus.DISABLED).externalId(externalId2));

        dao.setB2BPrimaryCard(uid, externalId2);

        Option<CardEntity> updatedCard = dao.findByIdO(card.getId());
        Option<CardEntity> updatedCard2 = dao.findByIdO(card2.getId());
        Assert.assertTrue(updatedCard.isPresent());
        Assert.assertEquals(updatedCard.get().getPurpose(), CardPurpose.B2B);
        Assert.assertTrue(updatedCard2.isPresent());
        Assert.assertEquals(updatedCard2.get().getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.assertEquals(updatedCard2.get().getStatus(), CardStatus.ACTIVE);

        Option<CardEntity> b2bPrimaryCard = dao.findB2BPrimary(uid);
        Assert.assertEquals(b2bPrimaryCard, updatedCard2);
    }

    @Test
    public void setB2BPrimaryCard_notExistingCard() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        String externalId2 = UUID.randomUUID().toString();

        CardDao.InsertData data = createBuilder(uid, externalId, CardPurpose.B2B_PRIMARY).build();
        CardEntity card1 = dao.insert(data);

        dao.setB2BPrimaryCard(uid, externalId2);

        Option<CardEntity> updatedCard1 = dao.findByIdO(card1.getId());
        Assert.assertTrue(updatedCard1.isPresent());
        Assert.assertEquals(updatedCard1.get().getPurpose(), CardPurpose.B2B);

        Option<CardEntity> card2 = dao.findByExternalId(uid, externalId2);
        Assert.assertTrue(card2.isPresent());
        Assert.assertEquals(card2.get().getPurpose(), CardPurpose.B2B_PRIMARY);

        Option<CardEntity> b2bPrimaryCard = dao.findB2BPrimary(uid);
        Assert.assertEquals(b2bPrimaryCard, card2);
    }

    @Test
    public void updatePurpose_b2bPrimary() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId,
                CardPurpose.B2B).build();
        dao.insert(data);

        Option<CardEntity> updatedCard = dao.updatePurpose(uid, externalId,
                CardPurpose.B2B_PRIMARY);

        Assert.assertTrue(updatedCard.isPresent());
        Assert.assertEquals(updatedCard.get().getPurpose(), CardPurpose.B2B_PRIMARY);
    }

    @Test
    public void updatePurpose_b2b() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId,
                CardPurpose.B2B_PRIMARY).build();
        dao.insert(data);

        Option<CardEntity> updatedCard = dao.updatePurpose(uid, externalId,
                CardPurpose.B2B);

        Assert.assertTrue(updatedCard.isPresent());
        Assert.assertEquals(updatedCard.get().getPurpose(), CardPurpose.B2B);
    }

    @Test
    public void updateStatusAndPurpose() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();
        CardDao.InsertData data = createBuilder(uid, externalId, CardPurpose.B2B_PRIMARY)
                .status(CardStatus.DISABLED).build();
        CardEntity card = dao.insert(data);

        dao.updateStatusAndPurpose(card.getId(), CardStatus.ACTIVE, CardPurpose.B2B);

        Option<CardEntity> updatedCard = dao.findByIdO(card.getId());
        Assert.assertTrue(updatedCard.isPresent());
        Assert.assertEquals(updatedCard.get().getPurpose(), CardPurpose.B2B);
        Assert.assertEquals(updatedCard.get().getStatus(), CardStatus.ACTIVE);
    }

    @Test
    public void updateStatusOfMultipleCards() {
        PassportUid uid = PassportUid.cons(123);
        CardEntity card1 = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.status(CardStatus.ACTIVE));
        CardEntity card2 = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.status(CardStatus.DISABLED));
        CardEntity card3 = paymentFactory.insertCard(uid, CardPurpose.B2B,
                x -> x.status(CardStatus.EXPIRED));
        CardEntity card4 = paymentFactory.insertCard(uid, CardPurpose.B2B,
                x -> x.status(CardStatus.ACTIVE));

        dao.updateStatus(Cf.list(card1.getExternalId(), card2.getExternalId(),
                card3.getExternalId()), CardStatus.DISABLED);

        Assert.assertEquals(dao.findById(card1.getId()).getStatus(), CardStatus.DISABLED);
        Assert.assertEquals(dao.findById(card2.getId()).getStatus(), CardStatus.DISABLED);
        Assert.assertEquals(dao.findById(card3.getId()).getStatus(), CardStatus.DISABLED);
        Assert.assertEquals(dao.findById(card4.getId()).getStatus(), CardStatus.ACTIVE);
    }

    @Test
    public void updatePurpose_b2bNotExistingCard() {
        PassportUid uid = PassportUid.cons(123);
        String externalId = UUID.randomUUID().toString();

        Option<CardEntity> updatedCard = dao.updatePurpose(uid, externalId,
                CardPurpose.B2B);

        Assert.assertTrue(updatedCard.isEmpty());
    }

    private CardDao.InsertData.InsertDataBuilder createBuilder(PassportUid uid, String externalId,
                                                               CardPurpose purpose) {
        return CardDao.InsertData.builder()
                .uid(uid)
                .purpose(purpose)
                .externalId(externalId)
                .status(CardStatus.ACTIVE);
    }
}

