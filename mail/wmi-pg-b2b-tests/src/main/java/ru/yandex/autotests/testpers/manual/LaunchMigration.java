package ru.yandex.autotests.testpers.manual;

import com.google.common.base.Splitter;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;
import ru.yandex.autotests.passport.api.tools.registration.RegUser;
import ru.yandex.qatools.allure.annotations.Title;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.form;
import static jersey.repackaged.com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.basic;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule.viaRemoteHost;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshRemotePortForwardingRule.localPortForMocking;

/**
 * @author lanwen (Merkushev Kirill)
 *         Date: 28.05.15
 */
public class LaunchMigration {

    public static final Logger LOG = Logger.getLogger(LaunchMigration.class);
    public static final String TO_PG = "ora2pg-package-steal_users";
    public static final String TOKEN = "b0f4e20ee5e182627722db7b480414fe";
    public static final String LOGIN = "robot-aqua-testpers";
    public static final String JENKINS = "jenkins.mail.yandex.net";
    public static final String PG_LOGIN_SUFFIX = "pg";
    public static final String MDB_LOGIN_SUFFIX = "mdb";

    private Client client = ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()));

    public static final String BACK_TO_ORA = "mail-mdb-ora2pg_migrate-back-to-ora";

    @ClassRule
    public static SshLocalPortForwardingRule fwd = viaRemoteHost(props().betaURI())
            .forwardTo(URI.create("http://blackbox.yandex.net:80"))
            .onLocalPort(localPortForMocking());

    private static final File CACHE_FILE = new File("suid-mdb.txt");
    private static final File NEW_USERS_FILE = new File("new_users.txt");
    private static final File NEW_USERS_OLD_USERS_FILE = new File("new_users-old_users.txt");

    @Test
    @Title("0. Новые пользователи")
    public void regUsers() throws Exception {
        Collection<String> accs = new ArrayList<>();
        
        for (int i = 2; i < 21; i++) {
            accs.add("pgtest-root" + i);
        }
        accs.stream().forEach(account -> {

            try {
                RegUser newUser = UserCreate.createNewUser(/*account, ""*/);

                String s = format("login:%s::pwd:%s::uid:%s%n", newUser.getLogin(), newUser.getPwd(), newUser.getUid());
                LOG.info(s);
                try {
                    Files.asCharSink(NEW_USERS_FILE, UTF_8, FileWriteMode.APPEND).write(s);
                } catch (IOException e) {
                    throw new RuntimeException("", e);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("", e);
                }
            } catch (Throwable ignored) {
            }
        });
    }

    @Test
    @Title("0. Новые пользователи")
    public void regUser() throws Exception {
        RegUser newUser = UserCreate.createNewUserInTestEnvironment("GCityLinByIp-pg", PG_LOGIN_SUFFIX);
        String s = format("login:%s::pwd:%s::uid:%s%n", newUser.getLogin(), newUser.getPwd(), newUser.getUid());
        LOG.info(s);
        try {
            Files.asCharSink(NEW_USERS_FILE, UTF_8, FileWriteMode.APPEND).write(s);
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }
    }


    @Test
    @Title("0. Новые пользователи в тестовом окружении")
    public void regUsersInTestEnvironment() throws Exception {
        Collection<Account> accs = WmiCoreProperties.props().accounts().values();
        accs.stream().forEach(account -> {

            try {
                RegUser newUser = UserCreate.createNewUserInTestEnvironment(account.getLogin(), MDB_LOGIN_SUFFIX);

                String s = format("login:%s::pwd:%s::uid:%s%n", newUser.getLogin(), newUser.getPwd(), newUser.getUid());
                LOG.info(s);
                try {
                    Files.asCharSink(NEW_USERS_FILE, UTF_8, FileWriteMode.APPEND).write(s);
                } catch (IOException e) {
                    throw new RuntimeException("", e);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("", e);
                }
            } catch (Throwable ignored) {
            }
        });
    }

    @Test
    @Title("1. Сначала закешируй файл")
    public void fetchSuidAndMdb() throws Exception {
        CACHE_FILE.delete();
        CACHE_FILE.createNewFile();

        Collection<Account> accs = WmiCoreProperties.props().accounts().values();

        accs.stream().forEach(account -> {
            String s = bbInfo(account.getLogin(), client);

            String format = format("%s:%s:%s:%s:%s%n",
                    account.getLogin(),
                    account.getPassword(),
                    mdb(s),
                    suid(s),
                    uid(s)
            );

            LOG.info(format);
            try {
                Files.asCharSink(CACHE_FILE, UTF_8, FileWriteMode.APPEND).write(format);
            } catch (IOException e) {
                throw new RuntimeException("", e);
            }
        });
    }

    @Test
    @Title("1.1. Старый - новый")
    public void fetchSuidAndMdbPair() throws Exception {
        NEW_USERS_OLD_USERS_FILE.delete();
        NEW_USERS_OLD_USERS_FILE.createNewFile();

        Files.asCharSource(NEW_USERS_FILE, UTF_8).readLines().stream()
                .map(line -> Splitter.on("::").splitToList(line).get(0).split(":")[1])
                .forEach(login -> {
                    LOG.info(login + " - " + substringBeforeLast(login, "-".concat(PG_LOGIN_SUFFIX)));
                    String oldU = bbInfo(substringBeforeLast(login, "-".concat(PG_LOGIN_SUFFIX)), client);
                    String newU = bbInfo(login, client);

                    String format = format("(%s, %s, %s) - (%s, %s)%n",
                            suid(oldU),
                            mdb(oldU),
                            uid(oldU),
                            uid(newU),
                            suid(newU)
                    );

                    LOG.info(format);
                    try {
                        Files.asCharSink(NEW_USERS_OLD_USERS_FILE, UTF_8, FileWriteMode.APPEND).write(format);
                    } catch (IOException e) {
                        throw new RuntimeException("", e);
                    }
                });
    }

    private String uid(String oldU) {
        return substringBetween(oldU, "<uid hosted=\"0\">", "<");
    }

    private String suid(String oldU) {
        return substringBetween(oldU, "id=\"subscription.suid.2\">", "<");
    }

    private static String mdb(String oldU) {
        return substringBetween(oldU, "id=\"hosts.db_id.2\">", "<");
    }

    private static String bbInfo(String login, Client client) {
        return client
                .target(UriBuilder.fromUri(fwd.local())
                        .path("/blackbox/")
                        .queryParam("method", "userinfo")
                        .queryParam("login", login)
                        .queryParam("sid", "smtp")
                        .queryParam("userip", "127.0.0.1")
                        .queryParam("dbfields", on(",").join("subscription.suid.2", "hosts.db_id.2")).build())
                .request().header("Host", "blackbox.yandex.net").get().readEntity(String.class);
    }

    @Test
    @Title("2. Смигрируй файл")
    public void transfer() throws IOException {
        client = ClientBuilder.newClient();
        Files.asCharSource(CACHE_FILE, UTF_8).readLines().stream()
                .map(convert())
                .filter(next -> !next.getMdb().equals("pg"))
                .forEach(next -> {
                            LOG.info(format("[2PG] Next is - %s:%s", next.getSuid(), next.getMdb()));

                            String s = client.target(UriBuilder.fromPath("/job/{job}/buildWithParameters")
                                    .scheme("http").host(JENKINS)
                                    .build(TO_PG))
                                    .register(basic(LOGIN, TOKEN))
                                    .request().post(form(
                                            new Form("SUID", next.getSuid()).param("SOURCE", next.getMdb())
                                    )).readEntity(String.class);
                            LOG.info(s);
                        }
                );
    }

    @Test
    @Title("3. Перекешируй данные и можешь мигрировать обратно")
    public void transferBack() throws IOException {
        client = ClientBuilder.newClient();
        Files.asCharSource(CACHE_FILE, UTF_8).readLines().stream()
                .map(convert())
                .filter(next -> next.getMdb().equals("pg"))
                .forEach(next -> {
                    LOG.info(format("[2ORA] Next is - %s:%s", next.getSuid(), next.getMdb()));

                    String s = client.target(UriBuilder.fromPath("/job/{job}/buildWithParameters")
                            .scheme("http").host(JENKINS)
                            .build(BACK_TO_ORA))
                            .register(basic(LOGIN, TOKEN))
                            .request().post(form(
                                    new Form("SUID", next.getSuid())
                            )).readEntity(String.class);
                    LOG.info(s);
                });
    }

    @NotNull
    public Function<String, SuidMdb> convert() {
        return line -> {
            List<String> strings = Splitter.on(":").splitToList(line);
            return new SuidMdb(strings.get(4), strings.get(3), strings.get(2));
        };
    }

    public static class SuidMdb {
        private String uid;
        private String suid;
        private String mdb;

        public SuidMdb(String uid, String suid, String mdb) {
            this.uid = uid;
            this.suid = suid;
            this.mdb = mdb;
        }

        public String getSuid() {
            return suid;
        }

        public String getMdb() {
            return mdb;
        }

        public String getUid() {
            return uid;
        }
    }
}
