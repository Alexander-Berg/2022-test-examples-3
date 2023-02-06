package ru.yandex.market.ir.matcher2.matcher.matcher.deep;

import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.ir.io.StatShardKnowledge;
import ru.yandex.market.ir.matcher2.matcher.deep.DeepMatchIndexElem;
import ru.yandex.market.ir.matcher2.matcher.deep.TsvCategoryDao;
import ru.yandex.market.ir.matcher2.matcher.utils.FileUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author inenakhov
 */
public class TsvCategoryDaoTest {
    private String pathToDeepMatcherResource = FileUtil.getAbsolutePath("/deep");
    private TsvCategoryDao dao;

    @Before
    public void setUp() throws Exception {
        StatShardKnowledge statShardKnowledge = new StatShardKnowledge(false,
                StatShardKnowledge.DEFAULT_SHARD_ID, shardId -> {
            throw new UnsupportedOperationException();
        });
        dao = new TsvCategoryDao();
        dao.setDirPath(pathToDeepMatcherResource);
        dao.setStatShardKnowledge(statShardKnowledge);
    }

    @Test
    public void loadCategoryIds() throws Exception {
        IntList intList = dao.loadCategoryIds();
        assertEquals(3, intList.size());
        assertTrue(intList.contains(90472));
        assertTrue(intList.contains(90490));
        assertTrue(intList.contains(90602));
    }

    @Test
    public void loadCategory() throws Exception {
        Map<String, DeepMatchIndexElem> index = dao.loadCategory(dao.getPath(90490).get());
        assertEquals(4, index.size());
        assertTrue(index.containsKey("11382ce93e7f5746157bfd644e6c76dd"));
    }
}
