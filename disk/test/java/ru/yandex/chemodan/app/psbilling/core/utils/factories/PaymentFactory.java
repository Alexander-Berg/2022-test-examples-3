package ru.yandex.chemodan.app.psbilling.core.utils.factories;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import java.util.function.Function;

import lombok.AllArgsConstructor;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.Money;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.inside.passport.PassportUid;

@AllArgsConstructor
public class PaymentFactory {
    private GroupTrustPaymentRequestDao groupTrustPaymentRequestDao;
    private CardDao cardDao;

    public GroupTrustPaymentRequest insertGroupPayment(
            long clientId, PassportUid uid,
            Function<GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder,
                    GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder> customizer) {
        GroupTrustPaymentRequestDao.InsertData.InsertDataBuilder builder =
                GroupTrustPaymentRequestDao.InsertData.builder()
                        .status(PaymentRequestStatus.SUCCESS)
                        .clientId(clientId)
                        .operatorUid(uid.toString())
                        .requestId(UUID.randomUUID().toString())
                        .transactionId(Option.of(UUID.randomUUID().toString()))
                        .paymentInitiationType(PaymentInitiationType.USER)
                        .money(new Money(BigDecimal.ONE, Currency.getInstance("RUB")));
        builder = customizer.apply(builder);
        return groupTrustPaymentRequestDao.insert(builder.build());
    }

    public GroupTrustPaymentRequest insertGroupPayment(long clientId, PassportUid uid) {
        return insertGroupPayment(clientId, uid, x -> x);
    }

    public CardEntity insertCard(PassportUid uid, CardPurpose purpose) {
        return cardDao.insert(CardDao.InsertData.builder()
                .uid(uid)
                .externalId(UUID.randomUUID().toString())
                .purpose(purpose)
                .status(CardStatus.ACTIVE)
                .build());
    }

    public CardEntity insertCard(PassportUid uid, String cardId, CardPurpose purpose) {
        return cardDao.insert(CardDao.InsertData.builder()
                .uid(uid)
                .externalId(cardId)
                .purpose(purpose)
                .status(CardStatus.ACTIVE)
                .build());
    }

    public CardEntity insertCard(PassportUid uid, CardPurpose purpose,
                                 Function<CardDao.InsertData.InsertDataBuilder,
                                         CardDao.InsertData.InsertDataBuilder> customizer) {
        CardDao.InsertData.InsertDataBuilder builder = CardDao.InsertData.builder()
                .uid(uid)
                .externalId(UUID.randomUUID().toString())
                .purpose(purpose)
                .status(CardStatus.ACTIVE);
        builder = customizer.apply(builder);
        return cardDao.insert(builder.build());
    }
}
