package ru.yandex.market.global.checkout.factory;

import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.global.checkout.domain.referral.ReferralRepository;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.tables.pojos.Referral;

@Transactional
public class TestReferralFactory {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestReferralFactory.class).build();

    @Autowired
    private ReferralRepository referralRepository;

    public Referral createReferral() {
        return createReferral(Function.identity());
    }

    public Referral createReferral(Function<Referral, Referral> setupReferral) {
        Referral referral = setupReferral.apply(RANDOM.nextObject(Referral.class));
        referralRepository.insert(referral);
        return referral;
    }

}
