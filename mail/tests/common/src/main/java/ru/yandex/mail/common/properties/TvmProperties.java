package ru.yandex.mail.common.properties;

import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.secrets.Secret;
import ru.yandex.qatools.secrets.SecretsLoader;

import java.util.HashMap;

public class TvmProperties {
    @Secret("sec-01dq7m4gsyk4jm1pjt7cfehg1w")
    private String tvmSecretProduction = "";

    @Secret("sec-01dq7m4h1j7vtgt5vsb4jbqm35")
    private String tvmSecretIntranetProduction = "";

    @Secret("sec-01dq7m4gk4wgnx22s1q8fsb5ve")
    private String tvmSecretTesting = "N1KNQjXuOcZxA9kQIPKYCw";

    @Property("settings.scope")
    private String settingsScope = "";

    @Property("testing.scope")
    private String testingScope = "";

    private final static int selfIdProduction = 2002332;
    private final static int selfIdIntranetProduction = 2002334;
    private final static int selfIdTesting = 2002330;

    private NativeTvmClient client;

    private TvmApiSettings settingsTesting() {
        return TvmApiSettings
                .create()
                .setSelfTvmId(selfIdTesting)
                .enableServiceTicketsFetchOptions(tvmSecretTesting, new HashMap<String, Integer>() {{
                    put("hound", 2000501);
                    put("mbody", 2000585);
                    put("mops", 2000577);
                    put("akita", 2000429);
                    put("sendbernar", 2000433);
                }});
    }

    private TvmApiSettings settingsProduction() {
        return TvmApiSettings
                .create()
                .setSelfTvmId(selfIdProduction)
                .enableServiceTicketsFetchOptions(tvmSecretProduction, new HashMap<String, Integer>() {{
                    put("hound", 2000499);
                    put("mbody", 2000581);
                    put("mops", 2000571);
                    put("akita", 2000430);
                    put("sendbernar", 2000435);
                }});
    }

    private TvmApiSettings settingsIntranetProduction() {
        return TvmApiSettings
                .create()
                .setSelfTvmId(selfIdIntranetProduction)
                .enableServiceTicketsFetchOptions(tvmSecretIntranetProduction, new HashMap<String, Integer>() {{
                    put("hound", 2000500);
                    put("mbody", 2000583);
                    put("mops", 2000575);
                    put("akita", 2000428);
                    put("sendbernar", 2000434);
                }});
    }

    private TvmProperties() {
        PropertyLoader
                .populate(this);

        Scopes scope;

        if (settingsScope.isEmpty()) {
            scope = Scopes.from(testingScope);
        } else {
            scope = Scopes.from(settingsScope);
        }

        try {
            SecretsLoader
                    .populate(this);
        } catch (Exception e) {
            if (scope != Scopes.TESTING && scope != Scopes.DEVPACK) {
                throw e;
            }
        }

        TvmApiSettings settings;

        switch (scope) {
            case PRODUCTION:
                settings = settingsProduction();
                break;
            case INTRANET_PRODUCTION:
                settings = settingsIntranetProduction();
                break;
            case TESTING:
                settings = settingsTesting();
                break;
            default:
                settings = settingsTesting();
                break;
        }

        client = NativeTvmClient.create(settings);
    }

    private static TvmProperties instance;

    public static synchronized TvmProperties props() {
        if (instance == null) {
            instance = new TvmProperties();
        }
        return instance;
    }

    public String ticketFor(String alias) {
        return client.getServiceTicketFor(alias);
    }
}
