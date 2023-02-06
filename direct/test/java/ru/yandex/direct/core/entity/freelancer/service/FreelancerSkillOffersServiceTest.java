package ru.yandex.direct.core.entity.freelancer.service;

import java.math.BigDecimal;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkillOffer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkillOfferDuration;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.FreelancerSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class FreelancerSkillOffersServiceTest {
    private static final BigDecimal TEST_SKILL_OFFER_PRICE = BigDecimal.valueOf(65536.0d);
    private static final FreelancerSkillOfferDuration TEST_SKILL_OFFER_DURATION = FreelancerSkillOfferDuration.MONTHLY;
    @Autowired
    FreelancerSkillOffersService freelancerSkillOffersService;
    @Autowired
    FreelancerSteps freelancerSteps;

    @Test
    public void setFreelancerSkillOffer_success() {
        FreelancerInfo freelancerInfo = freelancerSteps.addDefaultFreelancer();
        Long testSkillId = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH.getSkillId();
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        ClientId clientId = ClientId.fromLong(freelancerId);
        FreelancerSkillOffer srcOffer = new FreelancerSkillOffer()
                .withFreelancerId(freelancerId)
                .withSkillId(testSkillId)
                .withPrice(TEST_SKILL_OFFER_PRICE)
                .withDuration(TEST_SKILL_OFFER_DURATION);
        freelancerSkillOffersService.setFreelancerSkillOffer(clientId, singletonList(srcOffer));
        // повторный вызов вне должен создать вторую запись
        freelancerSkillOffersService.setFreelancerSkillOffer(clientId, singletonList(srcOffer));
        SoftAssertions sa = new SoftAssertions();
        List<FreelancerSkillOffer> freelancerSkillsOffers =
                freelancerSkillOffersService.getFreelancerSkillsOffers(singletonList(freelancerId));
        long skillOffersCount = StreamEx.of(freelancerSkillsOffers)
                .filter(l -> l.getFreelancerId().equals(freelancerId) && l.getSkillId().equals(testSkillId))
                .count();
        sa.assertThat(skillOffersCount).isEqualTo(1);
        FreelancerSkillOffer resultOffer = StreamEx.of(freelancerSkillsOffers)
                .findAny(l -> l.getFreelancerId().equals(freelancerId) && l.getSkillId().equals(testSkillId))
                .orElse(null);
        checkNotNull(resultOffer, "Почему-то вообще не завелась услуга для фрилансера.");
        //Два BigDecimal с одним значением но разной точностью считаются разными, поэтому сравнивать их "в лоб" после записи-чтения в БД - дело неблагодарное. Проще конвертировать в double.
        sa.assertThat(resultOffer.getPrice().doubleValue()).isEqualTo(srcOffer.getPrice().doubleValue());
        sa.assertThat(resultOffer.getDuration()).isEqualTo(srcOffer.getDuration());
        sa.assertAll();
    }

    @Test
    public void deleteFreelancerSkillsOffer_success() {
        FreelancerInfo freelancerInfo = freelancerSteps.addDefaultFreelancer();
        Long testSkillId = FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH.getSkillId();
        Long freelancerId = freelancerInfo.getFreelancer().getFreelancerId();
        ClientId clientId = ClientId.fromLong(freelancerId);
        FreelancerSkillOffer srcOffer = new FreelancerSkillOffer()
                .withFreelancerId(freelancerId)
                .withSkillId(testSkillId)
                .withPrice(TEST_SKILL_OFFER_PRICE)
                .withDuration(TEST_SKILL_OFFER_DURATION);
        freelancerSkillOffersService.setFreelancerSkillOffer(clientId, singletonList(srcOffer));
        long startOffersCount = getSkillOffersCount(freelancerId, testSkillId);
        checkState(startOffersCount == 1, "Почему-то не завелась услуга для фрилансера.");
        freelancerSkillOffersService.deleteFreelancerSkillsOffer(clientId, singletonList(testSkillId));
        long resultOffersCount = getSkillOffersCount(freelancerId, testSkillId);
        assertThat(resultOffersCount).isEqualTo(0);
    }

    private long getSkillOffersCount(Long freelancerId, Long testSkillId) {
        List<FreelancerSkillOffer> freelancerSkillsOffers =
                freelancerSkillOffersService.getFreelancerSkillsOffers(singletonList(freelancerId));
        return StreamEx.of(freelancerSkillsOffers)
                .filter(l -> l.getFreelancerId().equals(freelancerId) && l.getSkillId().equals(testSkillId))
                .count();
    }
}
