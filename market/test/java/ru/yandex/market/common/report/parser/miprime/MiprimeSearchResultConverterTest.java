package ru.yandex.market.common.report.parser.miprime;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.common.report.model.json.miprime.MiprimeResult;
import ru.yandex.market.common.report.model.json.miprime.MiprimeSearch;
import ru.yandex.market.common.report.model.json.miprime.MiprimeSearchResult;
import ru.yandex.market.common.report.parser.json.MiprimeSearchResultParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class MiprimeSearchResultConverterTest {

    private static final String PATH = "/files/converter/prime_search_report_ONE.json";
    private static final MiprimeSearchResultParser PARSER = new MiprimeSearchResultParser();

    @Test
    public void testConvert() throws Exception {
        final MiprimeSearchResult result = convert(PATH);

        final Optional<MiprimeSearch> searchOptional = result.getSearch();
        Assert.assertTrue("Incorrect MiprimeSearch", searchOptional.isPresent());
        final MiprimeSearch miprimeSearch = searchOptional.get();

        final List<MiprimeResult> results = miprimeSearch.getResults();
        Assert.assertEquals("Incorrect result size", 1, results.size());

        final MiprimeResult miprimeResult = results.get(0);
        Assert.assertTrue(miprimeResult.isCutPrice());
    }

    private static MiprimeSearchResult convert(final String path) throws IOException {
        try (InputStream is = MiprimeSearchResultConverterTest.class.getResourceAsStream(path)) {
            return PARSER.parse(is);
        }
    }
}
