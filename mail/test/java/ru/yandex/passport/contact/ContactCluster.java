package ru.yandex.passport.contact;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.contact.config.ContactProxyConfigBuilder;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

import static ru.yandex.passport.contact.ContactTestUtils.Contact;
import static ru.yandex.passport.contact.ContactTestUtils.Pair;
import static ru.yandex.passport.contact.ContactTestUtils.User;
import static ru.yandex.passport.contact.ContactTestUtils.contactJson;
import static ru.yandex.passport.contact.ContactTestUtils.userJson;

public class ContactCluster implements GenericAutoCloseable<IOException>  {
    private final GenericAutoCloseableChain<IOException> chain;

    private final TestSearchBackend messagesBackend;
    private final TestSearchBackend usersBackend;

    private static final String USERS_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/chats_backend/files/chats_backend.conf");
    private static final String MESSAGES_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/messages_backend/files/messages_backend.conf");

    private final ContactProxy proxy;

    private static final String MESSAGES_SERVICE = "messenger_messages";
    private static final String USERS_SERVICE = "messenger_users";

    public static final String CONTACT_CONFIG =
            Paths.getSourcePath("mail/search/passport/contact/files/contact_proxy.conf");

    public ContactCluster(final TestBase testBase)
            throws Exception {
        System.setProperty("TVM_API_HOST", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("TVM_ALLOWED_SRCS", "");
        System.setProperty("SECRET", "");
        System.setProperty("SERVER_NAME", "");
        System.setProperty("JKS_PASSWORD", "");
        System.setProperty("INDEX_PATH", "");
        System.setProperty("INDEX_DIR", "");
        System.setProperty("MAIL_SEARCH_TVM_ID", "0");
        System.setProperty("YT_ACCESS_LOG", "");
        System.setProperty("SERVICE_CONFIG", "null.conf");
        System.setProperty("BSCONFIG_IPORT", "0");
        System.setProperty("BSCONFIG_IDIR", ".");
        System.setProperty("OLD_SEARCH_PORT", "0");
        System.setProperty("SEARCH_PORT", "0");
        System.setProperty("INDEX_PORT", "0");
        System.setProperty("DUMP_PORT", "0");
        System.setProperty("CONSUMER_PORT", "0");
        System.setProperty("CMNT_API", "empty-mail-producer");
        System.setProperty("MOXY_HOST", "localhost");
        System.setProperty("MESSENGER_ROUTER_TVM_CLIENT_ID", "");
        System.setProperty("MESSENGER_ROUTER_HOST", "localhost");
        System.setProperty("MAIL_PRODUCER", "empty-mail-producer");
        System.setProperty("CHATS_SERVICE", "messenger_chats");
        System.setProperty("USERS_SERVICE", "messenger_users");
        System.setProperty("MESSAGES_SERVICE", "messenger_messages");
        System.setProperty("TASKS_SERVICE", "tasks_service");
        System.setProperty("META_API_HOST", "");
        System.setProperty("META_API_TVM_CLIENT_ID", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("CPU_CORES", "2");
        System.setProperty("PROXY_WORKERS", "2");
        System.setProperty("SEARCH_THREADS", "2");
        System.setProperty("MERGE_THREADS", "2");
        System.setProperty("LIMIT_SEARCH_REQUESTS", "10");
        System.setProperty("LIMIT_FORWARD_REQUESTS", "10");
        System.setProperty("INDEX_THREADS", "2");
        System.setProperty("MOXY_TVM_CONF", "moxy-notvm.conf");
        System.setProperty("HOSTNAME", "localhost");
        System.setProperty("NGINX_SSL_PORT", "0");
        System.setProperty("FULL_LOG_LEVEL", "all");
        System.setProperty("BLOCK_CASHE_SIZE", "200M");
        System.setProperty("COMPRESSED_CASHE_SIZE", "200M");
        System.setProperty("LIMIT_RECENT_REQUESTS", "5");

        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>())) {

            Path tmpDir1 = Files.createTempDirectory(testBase.testName.getMethodName());
            Path tmpDir2 = Files.createTempDirectory(testBase.testName.getMethodName());
            IniConfig chatsConfig =
                TestSearchBackend.patchConfig(
                    tmpDir1,
                    true,
                    new IniConfig(new File(USERS_BACKEND_CONFIG)));
            chatsConfig.put("shards", "1");
            IniConfig mesConfig =
                TestSearchBackend.patchConfig(
                    tmpDir2,
                    true,
                    new IniConfig(new File(MESSAGES_BACKEND_CONFIG)));

            usersBackend = new TestSearchBackend(tmpDir1, true, chatsConfig);
            messagesBackend = new TestSearchBackend(tmpDir2, true, mesConfig);

            chain.get().add(messagesBackend);
            chain.get().add(usersBackend);

            String messagesSearchmap = messagesBackend.searchMapRule(MESSAGES_SERVICE, PrefixType.STRING);
            String usersSearchmap = usersBackend.searchMapRule(USERS_SERVICE, PrefixType.STRING);

            ContactProxyConfigBuilder configBuilder = new ContactProxyConfigBuilder(
                patchProxyConfig(new IniConfig(new File(CONTACT_CONFIG))));

            configBuilder.searchMapConfig().content(messagesSearchmap + usersSearchmap);

            proxy = new ContactProxy(configBuilder.build());
            chain.get().add(proxy);

            this.chain = chain.release();
        }
    }

    public static IniConfig patchProxyConfig(final IniConfig config) throws Exception {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.sections().remove("https");
        IniConfig iam = config.section("iam");
        iam.sections().remove("https");
        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }

    public ContactProxy proxy() {
        return proxy;
    }

    public void start() throws IOException {
        proxy.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public void addUser(User user) throws IOException {
        for (String guid: user.guids()) {
            usersBackend.add(userJson(user.puid(), guid));
        }
    }

    public void flush() throws IOException {
        messagesBackend.flush();
        usersBackend.flush();
    }

    public void addContact(Contact contact) throws IOException {
        messagesBackend.add(new StringPrefix(contact.ownerGuid()), contactJson(contact.ownerGuid(), contact.name(), contact.contactGuid()));
    }

    public void addContacts(Pair<Contact, Contact> contacts) throws IOException {
        addContact(contacts.first());
        addContact(contacts.second());
    }

    public TestSearchBackend usersBackend() {
        return usersBackend;
    }

    public TestSearchBackend messagesBackend() {
        return messagesBackend;
    }
}
