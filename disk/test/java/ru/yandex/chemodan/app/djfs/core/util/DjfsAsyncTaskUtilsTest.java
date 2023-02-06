package ru.yandex.chemodan.app.djfs.core.util;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DjfsAsyncTaskUtilsTest {
    @Test
    public void activeUid() {
        String deduplicationId = "update_last_files_cache__bc79a1da479adb69f533226f15055a4a";
        Assert.equals("c92adc68aa499d9eaa49454a537dc13c", DjfsAsyncTaskUtils.activeUid(deduplicationId));
    }
}
