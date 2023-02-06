package ru.yandex.chemodan.app.psbilling.core.dao.promocodes.impl;

import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PsBillingPromoCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.SafePromoCode;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class PromoCodeDaoImplTest extends PsBillingPromoCoreTest {

    @Autowired
    private PromoCodeDao promoCodeDao;

    @Before
    public void setup() {
        DateUtils.freezeTime();
    }

    @Test
    public void blockCodeDirectCase() {
        PromoCodeEntity promoCode = psBillingPromoFactory.createPromoCode(x -> x
                        .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                        .promoCodeType(PromoCodeType.B2B)
                        .promoTemplateId(Option.of(psBillingPromoFactory.createPromo(Function.identityF()).getId()))
        );

        Assert.equals(PromoCodeStatus.ACTIVE, promoCode.getStatus());
        Assert.equals(Instant.now(), promoCode.getUpdatedAt());
        Assert.equals(Instant.now(), promoCode.getStatusUpdatedAt());
        Assert.none(promoCode.getStatusReason());

        DateUtils.freezeTime(Instant.now().plus(Duration.standardDays(1)));

        String reason = UUID.randomUUID().toString();
        PromoCodeEntity promoCodeEntity = promoCodeDao.blockCode(promoCode.getCode(), reason);

        Assert.equals(promoCode.getCode(), promoCodeEntity.getCode());
        Assert.equals(PromoCodeStatus.BLOCKED, promoCodeEntity.getStatus());
        Assert.equals(Instant.now(), promoCodeEntity.getUpdatedAt());
        Assert.equals(Instant.now(), promoCodeEntity.getStatusUpdatedAt());
        Assert.notEquals(promoCode.getUpdatedAt(), promoCodeEntity.getUpdatedAt());
        Assert.notEquals(promoCode.getStatusUpdatedAt(), promoCodeEntity.getStatusUpdatedAt());
        Assert.some(reason, promoCodeEntity.getStatusReason());
    }

    @Test
    public void blockCodeFailIfNotFound() {
        SafePromoCode promoCode = SafePromoCode.cons(UUID.randomUUID().toString());
        String reason = UUID.randomUUID().toString();

        Assert.assertThrows(() -> promoCodeDao.blockCode(promoCode, reason), EmptyResultDataAccessException.class);
    }

    @Test
    public void decrementFailOnExhaust() {
        PromoCodeEntity promoCode = psBillingPromoFactory.createPromoCode(x -> x
                .codes(Cf.list(SafePromoCode.cons(UUID.randomUUID().toString())))
                .promoCodeType(PromoCodeType.B2B)
                .promoTemplateId(Option.of(psBillingPromoFactory.createPromo(Function.identityF()).getId()))
                .remainingActivations(Option.of(1))
        );

        promoCodeDao.decrementRemainingActivations(promoCode.getCode());

        Assert.assertThrows(
                () -> promoCodeDao.decrementRemainingActivations(promoCode.getCode()),
                DataIntegrityViolationException.class
        );
    }
}
