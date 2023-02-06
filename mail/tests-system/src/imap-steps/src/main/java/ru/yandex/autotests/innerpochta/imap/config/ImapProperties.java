package ru.yandex.autotests.innerpochta.imap.config;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;
import ru.yandex.qatools.properties.annotations.Use;
import ru.yandex.qatools.properties.annotations.With;
import ru.yandex.qatools.properties.providers.MapOrSyspropPathReplacerProvider;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.ConnectionTypes.DEFAULT;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.ConnectionTypes.SSL;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.ConnectionTypes.TLS;

@Resource.Classpath("imap.${system.testing.scope}.properties")
@With(MapOrSyspropPathReplacerProvider.class)
public class ImapProperties {
    private static final int SSL_PORT = 993;
    private static final int PLAIN_PORT = 143;

    private static ImapProperties instance;
    @Use(AccountsConverter.class)
    @Property("mailboxes.json.file")
    private Map<String, Account> accounts = new HashMap<String, Account>() {{
        put("default", new Account("defaultuser", "testqa"));
    }};
    @Property("imap.host")
    private String host = "imap6-qa.mail.yandex.net";
    @Property("imap.port.ssl")
    private int ssl_port = SSL_PORT;
    @Property("imap.port.plain")
    private int plain_port = PLAIN_PORT;
    @Property("connection.type")
    private String connectionType = DEFAULT.value();
    @Property("smtp.server")
    private String smtpServer = "smtp.yandex.com";
    @Property("smtp.port")
    private int smtpPort = 25;
    @Property("sender.login")
    private String senderLogin = "alkedr-imap-test-sender@yandex.ru";
    @Property("default.user.login")
    private String defaultUserLogin = "";
    @Property("default.user.pwd")
    private String defaultUserPwd = "";
    @Property("use.default.user")
    private boolean useDefaultUser = false;
    @Property("system.folders.lang")
    private String systemFoldersLang = "ru";
    @Property("web.prod.host")
    private String prodHost = "";
    @Property("pop3.port")
    private int pop3Port = 993;
    @Property("is.local.debug")
    private boolean isLocalDebug = false;

    private ImapProperties() {
        PropertyLoader.populate(this);
    }

    public static ImapProperties props() {
        if (instance == null) {
            instance = new ImapProperties();
        }
        return instance;
    }

    public boolean isSSl() {
        return connectionType.equals(SSL.value());
    }

    public boolean isTLS() {
        return connectionType.equals(TLS.value());
    }

    public ImapProperties reset() {
        PropertyLoader.populate(this);
        return this;
    }

    public String getHost() {
        return host;
    }

    public int getPlainPort() {
        return plain_port;
    }

    public int getSslPort() {
        return ssl_port;
    }

    public int getPort() {
        if (connectionType.equals(DEFAULT.value())) {
            return plain_port;
        } else {
            return ssl_port;
        }
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getSenderLogin() {
        return senderLogin;
    }

    public boolean isUseDefaultUser() {
        return useDefaultUser;
    }

    public String getDefaultUserLogin() {
        return defaultUserLogin;
    }

    public String getDefaultUserPwd() {
        return defaultUserPwd;
    }

    public String getSystemFoldersLang() {
        return systemFoldersLang;
    }

    public int getPop3Port() {
        return pop3Port;
    }

    public <T> Account account(T bean) {
        return account(bean.getClass());
    }

    public Account account(Class<?> clazz) {
        return account(clazz.getSimpleName());
    }

    public Account account(String loginGroup) {
        if (useDefaultUser) {
            return new Account(defaultUserLogin, defaultUserPwd);
        }
        return accounts.containsKey(loginGroup) ? accounts.get(loginGroup) : accounts.get("default");
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getWebInterfaceProdHost() {
        return prodHost;
    }

    public boolean isLocalDebug() {
        return isLocalDebug;
    }

    public static enum ConnectionTypes {
        DEFAULT("text"),
        SSL("ssl"),
        TLS("tls");

        private String value;

        private ConnectionTypes(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}
