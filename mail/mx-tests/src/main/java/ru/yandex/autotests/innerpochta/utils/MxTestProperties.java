package ru.yandex.autotests.innerpochta.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;
import ru.yandex.qatools.properties.annotations.Use;
import ru.yandex.qatools.properties.annotations.With;
import ru.yandex.qatools.properties.providers.MapOrSyspropPathReplacerProvider;
import ru.yandex.qatools.secrets.Secret;
import ru.yandex.qatools.secrets.SecretsLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Resource.Classpath("wmi-core.${system.testing.scope}.properties")
@With(MapOrSyspropPathReplacerProvider.class)
public class MxTestProperties {
    public MxTestProperties() {
        SecretsLoader.populate(this);
    }

    private static MxTestProperties instance;

    public static MxTestProperties mxTestProps() {
        if (instance == null) {
            instance = new MxTestProperties();
        }
        return instance;
    }

    private Logger log = LogManager.getLogger(this.getClass());

    @Secret("sec-01d1rntr10f8jyvc33qazewj37")
    private String robotGerritWebmailTeamSshKey;

    @Property("mx.server")
    private String mxServer;

    @Property("mx.port")
    private int mxPort = 25;

    @Property("nsls.host")
    private String nslsHost;

    @Property("nsls.port")
    private int nslsPort = 1234;

    @Property("testing.scope")
    private String testingScope = "pg";

    @Property("use.tls")
    private boolean isUseTls = false;

    @Use(AccountsConverter.class)
    @Property("mx.senderAccounts")
    private final Map<String, List<Map<String, String>>> senderAccounts = new HashMap<>();

    @Use(AccountsConverter.class)
    @Property("mx.receiverAccounts")
    private final Map<String, List<Map<String, String>>> receiverAccounts = new HashMap<>();

    public List<Account> senderAccounts(Class clazz) {
        return accounts(senderAccounts, clazz.getSimpleName());
    }

    public List<Account> receiverAccounts(Class clazz) {
        return accounts(receiverAccounts, clazz.getSimpleName());
    }

    private List<Account> accounts(Map<String, List<Map<String, String>>> accountMap, String loginGroup) {
        if (accountMap.containsKey(loginGroup)) {
            return accountMap.get(loginGroup).stream()
                    .filter(f -> f.containsKey("login"))
                    .map(m -> {
                        Account account = new Account(m.get("login"), m.getOrDefault("pwd", "unknown_pwd"));
                        if (m.containsKey("domain")) {
                            account.domain(m.get("domain"));
                        }
                        return account;
                    })
                    .collect(Collectors.toList());
        } else {
            log.warn("В конфиге нет аккаунта для группы: " + loginGroup);
            return Collections.emptyList();
        }
    }

    public int getMxPort() { return this.mxPort; }

    public String getMxServer() {
        return this.mxServer;
    }

    public boolean isCorpServer() {
        return this.testingScope.equals("webcorp");
    }

    public String getNslsHost() { return this.nslsHost; }

    public int getNslsPort() { return this.nslsPort; }

    public boolean isUseTls() { return this.isUseTls; }

    public String getNslsAppHost() { return "app.".concat(this.nslsHost); }

    public String getRobotGerritWebmailTeamSshKey() {
        return this.robotGerritWebmailTeamSshKey;
    }

}
