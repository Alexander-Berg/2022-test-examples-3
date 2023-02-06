package ru.yandex.autotests.innerpochta.utils;

import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;

import static com.google.common.base.Joiner.on;

public class SettingsApiObj extends Obj {

    public static final String SEPARATOR = "%0d";

    public SettingsApiObj uid(String uid) {
        set(true, "uid", uid);
        return this;
    }

    public static SettingsApiObj settings(String uid) {
        return new SettingsApiObj().uid(uid);
    }

    public SettingsApiObj settingsList(String... settings) {
        set(true, "settings_list", on(SEPARATOR).join(settings));
        return this;
    }

    public SettingsApiObj askValidator() {
        set(true, "ask_validator", "y");
        return this;
    }

    public SettingsApiObj format(String format) {
        set(true, "format", format);
        return this;
    }
}
