package ru.yandex.calendar.frontend.caldav.ban;

import org.joda.time.Duration;
import org.junit.Test;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author gutman
 */
public class QueueWithExpirationTestLong {

    @Test
    public void test() {
        QueueWithExpiration<Integer> queue = new QueueWithExpiration<Integer>();
        queue.init();

        queue.add(1, Duration.millis(1000));
        queue.add(2, Duration.millis(1500));
        queue.add(3, Duration.millis(2000));
        queue.add(4, Duration.millis(2500));
        queue.add(5, Duration.millis(3000));

        Assert.equals(5, queue.size());
        ThreadUtils.sleep(1100);
        Assert.equals(4, queue.size());
        ThreadUtils.sleep(500);
        Assert.equals(3, queue.size());
        ThreadUtils.sleep(500);
        Assert.equals(2, queue.size());
        ThreadUtils.sleep(500);
        Assert.equals(1, queue.size());
        Integer[] integers = queue.toArray(new Integer[0]);
        Assert.equals(5, integers[0]);
    }

}
