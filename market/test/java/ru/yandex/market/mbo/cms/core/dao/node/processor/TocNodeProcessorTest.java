package ru.yandex.market.mbo.cms.core.dao.node.processor;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.ClientUtil;

/**
 * @author sergtru
 * @since 23.05.2018
 */
public class TocNodeProcessorTest {
    @Test
    public void testTransliterate() {
        Assert.assertEquals("Zhukov-FR-35", ClientUtil.transliterate("Жуков_FR-35"));
    }

    @Test
    public void testTransliterateCaps() {
        //maybe we want LUCHSHE here?
        Assert.assertEquals("LUChShE", ClientUtil.transliterate("ЛУЧШЕ"));
    }
}
