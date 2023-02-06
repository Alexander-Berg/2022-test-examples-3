package ru.yandex.tikaite.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@SuppressWarnings("InputStreamSlowMultibyteRead")
public class SearchingInputStream extends InputStream {
    private final InputStream is;
    private final byte[] mark;
    private final byte[] buf;
    private boolean open = true;
    private boolean found = false;

    public SearchingInputStream(final InputStream is, final byte[] mark)
        throws IOException
    {
        this.is = is;
        this.mark = mark;
        buf = new byte[mark.length];
        int len = 0;
        while (len < buf.length) {
            int read = is.read();
            if (read == -1) {
                throw new EOFException("Stream is shorter than mark");
            }
            buf[len++] = (byte) read;
        }
    }

    private void checkFound() throws IOException {
        if (found) {
            throw new MarkFoundException();
        }
    }

    @Override
    public int read() throws IOException {
        if (Arrays.equals(buf, mark)) {
            found = true;
        }

        checkFound();

        int result = buf[0];
        for (int i = 1; i < buf.length; ++i) {
            buf[i - 1] = buf[i];
        }
        int read = is.read();
        if (read == -1) {
            throw new EOFException("Mark not found");
        }
        buf[buf.length - 1] = (byte) read;
        return result;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            checkFound();
            is.close();
        }
    }
}

