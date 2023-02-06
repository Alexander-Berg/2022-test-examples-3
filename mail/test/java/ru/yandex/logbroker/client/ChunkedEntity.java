package ru.yandex.logbroker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.io.EmptyInputStream;

import ru.yandex.util.string.StringUtils;

public class ChunkedEntity extends BasicHttpEntity {
    private static final int INTERVAL = 10;
    private static final int BUFFER_SIZE = 10;

    private final LinkedBlockingQueue<byte[]> queue;
    private volatile boolean finish = false;

    public ChunkedEntity() {
        this.queue = new LinkedBlockingQueue<>(BUFFER_SIZE);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isChunked() {
        return true;
    }

    @Override
    public long getContentLength() {
        return ContentLengthStrategy.CHUNKED;
    }

    public void addChunk(final byte[] data) throws InterruptedException {
        queue.put(data);
    }

    public void chunk(final String prefix, final String data)
        throws InterruptedException
    {
        addChunk(
            StringUtils.concat(prefix, "\n", data)
            .getBytes(Charset.forName("utf-8")));
    }

    public void finish() {
        this.finish = true;
    }

    @Override
    public InputStream getContent()
        throws UnsupportedOperationException
    {
        return EmptyInputStream.INSTANCE;
    }

    @Override
    public void writeTo(final OutputStream outputStream) throws IOException {
        try {
            while (!finish) {
                byte[] data = queue.poll();
                if (data == null) {
                    Thread.sleep(INTERVAL);
                    continue;
                }

                outputStream.write(data);
                outputStream.flush();
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        }
    }

    @Override
    public boolean isStreaming() {
        return true;
    }
}
