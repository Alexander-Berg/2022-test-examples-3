package ru.yandex.mail.common.execute;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

import static ch.ethz.ssh2.ChannelCondition.EXIT_SIGNAL;
import static java.lang.String.format;

public class RemoteSsh extends Shell {
    private Session session;
    private Logger logger = Logger.getLogger(this.getClass());

    RemoteSsh(String user, String host, String key) throws IOException {
        this.session = getSSHConnection(user, host, key).openSession();
    }

    @Override
    public int exec(String cmd, long timeout) throws IOException {
        logger.info(cmd);

        session.execCommand(cmd);
        session.waitForCondition(EXIT_SIGNAL, timeout);

        return session.getExitStatus();
    }

    @Override
    public InputStream getStdout() {
        return session.getStdout();
    }

    @Override
    public void close() {
        session.close();
    }

    private Connection getSSHConnection(String login, String testServer, String authKey) {
        Connection conn;
        logger.trace("Try to connect by ssh to " + testServer);
        conn = new Connection(testServer);
        try {
            conn.connect();
            logger.trace("Authentication with public key...");
            boolean isAuthenticated = conn.authenticateWithPublicKey(login, authKey.toCharArray(), "");

            if (!isAuthenticated) {
                throw new IOException(format("Authentication failed (login: %s)", login));
            }
        } catch (IOException exc) {
            throw new RuntimeException("Connection to " + testServer + " failed: " + exc.getMessage(), exc);
        }
        logger.trace("Connection succeeded");
        return conn;
    }
}
