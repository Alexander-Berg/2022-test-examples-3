package ru.yandex.autotests.innerpochta.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPOutputStream;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.qatools.allure.annotations.Attachment;
import ru.yandex.qatools.allure.annotations.Step;

public class Ssh implements AutoCloseable {
    public static final String DEFAULT_SSH_LOGIN = "robot-gerrit";
    public static final String PASSPHRASE = "";
    private final Logger logger = Logger.getLogger(this.getClass());
    private Connection connection;
    private String login = "robot-gerrit";
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
        this.logger.info(String.format("[SSH] Connecting to: %s@%s", this.login, this.connection.getHostname()));

        try {
            this.connection.connect();
        } catch (IOException var3) {
            this.logger.error(String.format("Can't connect to: %s@%s", this.login, this.connection.getHostname()), var3);
        }

        try {
            this.connection.authenticateWithPublicKey(this.login, this.privateKey, "");
        } catch (IllegalStateException | IOException var2) {
            this.close();
            this.logger.error(String.format("Authentication exception when auth on: %s@%s", this.login, this.connection.getHostname()), var2);
        }

        if (!this.connection.isAuthenticationComplete()) {
            this.close();
            this.logger.error(String.format("Authentication failed on: %s@%s", this.login, this.connection.getHostname()));
        }

        return this;
    }

    public void close() {
        this.connection.close();
    }

    @Step("[SSH]: $ {0}")
    public String cmd(String cmd) throws IOException {
        this.shouldBeAuthenficated();
        Session sess = this.connection.openSession();
        LogManager.getLogger(this.getClass()).info(String.format("[SSH]: $ %s", cmd));
        sess.execCommand(cmd);
        String out = this.gobble("STDOUT", sess.getStdout());
        this.gobble("STDERR", sess.getStderr());
        sess.close();
        return out;
    }

    public Connection conn() {
        return this.connection;
    }

    private String gobble(String comment, InputStream out) throws IOException {
        String var6;
        try {
            StreamGobbler input = new StreamGobbler(out);
            Throwable var4 = null;

            try {
                String what = IOUtils.toString(input, "UTF-8");
                if (StringUtils.isNotBlank(what)) {
                    this.attach(time(), comment, what);
                }

                var6 = what;
            } catch (Throwable var22) {
                var4 = var22;
                throw var22;
            } finally {
                if (input != null) {
                    if (var4 != null) {
                        try {
                            input.close();
                        } catch (Throwable var21) {
                            var4.addSuppressed(var21);
                        }
                    } else {
                        input.close();
                    }
                }

            }
        } finally {
            IOUtils.closeQuietly(out);
        }

        return var6;
    }

    @Attachment("[{0}]: {1}")
    private String attach(String time, String comment, String what) {
        return what;
    }

    public File scpTo(File file, String remotePath) throws IOException {
        this.logger.debug(String.format("Uploading file by ssh to [%s]", remotePath));
        String name = StringUtils.substringAfterLast(remotePath, "/");
        String path = StringUtils.substringBeforeLast(remotePath, "/");

        try {
            SCPOutputStream scp = this.connection.createSCPClient().put(name, file.length(), path, "0644");
            Throwable var6 = null;

            File var10;
            try {
                FileInputStream fis = new FileInputStream(file);
                Throwable var8 = null;

                try {
                    WritableByteChannel wbc = Channels.newChannel(scp);
                    fis.getChannel().transferTo(0L, file.length(), wbc);
                    var10 = file;
                } catch (Throwable var35) {
                    var8 = var35;
                    throw var35;
                } finally {
                    if (fis != null) {
                        if (var8 != null) {
                            try {
                                fis.close();
                            } catch (Throwable var34) {
                                var8.addSuppressed(var34);
                            }
                        } else {
                            fis.close();
                        }
                    }

                }
            } catch (Throwable var37) {
                var6 = var37;
                throw var37;
            } finally {
                if (scp != null) {
                    if (var6 != null) {
                        try {
                            scp.close();
                        } catch (Throwable var33) {
                            var6.addSuppressed(var33);
                        }
                    } else {
                        scp.close();
                    }
                }

            }

            return var10;
        } catch (IOException var39) {
            throw new RuntimeException("Can't upload file by scp", var39);
        }
    }

    private void shouldBeAuthenficated() throws IOException {
        if (!this.connection.isAuthenticationComplete()) {
            this.auth();
        }

    }

    public static String time() {
        return (new SimpleDateFormat("HH:mm:ss.SSS")).format(new Date());
    }
}
