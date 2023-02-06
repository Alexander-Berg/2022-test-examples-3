package ru.yandex.market.olap2.load;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.olap2.util.ProgressInputStream;
import ru.yandex.market.olap2.util.SleepUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class LoadInputStreamTest {
    @Test
    @Ignore
    public void testReporting() throws IOException, InterruptedException {
        AtomicLong c = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(10);
        for(int i = 0; i < 10; i++) {
            int z = i;
            LoadInputStream a = write("i_" + i,
                StringUtils.repeat("a", 40 * 1024*1024));
            new Thread(() -> {
                try {
                    int total = 0;
                    while (true) {
                        int b = a.getStream().read();
                        if(b < 0) break;
                        total++;
                        if((z == 8 || z == 2) && total > 2 * 1024 * 1024) {
                            break;
                        }
                        for(int j = 0; j < 1 + z; j++) {
                            c.addAndGet(b);
                        }
                    }
                    a.close();
                    SleepUtil.sleep(2000);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        System.out.println(c.get());
    }

    @SneakyThrows
    private LoadInputStream write(String tag, String data) {
        return new LoadInputStream(
            tag,
            new ProgressInputStream(
                new ByteArrayInputStream(data.getBytes("UTF-8"))));
    }
}
