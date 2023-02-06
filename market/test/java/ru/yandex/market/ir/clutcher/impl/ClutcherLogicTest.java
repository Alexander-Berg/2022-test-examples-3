package ru.yandex.market.ir.clutcher.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.ir.io.StatJsonKnowledge;
import ru.yandex.ir.io.StatShardKnowledge;
import ru.yandex.market.ir.clutcher.CategoryTreeData;
import ru.yandex.market.ir.clutcher.dao.ClutcherKnowledge;
import ru.yandex.market.ir.clutcher.dao.ProtoCategoryDao;
import ru.yandex.market.ir.clutcher.proto.Clutcher;
import ru.yandex.market.ir.clutcher.utils.ProtoUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


/**
 * @author inenakhov
 */
@SuppressWarnings({"MagicNumber", "RedundantThrows"})
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClutcherLogicTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String CATEGORY_DUMP_FILE_JSON = "all_models_278353.json";
    private static final String CATEGORY_DUMP_FILE_PB = "all_models_278353.pb";
    private static final int CATEGORY_ID = 278353;

    @Mock
    private CategoryTreeData categoryTreeData;

    @Spy
    private DefaultClutcher defaultClutcher;

    @Before
    public void setUp() throws Exception {
        String resourcesPath = ProtoUtils.fromJsonToProto(
                CATEGORY_DUMP_FILE_JSON, CATEGORY_DUMP_FILE_PB, temporaryFolder);
        String resourceFolder = Paths.get(resourcesPath).toString();

        StatShardKnowledge statShardKnowledge = new StatShardKnowledge(false,
                StatShardKnowledge.DEFAULT_SHARD_ID, shardId -> {
		    throw new UnsupportedOperationException();
		});

        StatJsonKnowledge statJsonKnowledge = new StatJsonKnowledge();
        statJsonKnowledge.setDataGetterDir(resourceFolder);
        statJsonKnowledge.setStatShardKnowledge(statShardKnowledge);

        statJsonKnowledge.reload();
        ProtoCategoryDao protoCategoryDao = new ProtoCategoryDao();
        protoCategoryDao.setDirPath(resourceFolder);
        protoCategoryDao.setStatJsonKnowledge(statJsonKnowledge);

        ClutcherKnowledge clutcherKnowledge = protoCategoryDao.reloadCategory(CATEGORY_ID, null);

        when(categoryTreeData.isAliasAllowedCategory(anyInt())).thenReturn(true);
        defaultClutcher = spy(new DefaultClutcher());
        defaultClutcher.setCategoryTreeData(categoryTreeData);
        when(defaultClutcher.getClutcherKnowledge(anyInt())).thenReturn(clutcherKnowledge);
        when(defaultClutcher.isAlive()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        Path path = Paths.get(CATEGORY_DUMP_FILE_PB);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    @Test
    public void logicTest() throws Exception {
        //Offer_id clustering
        DefaultClutcher.ClutchResult offerIdClutch = defaultClutcher.getClutch(278353,
                                                                        "77ceea980b38e097b4b6e6c28e1660ff",
                                                                        "title",
                                                                        "description",
                                                                        "",
                                                                        "16553",
                                                                        10887478);


        Assert.assertEquals(100137009985L, offerIdClutch.getClusterId());
        Assert.assertEquals(10887478, offerIdClutch.getVendorId());
        Assert.assertEquals(Clutcher.ClutchType.OFFER_ID_CLUTCH_OK, offerIdClutch.getClutchType());
        Assert.assertEquals(false, offerIdClutch.isPublishedInMbo());

        //Alias clustering
        DefaultClutcher.ClutchResult aliasClutch = defaultClutcher.getClutch(278353,
                                                                             "980b38e097b4b6e6c28e1660ff",
                                                                             "title 9785779308489",
                                                                             "description 9785779308489",
                                                                             "",
                                                                             "1655316553",
                                                                             12715369);


        Assert.assertEquals(100086, aliasClutch.getClusterId());
        Assert.assertEquals(12715369, aliasClutch.getVendorId());
        Assert.assertEquals(Clutcher.ClutchType.ALIAS_CLUTCH_OK, aliasClutch.getClutchType());
        Assert.assertEquals(true, aliasClutch.isPublishedInMbo());
    }
}
