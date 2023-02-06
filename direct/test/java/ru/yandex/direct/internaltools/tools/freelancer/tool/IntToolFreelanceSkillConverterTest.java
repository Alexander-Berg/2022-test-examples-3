package ru.yandex.direct.internaltools.tools.freelancer.tool;

import java.math.BigDecimal;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkillOffer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkillOfferDuration;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.internaltools.tools.freelancer.container.IntToolFreelanceSkillConverter;
import ru.yandex.direct.internaltools.tools.freelancer.model.FreelancerSkillOfferDurationParameters;
import ru.yandex.direct.internaltools.tools.freelancer.model.IntToolSkillOfferChange;
import ru.yandex.direct.internaltools.tools.freelancer.model.IntToolSkillOfferView;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(JUnitParamsRunner.class)
public class IntToolFreelanceSkillConverterTest {

    @Test
    @Parameters(method = "correctFreelancerSkillOfferDurationView")
    public void convertSkillsToView(FreelancerSkillOfferDuration duration) {
        FreelancerSkillOffer skillOffer =
                new FreelancerSkillOffer()
                        .withSkillId(1L)
                        .withFreelancerId(1L)
                        .withPrice(BigDecimal.TEN)
                        .withDuration(duration);

        IntToolSkillOfferView intToolSkillOffer =
                IntToolFreelanceSkillConverter.toIntToolSkillOffer(skillOffer, CurrencyCode.RUB);
        assertThat(intToolSkillOffer).isNotNull();
    }

    @SuppressWarnings("unused")
    Object[] correctFreelancerSkillOfferDurationView() {
        return FreelancerSkillOfferDuration.values();
    }

    @Test
    public void convertSkillsToView_null() {
        FreelancerSkillOffer skillOffer =
                new FreelancerSkillOffer()
                        .withSkillId(1L)
                        .withFreelancerId(1L)
                        .withPrice(BigDecimal.TEN)
                        .withDuration(null);

        IntToolSkillOfferView intToolSkillOffer =
                IntToolFreelanceSkillConverter.toIntToolSkillOffer(skillOffer, CurrencyCode.RUB);
        assertThat(intToolSkillOffer.getDuration()).isEqualTo("");
    }

    @Test
    @Parameters(method = "correctFreelancerSkillOfferDurationParameters")
    public void convertSkillsFromView(FreelancerSkillOfferDurationParameters params,
                                      FreelancerSkillOfferDuration duration) {
        IntToolSkillOfferChange skillOfferChange = new IntToolSkillOfferChange();
        skillOfferChange.setStrSkillId("1 | Настройка рекламных кампаний с нуля");
        skillOfferChange.setFreelancerId(1L);
        skillOfferChange.setPrice(10L);
        skillOfferChange.setDuration(params);

        FreelancerSkillOffer actual =
                IntToolFreelanceSkillConverter.toFreelancerSkillOffer(skillOfferChange);
        FreelancerSkillOffer expected =
                new FreelancerSkillOffer()
                        .withSkillId(1L)
                        .withFreelancerId(1L)
                        .withPrice(BigDecimal.TEN)
                        .withDuration(duration);
        MatcherAssert.assertThat(actual,
                beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @SuppressWarnings("unused")
    Iterable<Object[]> correctFreelancerSkillOfferDurationParameters() {
        return asList(new Object[][]{
                {FreelancerSkillOfferDurationParameters.FROM_1_TO_3_DAYS,
                        FreelancerSkillOfferDuration.FROM_1_TO_3_DAYS},
                {FreelancerSkillOfferDurationParameters.FROM_3_TO_7_DAYS,
                        FreelancerSkillOfferDuration.FROM_3_TO_7_DAYS},
                {FreelancerSkillOfferDurationParameters.FROM_7_TO_14_DAYS,
                        FreelancerSkillOfferDuration.FROM_7_TO_14_DAYS},
                {FreelancerSkillOfferDurationParameters.FROM_14_TO_28_DAYS,
                        FreelancerSkillOfferDuration.FROM_14_TO_28_DAYS},
                {FreelancerSkillOfferDurationParameters.FROM_1_TO_3_MONTHS,
                        FreelancerSkillOfferDuration.FROM_1_TO_3_MONTHS},
                {FreelancerSkillOfferDurationParameters.MORE_THAN_3_MONTHS,
                        FreelancerSkillOfferDuration.MORE_THAN_3_MONTHS},
                {FreelancerSkillOfferDurationParameters.MONTHLY, FreelancerSkillOfferDuration.MONTHLY},
                {FreelancerSkillOfferDurationParameters.NOT_DEFINED, FreelancerSkillOfferDuration.NOT_DEFINED},
                {null, FreelancerSkillOfferDuration.NOT_DEFINED}
        });
    }
}
