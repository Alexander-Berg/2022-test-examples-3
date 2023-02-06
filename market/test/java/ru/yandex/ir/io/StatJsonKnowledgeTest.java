package ru.yandex.ir.io;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.ir.util.TestUtil;

public class StatJsonKnowledgeTest {

    private static final String PATH = TestUtil.getSrcTestResourcesPath();

    @Test
    public void reloadOnly() throws Exception {
        StatShardKnowledge statShardKnowledge = new StatShardKnowledge(false,
                StatShardKnowledge.DEFAULT_SHARD_ID, shardId -> {
            throw new UnsupportedOperationException();
        });

        StatJsonKnowledge statJsonKnowledge = new StatJsonKnowledge();
        statJsonKnowledge.setDataGetterDir(PATH);
        statJsonKnowledge.setStatShardKnowledge(statShardKnowledge);

        statJsonKnowledge.reloadOnly();
        Assert.assertEquals(123456789000L, statJsonKnowledge.getCreationTime());

        FullFileStatEntry fileStatEntry = statJsonKnowledge.getFileStatEntry(PATH + "/books.4.matcher.xml");

        Assert.assertEquals("489c4754f928a00fd3ed6d53f8df53fc", fileStatEntry.getMd5());
        Assert.assertEquals(1495005664000L, fileStatEntry.getModificationTime());

        fileStatEntry = statJsonKnowledge.getFileStatEntry(PATH + "/stable/light-matcher/model-barcodes.txt");

        Assert.assertEquals("c8949309dbec8fcbd1eca818a78b391e", fileStatEntry.getMd5());
        Assert.assertEquals(1494831547000L, fileStatEntry.getModificationTime());

        Assert.assertEquals(1495005664 * 1000L, statJsonKnowledge.getLastModificationTimeMs());
    }

}
