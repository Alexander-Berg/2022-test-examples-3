package ru.yandex.autotests.testpers.ssh;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPOutputStream;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;


/**
 * User: lanwen
 * Date: 16.01.14
 * Time: 12:29
 * <p/>
 */
public class Ssh implements AutoCloseable {
    public static final String DEFAULT_SSH_LOGIN = "robot-gerrit";

    public static final String PASSPHRASE = "";

    private final Logger logger = Logger.getLogger(this.getClass());
    private Connection connection;
    private String login = DEFAULT_SSH_LOGIN;
    private char[] privateKey;

    public static Ssh withKeyOn(String host, String key) {
        Connection conn = new Connection(host);
        return new Ssh(conn, key.toCharArray());
    }

    public Ssh(Connection connection, char[] array) {
        this.connection = connection;
        this.privateKey = array;
    }

    public Ssh withLogin(String login) {
        this.login = login;
        return this;
    }

    public Ssh auth() {
        logger.info(format("[SSH] Connecting to: %s@%s", login, connection.getHostname()));

        try {
            connection.connect();
        } catch (IOException e) {
            logger.error(format("Can't connect to: %s@%s", login, connection.getHostname()), e);
        }

        try {
            connection.authenticateWithPublicKey(login, privateKey, PASSPHRASE);
        } catch (IOException | IllegalStateException e) {
            close();
            logger.error(format("Authentication exception when auth on: %s@%s",
                    login, connection.getHostname()), e);
        }

        if (!connection.isAuthenticationComplete()) {
            close();
            logger.error(format("Authentication failed on: %s@%s",
                    login, connection.getHostname()));
        }

        return this;
    }


    @Override
    public void close() {
        connection.close();
    }

    @Step("[SSH]: $ {0}")
    public String cmd(String cmd) throws IOException {
        shouldBeAuthenficated();

        Session sess = connection.openSession();
        LogManager.getLogger(this.getClass()).info(format("[SSH]: $ %s", cmd));
        sess.execCommand(cmd);

        String out = gobble("STDOUT", sess.getStdout());
        gobble("STDERR", sess.getStderr());
        sess.close();

        return out;
    }


    public Connection conn() {
        return connection;
    }


    private String gobble(String comment, InputStream out) throws IOException {
        try (StreamGobbler input = new StreamGobbler(out)) {
            String what = IOUtils.toString(input, "UTF-8");
            if (isNotBlank(what)) {
                attach(time(), comment, what);
            }
            return what;
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    @Attachment("[{0}]: {1}")
    private String attach(String time, String comment, String what) {
        return what;
    }


    public File scpTo(File file, String remotePath) throws IOException {
        logger.debug(String.format("Uploading file by ssh to [%s]", remotePath));

        String name = substringAfterLast(remotePath, "/");
        String path = substringBeforeLast(remotePath, "/");

        try (SCPOutputStream scp = connection.createSCPClient().put(name, file.length(), path, "0644");
             FileInputStream fis = new FileInputStream(file)
        ) {
            WritableByteChannel wbc = Channels.newChannel(scp);
            fis.getChannel().transferTo(0, file.length(), wbc);

            return file;
        } catch (IOException e) {
            throw new RuntimeException("Can't upload file by scp", e);
        }
    }


    /**
     * Для обратной совместимости, когда сервер используется не как рула.
     * Когда везде будет сервер использован как рула, можно будет не аутенфицировать здесь.
     *
     * @throws java.io.IOException
     */
    private void shouldBeAuthenficated() throws IOException {
        if (!connection.isAuthenticationComplete()) {
            auth();
        }
    }


    public static String time() {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    }

}
