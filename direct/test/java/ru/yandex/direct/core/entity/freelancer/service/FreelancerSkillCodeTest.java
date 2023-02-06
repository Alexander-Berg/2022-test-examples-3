package ru.yandex.direct.core.entity.freelancer.service;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill.allFreelancerSkills;

public class FreelancerSkillCodeTest {

    /**
     * Проверяем, что существующий набор значений {@link FreelancerSkill} не меняется.
     * <p>
     * Добавление нового значения следует согласовывать с командой frontend'а, чтобы они могли его отобразить.
     * Перед удалением необходимо убедиться, что frontend не присылает удаляемое значение и что из UGC DB оно не придёт.
     */
    @Test
    public void NoOneCodeWasDeleted() {
        List<String> allFreelancerSkillCodeIds = StreamEx.of(allFreelancerSkills())
                .map(FreelancerSkill::getSkillCode)
                .toList();
        //Коды в этом списке можно добавлять, но нельзя переименовывать и/или удалять, т.к. они используются во внешних системах.
        assertThat(allFreelancerSkillCodeIds)
                .containsExactlyInAnyOrder("SETTING_UP_CAMPAIGNS_FROM_SCRATCH",
                        "CAMPAIGN_CONDUCTING",
                        "CAMPAIGN_AUDIT");
    }
}
