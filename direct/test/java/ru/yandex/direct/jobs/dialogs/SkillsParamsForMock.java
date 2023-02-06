package ru.yandex.direct.jobs.dialogs;

import java.util.List;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.dialogs.service.DialogsService;
import ru.yandex.direct.dialogs.client.model.Skill;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SuppressWarnings("WeakerAccess")
public class SkillsParamsForMock {
    private final String skillId;
    private final String botGuid;
    private final String name;
    private final Boolean onAir;
    private final Skill.Error error;

    SkillsParamsForMock(String skillId, String botGuid, String name, Boolean onAir, Skill.Error error) {
        this.skillId = skillId;
        this.botGuid = botGuid;
        this.name = name;
        this.onAir = onAir;
        this.error = error;
    }

    String getSkillId() {
        return skillId;
    }

    String getBotGuid() {
        return botGuid;
    }

    String getName() {
        return name;
    }

    Boolean getOnAir() {
        return onAir;
    }

    Skill.Error getError() {
        return error;
    }

    @Override
    public String toString() {
        return "SkillMock{" +
                "skillId=" + skillId +
                ", botGuid=" + botGuid +
                ", name=" + name +
                ", onAir=" + onAir +
                ", error=" + error +
                '}';
    }

    static void setSkills(DialogsService mockDialogsService, List<SkillsParamsForMock> skillsParamsForMock) {
        when(mockDialogsService.getSkills(anyList())).thenAnswer(invocations -> {
            List<String> argument = invocations.getArgument(0);
            return StreamEx.of(skillsParamsForMock)
                    .map(SkillsParamsForMock::getSkill)
                    .filter(skill -> argument.contains(skill.getSkillId()))
                    .toList();
        });
    }

    static Skill getSkill(SkillsParamsForMock skillsParamsForMock) {
        Skill skill = new Skill();
        skill.setSkillId(skillsParamsForMock.getSkillId());
        skill.setBotGuid(skillsParamsForMock.getBotGuid());
        skill.setName(skillsParamsForMock.getName());
        skill.setOnAir(skillsParamsForMock.getOnAir());
        skill.setError(skillsParamsForMock.getError());
        return skill;
    }
}
