package ru.yandex.autotests.innerpochta.utils;

import gumi.builders.UrlBuilder;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;
import ru.yandex.qatools.properties.annotations.Use;
import ru.yandex.qatools.properties.annotations.With;
import ru.yandex.qatools.properties.providers.MapOrSyspropPathReplacerProvider;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 22.05.13
 * Time: 18:24
 */
@Resource.Classpath("settings-service.web-${system.settings.scope}.properties")
@With(MapOrSyspropPathReplacerProvider.class)
public class SettingsProperties {

    private static SettingsProperties instance;

    public static SettingsProperties props() {
        if (instance == null) {
            instance = new SettingsProperties();
        }
        return instance;
    }

    private SettingsProperties() {
        PropertyLoader.populate(this);
    }

    @Use(AccountsConverter.class)
    @Property("settings.accounts")
    private Map<String, List<Map<String, String>>> accounts = new HashMap<>();

    @Property("settings.uri")
    private URI settingsUri  = URI.create("http://7xbm563besmw3uux.sas.yp-c.yandex.net");

    @Property("passport.host")
    private URI passportHost = URI.create("https://passport-test.yandex.ru/");

    @Property("akita.port")
    private int akitaPort = 80;

    @Property("akita.host")
    private String akitaHost = "http://akita-test.mail.yandex.net";

    @Property("settings.initial")
    private String initialSettingsFile = "initial-settings-web-test.json";

    public String akitaUri() {
        return UrlBuilder.fromString(akitaHost).withPort(akitaPort).toString();
    }

    public Account account(Class clazz) {
        return accounts(clazz.getSimpleName()).get(0);
    }

    private List<Account> accounts(String loginGroup) {
        if (accounts.containsKey(loginGroup)) {
            return accounts.get(loginGroup).stream()
                    .filter(f -> f.containsKey("login") && f.containsKey("pwd"))
                    .map(m -> new Account(m.get("login"), m.get("pwd")))
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("В конфиге нет аккаунта для группы: " + loginGroup);
        }
    }

    public URI settingsUri() {
        return settingsUri;
    }

    public URI passportHost() {
        return passportHost;
    }

    public String initialSettingsFile() {
        return SettingsProperties.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath()
                .concat(initialSettingsFile);
    }

}
