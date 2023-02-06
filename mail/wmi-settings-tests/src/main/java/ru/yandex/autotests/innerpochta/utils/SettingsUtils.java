package ru.yandex.autotests.innerpochta.utils;

import  com.jayway.restassured.path.json.JsonPath;
import static com.jayway.restassured.path.json.JsonPath.from;

import java.util.List;

public class SettingsUtils {
    static  public String getSettingValue(String settingsString, String name) {
        JsonPath s = from(settingsString);
        List<String> settings = s.getList(
                "settings.findAll{key, value -> key == 'single_settings'}.collect{key, value -> value." + name + "}");
        if (settings.isEmpty()) {
            settings = s.getList(
                    "settings.findAll{key, value -> key == 'parameters' || 'profile'}.collect{key, value -> value.single_settings." + name + "}");
        }
        String setting = "default";
        for (String it : settings) {
            if (it != null) {
                setting = it;
                break;
            }
        };
        return setting;
    }

    static public Integer paramsCount(String settingsString) {
        JsonPath s = from(settingsString);
        List<Integer> counts = s.getList(
                "settings.findAll{key, value -> key == 'single_settings'}.collect{key, value -> value.size()}");
        if (counts.isEmpty()) {
            counts = s.getList(
                    "settings.findAll{key, value -> key == 'parameters'}.collect{key, value -> value.single_settings.size()}");
        }
        Integer count = 0;
        for (Integer it : counts) {
            if (it != null) {
                count = it;
                break;
            }
        };
        return count;
    }

    static public Integer profileCount(String settingsString) {
        JsonPath s = from(settingsString);
        List<Integer> counts = s.getList(
                "settings.findAll{key, value -> key == 'single_settings'}.collect{key, value -> value.size()}");
        if (counts.isEmpty()) {
            counts = s.getList(
                    "settings.findAll{key, value -> key == 'profile'}.collect{key, value -> value.single_settings.size()}");
        }
        Integer count = 0;
        for (Integer it : counts) {
            if (it != null) {
                count = it;
                break;
            }
        };
        return count;
    }

    static public List<String> validatedEmails(String settingsString) {
        JsonPath s = from(settingsString);
        List<String> emails = s.getList(
                "settings.emails.findAll{value -> value.validated == true}.collect{value -> value.address}");
        if (emails.isEmpty()) {
            emails = s.getList(
                    "settings.profile.emails.findAll{value -> value.validated == true}.collect{value -> value.address}");
        }
        return emails;
    }
}
