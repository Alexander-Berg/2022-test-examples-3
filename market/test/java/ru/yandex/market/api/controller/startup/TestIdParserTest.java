package ru.yandex.market.api.controller.startup;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.abtest.ClientActiveExperiment;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.UnitTestBase;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestIdParserTest extends UnitTestBase {

    private static final String TEST_ID_HEADER = "X-Test-Id";

    @Test
    public void coupleOfTests() {
        List<ClientActiveExperiment> parse = parse("111,112,alias1;222,223,;333,334");
        Assert.assertEquals(3, parse.size());
        ClientActiveExperiment actualSplit1 = parse.get(0);
        ClientActiveExperiment expectedSplit1 = new ClientActiveExperiment("111", "112", "alias1");
        assertSplit(expectedSplit1, actualSplit1);
        ClientActiveExperiment actualSplit2 = parse.get(1);
        ClientActiveExperiment expectedSplit2 = new ClientActiveExperiment("222", "223", null);
        assertSplit(expectedSplit2, actualSplit2);
        ClientActiveExperiment actualSplit3 = parse.get(2);
        ClientActiveExperiment expectedSplit3 = new ClientActiveExperiment("333", "334", null);
        assertSplit(expectedSplit3, actualSplit3);
    }

    @Test
    public void emptyHeader() {
        Collection<ClientActiveExperiment> parse = parse("");
        Assert.assertTrue(parse.isEmpty());
    }

    @Test
    public void ignoreTokensAfterAliasName() {
        List<ClientActiveExperiment> parse = parse("123,456,789,012");
        Assert.assertEquals(1, parse.size());
        ClientActiveExperiment actualSplit = parse.get(0);
        ClientActiveExperiment expectedSplit = new ClientActiveExperiment("123", "456", "789");
        assertSplit(expectedSplit, actualSplit);
    }

    @Test
    public void nullHeader() {
        Collection<ClientActiveExperiment> parse = parse(null);
        Assert.assertTrue(parse.isEmpty());
    }

    @Test
    public void singleValue() {
        List<ClientActiveExperiment> parse = parse("123,456");
        Assert.assertEquals(1, parse.size());
        ClientActiveExperiment split = parse.get(0);
        ClientActiveExperiment expected = new ClientActiveExperiment("123", "456", null);
        assertSplit(expected, split);
    }

    @Test
    public void tokenIdOrBucketIdIsEmpty_waitIgnoreSuchTest() {
        List<ClientActiveExperiment> parse = parse("123,,alias;,456,alias2;");
        Assert.assertEquals(0, parse.size());
    }

    @Test
    public void trash() {
        List<ClientActiveExperiment> parse = parse("trash");
        Assert.assertEquals(0, parse.size());
    }

    @Test
    public void trashBetweenTest() {
        List<ClientActiveExperiment> parse = parse("123,456;trash;789,012");
        Assert.assertEquals(2, parse.size());
        ClientActiveExperiment actualSplit1 = parse.get(0);
        ClientActiveExperiment expectedSplit1 = new ClientActiveExperiment("123", "456", null);
        assertSplit(expectedSplit1, actualSplit1);
        ClientActiveExperiment actualSplit2 = parse.get(1);
        ClientActiveExperiment expectedSplit2 = new ClientActiveExperiment("789", "012", null);
        assertSplit(expectedSplit2, actualSplit2);
    }

    private void assertSplit(ClientActiveExperiment expected, ClientActiveExperiment split) {
        Assert.assertEquals(expected.getTestId(), split.getTestId());
        Assert.assertEquals(expected.getBucket(), split.getBucket());
        Assert.assertEquals(expected.getAlias(), split.getAlias());
    }

    private List<ClientActiveExperiment> parse(String headerValue) {
        HttpServletRequest request = MockRequestBuilder.start()
            .header(TEST_ID_HEADER, headerValue)
            .build();

        Result<Collection<ClientActiveExperiment>, ValidationError> result = Parameters.AB_TEST_LIST_PARSER.get(request);
        Assert.assertTrue("error during parse header value", result.isOk());
        return new ArrayList<>(result.getValue());
    }

}
