package ru.yandex.chemodan.app.djfs.core.legacy.formatting;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.djfs.core.legacy.LegacyFilesystemActions;
import ru.yandex.misc.test.Assert;

public class DefaultFoldersSerializingTest {

    @Test
    public void testOnlyPathsSerialization() throws JsonProcessingException {
        Assert.assertEquals("{\"attach\":\"/disk/Почтовые вложения\"}",
                new String(LegacyFilesystemActions.mapper.writeValueAsBytes(new DefaultFoldersOnlyPathPojo(Cf.map("attach", "/disk/Почтовые вложения")))));
    }

    @Test
    public void testFullSerialization() throws JsonProcessingException {
        Assert.assertEquals("{\"attach\":{\"path\":\"/disk/Почтовые вложения\",\"exist\":1}}",
                new String(LegacyFilesystemActions.mapper.writeValueAsBytes(new DefaultFoldersFullPojo(Cf.map("attach", new DefaultFolderPojo("/disk/Почтовые вложения", 1))))));
    }
}
