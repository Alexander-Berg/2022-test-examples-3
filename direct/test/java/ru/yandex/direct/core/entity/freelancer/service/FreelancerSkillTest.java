package ru.yandex.direct.core.entity.freelancer.service;

import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(JUnitParamsRunner.class)
public class FreelancerSkillTest {

    @SuppressWarnings("unused")
    public static Collection<FreelancerSkill> allActiveSkills() {
        return FreelancerSkill.allFreelancerSkills();
    }

    @Test
    @Parameters(method = "allActiveSkills")
    public void getById_success_forActiveSkillIds(FreelancerSkill activeSkill) {
        FreelancerSkill actual = FreelancerSkill.getById(activeSkill.getSkillId());
        assertThat(actual).isEqualTo(activeSkill);
    }

    @Test
    @Parameters(method = "allActiveSkills")
    public void isActiveSkillId_success_forActiveSkillIds(FreelancerSkill activeSkill) {
        boolean actual = FreelancerSkill.isActiveSkillId(activeSkill.getSkillId());
        assertThat(actual).isTrue();
    }

    // Тесты на выключенную услугу METRIKA_SETUP

    @Test
    public void getById_error_onMetrikaSetupSkillId() {
        assertThatThrownBy(() -> FreelancerSkill.getById(FreelancerSkill.METRIKA_SETUP.getSkillId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void isActiveSkillId_false_onMetrikaSetupSkillId() {
        boolean metrikaSetupSkillActive = FreelancerSkill.isActiveSkillId(FreelancerSkill.METRIKA_SETUP.getSkillId());
        assertThat(metrikaSetupSkillActive).isFalse();
    }

    @Test
    public void allFreelancerSkills_doesntContainMetrikaSetup() {
        assertThat(FreelancerSkill.allFreelancerSkills()).doesNotContain(FreelancerSkill.METRIKA_SETUP);
    }
}
