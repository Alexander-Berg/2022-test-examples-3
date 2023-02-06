package ru.yandex.mail.common.execute;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class Local extends Shell {
    private InputStream stdout;
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public int exec(String cmd, long timeout) throws IOException, InterruptedException {
        logger.info(cmd);

        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor(timeout, TimeUnit.MILLISECONDS);

        stdout = p.getInputStream();

        return p.exitValue();
    }

    @Override
    public InputStream getStdout() {
        return stdout;
    }

    @Override
    public void close() {
        stdout = null;
    }
}
