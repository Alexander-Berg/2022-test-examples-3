package ru.yandex.market.ir.matcher2.matcher.matcher.deep;

import java.util.Optional;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.ir.io.StatJsonKnowledge;
import ru.yandex.ir.io.StatShardKnowledge;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.protobuf.ProtoCategoryDao;
import ru.yandex.market.ir.matcher2.matcher.deep.DeepMatchIndexElem;
import ru.yandex.market.ir.matcher2.matcher.deep.DeepMatcherKnowledge;
import ru.yandex.market.ir.matcher2.matcher.deep.TsvCategoryDao;
import ru.yandex.market.ir.matcher2.matcher.utils.FileUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DeepMatcherKnowledgeRefreshTest {
    private StatJsonKnowledge statJsonKnowledge;
    private DeepMatcherKnowledge deepMatcherKnowledge;
    private StatShardKnowledge statShardKnowledge;

    @Mock
    private ProtoCategoryDao protoCategoryDao;

    @Before
    public void setUp() throws Exception {
        statShardKnowledge = new StatShardKnowledge(false,
                StatShardKnowledge.DEFAULT_SHARD_ID, shardId -> {
            throw new UnsupportedOperationException();
        });

        String pathToDeepMatcherResource = FileUtil.getAbsolutePath("/deep");
        statJsonKnowledge = new StatJsonKnowledge();
        statJsonKnowledge.setDataGetterDir(pathToDeepMatcherResource);
        statJsonKnowledge.setStatShardKnowledge(statShardKnowledge);
        protoCategoryDao.setStatJsonKnowledge(statJsonKnowledge);
        protoCategoryDao.setStatShardKnowledge(statShardKnowledge);

        when(protoCategoryDao.loadBooksCategoryIds()).thenReturn(new IntArrayList(Lists.newArrayList(90472)));

        TsvCategoryDao dao = new TsvCategoryDao();
        dao.setDirPath(pathToDeepMatcherResource);
        dao.setStatShardKnowledge(statShardKnowledge);
        deepMatcherKnowledge = new DeepMatcherKnowledge();
        deepMatcherKnowledge.setDao(dao);
        deepMatcherKnowledge.setStatJsonKnowledge(statJsonKnowledge);
        deepMatcherKnowledge.setNumberOfInitialLoadingThreads(1);
        deepMatcherKnowledge.setNumberOfUpdateThreads(1);
        deepMatcherKnowledge.setExcludedCategories("90602");
        deepMatcherKnowledge.setProtoCategoryDao(protoCategoryDao);
    }

    @Test
    public void refresh() throws Exception {
        deepMatcherKnowledge.afterPropertiesSet();
        deepMatcherKnowledge.refreshCategories();

        Optional<DeepMatchIndexElem> firstMatch = deepMatcherKnowledge.get(90490, "6d76c10ba8c40201591a78f16396be68");
        assertTrue(firstMatch.isPresent());
        DeepMatchIndexElem firstExpected = new DeepMatchIndexElem(90490, 4974329, 0.5228402878113174, 4974329, "PSKU");
        assertEquals(firstExpected, firstMatch.get());

        Optional<DeepMatchIndexElem> secondMatch = deepMatcherKnowledge.get(90472, "2db2a0c6f4e32a32ab5e069e5048ea11");
        assertFalse(secondMatch.isPresent());

        Optional<DeepMatchIndexElem> thirdMatch = deepMatcherKnowledge.get(90602, "6d76c10ba8c40201591a78f16396b000");
        assertFalse(thirdMatch.isPresent());
    }
}
