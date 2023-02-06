package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum ReportWizardImageEnum {
    TEXT_IMAGE("text_image"),
    TEXT_ONLY("text_only"),
    TEXT_VIDEO("text_video");

    private String value;

    ReportWizardImageEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
