package ru.yandex.market.api.run;

import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;
import ru.yandex.market.api.util.ApiStrings;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * @author dimkarp93
 */
public class StreamHelper {
    private static final PrintStream NULL_PRINT_STREAM = getPrintStream();

    @NotNull
    private static PrintStream getPrintStream() {
        try {
            return new PrintStream(new NullOutputStream(), true, ApiStrings.UTF8_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrintStream out = null;
    private static PrintStream err = null;

    public static void directStdoutToNull() {
        out = System.out;
        System.setOut(NULL_PRINT_STREAM);
    }

    public static void directStderrToNull() {
        err = System.err;
        System.setErr(NULL_PRINT_STREAM);
    }

    public static void directAllOutputToNull() {
        directStdoutToNull();
        directStderrToNull();
        ;
    }

    public static void restoreStdout() {
        if (null == out) {
            throw new RuntimeException("Stdout has not directed into null");
        }
        System.setOut(out);
    }

    public static void restoreStderr() {
        if (null == err) {
            throw new RuntimeException("Stderr has not directed into null");
        }
        System.setErr(err);
    }

    public static void restoreAllOutput() {
        restoreStdout();
        restoreStderr();
    }
}
