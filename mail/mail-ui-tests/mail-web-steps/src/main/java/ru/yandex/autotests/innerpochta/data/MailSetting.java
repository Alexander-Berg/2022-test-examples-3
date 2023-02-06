package ru.yandex.autotests.innerpochta.data;

public enum MailSetting {
    SAVE_SENT("save_sent");

    private String setting;

    private MailSetting(String setting) {
        this.setting = setting;
    }

    public String getSetting() {
        return setting;
    }

    @Override
    public String toString() {
        return setting;
    }
}
