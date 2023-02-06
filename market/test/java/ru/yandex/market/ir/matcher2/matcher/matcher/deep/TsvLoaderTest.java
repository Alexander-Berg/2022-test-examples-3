package ru.yandex.market.ir.matcher2.matcher.matcher.deep;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.deep.DeepMatchIndexElem;
import ru.yandex.market.ir.matcher2.matcher.deep.TsvLoader;
import ru.yandex.market.ir.matcher2.matcher.utils.FileUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author inenakhov
 */
public class TsvLoaderTest {
    @Test
    public void load() throws Exception {
        Path pathToFile = Paths.get(FileUtil.getAbsolutePath("/deep/smart_matcher_index_90490.tsv"));
        Map<String, DeepMatchIndexElem> loaded = TsvLoader.load(pathToFile);
        assertEquals(4, loaded.size());

        DeepMatchIndexElem firstLoaded = loaded.get("6d76c10ba8c40201591a78f16396be68");
        DeepMatchIndexElem firstExpected = new DeepMatchIndexElem(90490, 4974329, 0.5228402878113174, 4974329, "PSKU");
        assertEquals(firstExpected, firstLoaded);

        DeepMatchIndexElem secondLoaded = loaded.get("7217225acd0ed4f48bc132ec5e60d500");
        DeepMatchIndexElem secondExpected = new DeepMatchIndexElem(90490, 11892261, 0.7678904339192484, 4974329, "PSKU");
        assertEquals(secondExpected, secondLoaded);
    }
}
