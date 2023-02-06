package ru.yandex.market.olap2.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ProgressInputStreamTest {

    @Test
    public void testProgress() throws IOException {
        ProgressInputStream prog = new ProgressInputStream(
            new ByteArrayInputStream("zzzzaaaa".getBytes()));
        prog.read();
        assertThat(prog.getBytesRead(), is(1L));
        byte[] buf = new byte[1024*1024];
        int read = prog.read(buf);
        assertThat(prog.getBytesRead(), is(1L + read));
    }

    @Test
    public void testHugeProgress() throws IOException {
        byte[] data = StringUtils.repeat("zzzz", 2*1024*1024).getBytes();
        ProgressInputStream prog = new ProgressInputStream(
            new BufferedInputStream(
                new ByteArrayInputStream(data)));
        prog.read();
        prog.read();
        assertThat(prog.getBytesRead(), is(2L));
        prog.skip(19);
        assertThat(prog.getBytesRead(), is(2L + 19));
        byte[] buf = new byte[32*1024];
        while(true) {
            int read = prog.read(buf);
            if(read < 0) break;
            // discard read data
        }
        assertThat(prog.getBytesRead(), is(data.length - 1L)); // -1 end of stream
    }

    @Test
    @Ignore
    public void testProgressTh() throws IOException, InterruptedException {
        byte[] data = StringUtils.repeat("zzzz", 100*1024*1024).getBytes();
        System.out.println("data size: " + data.length);
        ProgressInputStream prog = new ProgressInputStream(
            new ByteArrayInputStream(data));
        AtomicInteger acc = new AtomicInteger();
        new Thread(()->{
            try {
                byte[] buf = new byte[256];
                int a = 0;
                while (true) {
                    int read = prog.read(buf);
                    if (read < 0) break;
                    for(int i = 0; i < 3000; i++) {
                        a += ThreadLocalRandom.current().nextInt() + read;
                    }
                }
                acc.set(a);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        for(int i = 0; i < 60; i++) {
            System.out.println(prog.getBytesRead());
            Thread.sleep(100);
        }

        System.out.println(acc.get());
    }

}
