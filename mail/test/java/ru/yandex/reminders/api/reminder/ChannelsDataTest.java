package ru.yandex.reminders.api.reminder;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author Eugene Voytitsky
 */
public class ChannelsDataTest {

    @Test(expected = IllegalArgumentException.class)
    public void callbackInvalidUrlNoSchema() {
        new ChannelsData.Callback("127.0.0.1:9090/reminder?suid=32748552").validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void callbackInvalidUrlNotAbsolute() {
        new ChannelsData.Callback("//127.0.0.1:9090/reminder?suid=32748552").validate();
    }

    @Test
    public void callbackOk() {
        ChannelsData.Callback callback = new ChannelsData.Callback("http://127.0.0.1:9090/reminder?suid=32748552");
        Assert.notNull(callback);
    }
}
