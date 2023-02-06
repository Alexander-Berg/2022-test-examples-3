package ru.yandex.direct.core.entity.freelancer.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkillOffer;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerSkillsRepository;
import ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerSkillOffersValidationService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill.CAMPAIGN_AUDIT;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill.CAMPAIGN_CONDUCTING;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill.METRIKA_SETUP;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill.SETTING_UP_CAMPAIGNS_FROM_SCRATCH;

@RunWith(JUnitParamsRunner.class)
@Description("Тестирование метода #getFreelancerMainSkillsOffer")
public class FreelancerSkillOffersServiceGettingMainSkillTest {
    private final ShardHelper shardHelper = mock(ShardHelper.class);
    private final FreelancerSkillOffersValidationService freelancerSkillOffersValidationService =
            mock(FreelancerSkillOffersValidationService.class);

    @SuppressWarnings("unused")
    private Object getFreelancerMainSkillsOfferParameters() throws IllegalAccessException {
        return Arrays.array(
                createParams("Должен возвращать null, если нет услуг.", null),

                createParams("Может возвращать НЕ приоритетную услугу, если нет приоритетных.", CAMPAIGN_AUDIT,
                        10.0, CAMPAIGN_AUDIT),

                createParams("Должен возвращать приоритетную услугу, невзирая на цену.", CAMPAIGN_CONDUCTING,
                        10.0, CAMPAIGN_AUDIT,
                        20.0, CAMPAIGN_CONDUCTING),

                createParams("Должен возвращать самую дешёвую из приоритетных услуг.", CAMPAIGN_CONDUCTING,
                        30.0, SETTING_UP_CAMPAIGNS_FROM_SCRATCH,
                        20.0, CAMPAIGN_CONDUCTING,
                        10.0, CAMPAIGN_AUDIT),

                createParams(
                        "Должен работать, даже если у фрилансера затесалась одна из отключенных услуг.", METRIKA_SETUP,
                        10.0, METRIKA_SETUP));
    }

    private Object[] createParams(String description, FreelancerSkill expectedSkill, Object... args)
            throws IllegalAccessException {
        if (args.length % 2 != 0) {
            throw new IllegalAccessException("Args must have even number of values.");
        }
        List<FreelancerSkillOffer> freelancerSkillOffers = new ArrayList<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            BigDecimal price = BigDecimal.valueOf((Double) args[i]);
            FreelancerSkill skill = (FreelancerSkill) args[i + 1];
            FreelancerSkillOffer offer = new FreelancerSkillOffer()
                    .withSkillId(skill.getSkillId())
                    .withPrice(price);
            freelancerSkillOffers.add(offer);
        }
        Long skillId = expectedSkill != null ? expectedSkill.getSkillId() : null;
        return new Object[]{description, skillId, freelancerSkillOffers};
    }

    @Test
    @Parameters(method = "getFreelancerMainSkillsOfferParameters")
    public void getFreelancerMainSkillsOffer(String description,
                                             Long expectedSkillId,
                                             List<FreelancerSkillOffer> freelancerSkillOffers) {
        FreelancerSkillsRepository freelancerSkillsRepository = mock(FreelancerSkillsRepository.class);
        when(freelancerSkillsRepository.getOffers(anyInt(), anyCollection())).thenReturn(freelancerSkillOffers);
        FreelancerSkillOffersService testedService = new FreelancerSkillOffersService(shardHelper,
                freelancerSkillsRepository,
                freelancerSkillOffersValidationService);

        FreelancerSkillOffer freelancerMainSkillsOffer = testedService.getFreelancerMainSkillsOffer(0L);
        Long skillId = freelancerMainSkillsOffer != null ? freelancerMainSkillsOffer.getSkillId() : null;

        assertThat(skillId).as(description).isEqualTo(expectedSkillId);
    }
}
