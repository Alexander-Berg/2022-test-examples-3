package ru.yandex.mail.common.execute;

import java.io.IOException;
import java.io.InputStream;

public abstract class Shell implements AutoCloseable {
    public abstract int exec(String cmd, long timeout) throws IOException, InterruptedException;
    public abstract InputStream getStdout();
}
