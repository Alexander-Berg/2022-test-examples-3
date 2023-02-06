package ru.yandex.direct.core.entity.freelancer.service;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkillOfferDuration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class FreelancerSkillOfferDurationTest {

    @Test
    @Parameters(method = "correctDbStrings")
    public void fromDb_success(String stringInDb) {
        FreelancerSkillOfferDuration duration = FreelancerSkillOfferDuration.fromDb(stringInDb);
        assertThat(duration.name()).isEqualTo(stringInDb.toUpperCase());
    }

    @SuppressWarnings("unused")
    private Object[] correctDbStrings() {
        return new String[]{
                "from_1_to_3_days",
                "from_3_to_7_days",
                "from_7_to_14_days",
                "from_14_to_28_days",
                "from_1_to_3_months",
                "more_than_3_months",
                "monthly",
                "not_defined"};
    }

    @Test
    public void fromDb_exception() {
        String stringInDb = "incorrect string";
        FreelancerSkillOfferDuration duration = FreelancerSkillOfferDuration.fromDb(stringInDb);
        assertThat(duration).isEqualTo(FreelancerSkillOfferDuration.NOT_DEFINED);
    }

    @Test
    @Parameters(method = "correctFreelancerSkillOfferDuration")
    public void toDb_success(FreelancerSkillOfferDuration duration, String expectedString) {
        String resultString = FreelancerSkillOfferDuration.toDb(duration);
        assertThat(resultString).isEqualTo(expectedString);
    }

    @SuppressWarnings("unused")
    Iterable<Object[]> correctFreelancerSkillOfferDuration() {
        return asList(new Object[][]{
                {FreelancerSkillOfferDuration.FROM_1_TO_3_DAYS, "from_1_to_3_days"},
                {FreelancerSkillOfferDuration.FROM_3_TO_7_DAYS, "from_3_to_7_days"},
                {FreelancerSkillOfferDuration.FROM_7_TO_14_DAYS, "from_7_to_14_days"},
                {FreelancerSkillOfferDuration.FROM_14_TO_28_DAYS, "from_14_to_28_days"},
                {FreelancerSkillOfferDuration.FROM_1_TO_3_MONTHS, "from_1_to_3_months"},
                {FreelancerSkillOfferDuration.MORE_THAN_3_MONTHS, "more_than_3_months"},
                {FreelancerSkillOfferDuration.MONTHLY, "monthly"},
                {FreelancerSkillOfferDuration.NOT_DEFINED, "not_defined"}
        });
    }
}
