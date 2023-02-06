package ru.yandex.autotests.innerpochta.imap.core.pop3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3SClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;

import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.09.14
 * Time: 19:55
 */
public final class Pop3Client extends ExternalResource {

    private final Logger log = LogManager.getLogger(this.getClass());

    private String hostName = props().getHost();
    private int port = props().getPop3Port();

    private POP3SClient client;
    private String user = "unknowuser";
    private String pass = "unknowpass";


    public Pop3Client() {
    }

    public static Pop3Client pop3() {
        return new Pop3Client();
    }

    public Pop3Client pop3(String user, String pass) {
        this.user = user;
        this.pass = pass;
        return this;
    }

    public Pop3Client pop3(String loginGroup) {
        return pop3(props().account(loginGroup).getLogin(), props().account(loginGroup).getPassword());
    }

    public Pop3Client pop3(Class<?> loginGroup) {
        return pop3(props().account(loginGroup).getLogin(), props().account(loginGroup).getPassword());
    }

    public <T> Pop3Client pop3(T loginGroup) {
        return pop3(props().account(loginGroup).getLogin(), props().account(loginGroup).getPassword());
    }

    public POP3SClient client() {
        return client;
    }

    public Pop3Client connect() {
        return connect(hostName, port);
    }

    @Step("Подключаемся к POP3: {0}:{1}")
    public Pop3Client connect(String hostName, int port) {
        try {
            log.warn(String.format("Connection to POP3... (%s %s)", hostName, port));
            client = new POP3SClient(true);
            client.connect(hostName, port);
            assertThat("Не удалось подконнектиться к серверу POP3", client.isConnected(), is(true));
            assertThat("POP3 не доступен", client.isAvailable(), is(true));
            login(user, pass, true);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось установить коннект с POP3", e);
        }
        return this;
    }

    @Step("Подключаемся к POP3 но не логинимся")
    public Pop3Client connectWithoutLogin() {
        try {
            log.warn(String.format("Connection to POP3... (%s %s)", hostName, port));
            client = new POP3SClient(true);
            client.connect(hostName, port);
            assertThat("Не удалось подконнектиться к серверу POP3", client.isConnected(), is(true));
            assertThat("POP3 не доступен", client.isAvailable(), is(true));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось установить коннект с POP3", e);
        }
        return this;
    }

    @Step("QUIT")
    public Pop3Client disconnect() {
        try {
            log.warn(String.format("Disconnect from POP3... (%s %s)", hostName, port));
            client.disconnect();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось закрыть коннект с POP3", e);
        }

        return this;
    }

    @Step("Выполняем комманду POP3: " +
            "USER {0} " +
            "PASS {1}")
    public void login(String user, String pass, Boolean ok) throws IOException {
        boolean res = client.login(user, pass);
        if (ok) {
            assertThat("Не удалось авторизоваться", res, is(true));
        } else {
            assertThat("Ожидали, что не сможем авторизоваться", res, is(false));
        }
    }

    @Step("Логинимся выбранным юзером")
    public Pop3Client login(Boolean ok) throws IOException {
        boolean res = client.login(user, pass);
        if (ok) {
            assertThat("Не удалось авторизоваться", res, is(true));
        } else {
            assertThat("Ожидали, что не сможем авторизоваться", res, is(false));
        }
        return this;
    }

    @Step("Выполняем комманду POP3: " +
            "NOOP")
    public void noop(Boolean ok) throws IOException {
        boolean res = client.noop();
        if (ok) {
            assertThat("Ожидали, что после NOOP, в ответе будет +OK", res, is(true));
        } else {
            assertThat("Ожидали, что после NOOP, в ответе будет -ERR", res, is(false));
        }
    }

    @Step("Выполняем комманду POP3: " +
            "DELE {0}")
    public void dele(int messageId, Boolean ok) throws IOException {
        boolean res = client.deleteMessage(messageId);
        if (ok) {
            assertThat(String.format("Не удалось удалить сообщение с номером %s," +
                    "ожидали что в ответе будет +OK", messageId), res, is(true));
        } else {
            assertThat(String.format("Ожидали, что после DELE %s, в ответе будет -ERR", messageId), res, is(false));
        }
    }

    @Step("Выполняем комманду POP3: " +
            "STAT")
    public POP3MessageInfo stat() throws IOException {
        return client.status();
    }

    @Step("Выполняем комманду POP3: " +
            "LIST")
    public POP3MessageInfo[] list() throws IOException {
        return client.listMessages();
    }

    @Step("Выполняем комманду POP3: " +
            "LIST {0}")
    public POP3MessageInfo list(int messageId) throws IOException {
        return client.listMessage(messageId);
    }

    @Step("Выполняем комманду POP3: " +
            "RSET")
    public void rset(Boolean ok) throws IOException {
        boolean res = client.reset();
        if (ok) {
            assertThat("Ожидали, что после RSET, в ответе будет +OK", res, is(true));
        } else {
            assertThat("Ожидали, что после RSET, в ответе будет -ERR", res, is(false));
        }
    }

    @Step("Выполняем комманду POP3: " +
            "TOP {0} {1}")
    public List<String> top(int messageId, int numLines) throws IOException {
        return getLines(client.retrieveMessageTop(messageId, numLines));
    }

    @Step("Получаем тему письма с номером {0}")
    public String getSubject(int messageId) throws IOException {
        List<String> list = getLines(client.retrieveMessageTop(messageId, 10));
        with(list).retain(containsString("Subject"));
        return with(list).retain(containsString("Subject")).get(0).replace("Subject:", "").trim();
    }

    @Step("Выполняем комманду POP3: " +
            "RETR {0}")
    public List<String> retr(int messageId) throws IOException {
        return getLines(client.retrieveMessage(messageId));
    }

    @Step("Выполняем комманду POP3: " +
            "RETR {0}")
    public int getSizeMessage(int messageId) throws IOException {
        return getBytes(client.retrieveMessage(messageId));
    }

    @Step("Выполняем комманду POP3: " +
            "QUIT")
    public void quit(Boolean ok) throws IOException {
        boolean res = client.logout();
        if (ok) {
            assertThat("Не удалось выполнить выход из POP3.\n " +
                    "Ожидали, что после QUIT, в ответе будет +OK", res, is(true));
        } else {
            assertThat("Ожидали, что после QUIT, в ответе будет -ERR", res, is(false));
        }
    }

    @Override
    protected void before() {
        connect();
    }

    @Override
    protected void after() {
        disconnect();
    }


    public List<String> getLines(Reader r) {
        List<String> lines = newArrayList();
        try {
            BufferedReader br = new BufferedReader(r);
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\n\r");
                line = br.readLine();
                lines.add(line);
            }
            log.warn("<- MESSAGE: \n\r\n\r" + sb);
            attach("Server", sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("Ну удалось прочитать сообщение из <Reader>", e);
        }
        return lines;
    }

    public Integer getBytes(Reader r) {
        int size;
        try {
            BufferedReader br = new BufferedReader(r);
            int value = 0;
            StringBuilder sb = new StringBuilder();
            while ((value = br.read()) != -1) {
                char c = (char) value;
                sb.append(c);
            }

            size = sb.toString().getBytes().length;
            log.info(sb);

        } catch (Exception e) {
            throw new RuntimeException("Ну удалось прочитать сообщение из <Reader>", e);
        }
        return size;
    }

    @Attachment("{0}-lines")
    private String attach(String who, String what) {
        return what;
    }

    public Pop3Client withUser(String user) {
        this.user = user;
        return this;
    }

    public Pop3Client withPass(String pass) {
        this.pass = pass;
        return this;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }
}
