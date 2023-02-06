package ru.yandex.direct.jobs.dialogs;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Dialog;

public class TestDialog {
    private Dialog dialog;

    private String expectedBotGuid;
    private String expectedName;
    private Boolean expectedIsActive;
    private StatusBsSynced expectedStatusBsSynced;

    public TestDialog withDialog(String skillId, String botGuid, String name, Boolean isActive) {
        this.dialog = new Dialog()
                .withSkillId(skillId)
                .withBotGuid(botGuid)
                .withName(name)
                .withIsActive(isActive);
        return this;
    }

    public TestDialog withExpected(
            String expectedBotGuid, String expectedName, Boolean expectedIsActive, StatusBsSynced expectedStatusBsSynced
    )
    {
        this.expectedBotGuid = expectedBotGuid;
        this.expectedName = expectedName;
        this.expectedIsActive = expectedIsActive;
        this.expectedStatusBsSynced = expectedStatusBsSynced;
        return this;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public String getExpectedBotGuid() {
        return expectedBotGuid;
    }

    public String getExpectedName() {
        return expectedName;
    }

    public Boolean getExpectedIsActive() {
        return expectedIsActive;
    }

    public StatusBsSynced getExpectedStatusBsSynced() {
        return expectedStatusBsSynced;
    }

    @Override
    public String toString() {
        return "TestDialog{" +
                "dialog=" + dialog +
                ", expectedBotGuid=" + expectedBotGuid +
                ", expectedName=" + expectedName +
                ", expectedIsActive=" + expectedIsActive +
                ", expectedStatusBsSynced=" + expectedStatusBsSynced +
                '}';
    }
}
