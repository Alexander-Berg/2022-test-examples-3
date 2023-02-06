package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock.BlockType.DIMENSIONS;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock.BlockType.WEIGHT_NET;

public class MasterDataIntoBlocksSplitterImplTest {
    private MasterDataIntoBlocksSplitter splitter;

    @Before
    public void setUp() throws Exception {
        splitter = new MasterDataIntoBlocksSplitterImpl(TestMdmParamUtils.createParamCacheMock());
    }

    @Test
    public void testAllBlocksHaveDefaultSource() {
        // given
        EnhancedRandom enhancedRandom = TestDataUtils.defaultRandom("MasterData is the best thing ever!".hashCode());
        MasterData masterData = TestDataUtils.generateMasterData("123", 8, enhancedRandom);

        // when
        List<ItemBlock> blocks = splitter.splitIntoBlocks(masterData);

        // then
        Assertions.assertThat(blocks)
            // Фильтруем ВГХ
            .filteredOn(it -> it.blockType != WEIGHT_NET && it.blockType != DIMENSIONS)
            .map(ItemBlock::getSource)
            .containsOnly(MasterDataSource.DEFAULT_SOURCE);
    }
}
