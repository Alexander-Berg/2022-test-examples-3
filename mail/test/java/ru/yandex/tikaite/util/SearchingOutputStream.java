package ru.yandex.tikaite.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class SearchingOutputStream extends OutputStream {
    private final OutputStream os;
    private final byte[] mark;
    private final byte[] buf;
    private int len = 0;
    private boolean open = true;
    private boolean found = false;

    public SearchingOutputStream(final OutputStream os, final byte[] mark)
        throws IOException
    {
        this.os = os;
        this.mark = mark;
        buf = new byte[mark.length];
    }

    private void checkFound() throws IOException {
        if (found) {
            throw new MarkFoundException();
        }
    }

    @Override
    public void write(final int b) throws IOException {
        if (len < buf.length) {
            buf[len++] = (byte) b;
        } else {
            for (int i = 1; i < buf.length; ++i) {
                buf[i - 1] = buf[i];
            }
            buf[buf.length - 1] = (byte) b;
        }
        if (len == buf.length && Arrays.equals(buf, mark)) {
            found = true;
        }
        checkFound();
        os.write(b);
    }

    @Override
    public void flush() throws IOException {
        checkFound();
        os.flush();
    }

    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            checkFound();
            os.close();
        }
    }
}

