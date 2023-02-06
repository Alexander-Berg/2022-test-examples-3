package ru.yandex.market.billing.pp.storage;

import java.io.StringReader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.billing.pp.validation.EmptyPpCollectionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Проверка метода строящего копию контейнера ПП {@link PpDescriptionsAll} с фильтром по типу {@link MarketTypeMarker}.
 *
 * @author vbudnev
 */
class PpDescriptionsAllTest {

    private PpDescriptionsAll ppsAllTypes;

    @BeforeEach
    void before() {
        ppsAllTypes = PpJsonUtils.loadPpDescriptions(new StringReader(
                "{" +
                        "\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"root/nested/default\"," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"importance\":true" +
                        "}," +
                        "\"124\":{" +
                        "\"description\":\"someDescr1\"," +
                        "\"path\":\"root/nested/default/some_fake_end\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"marketType\":\"WHITE\"" +
                        "}," +
                        "\"125\":{" +
                        "\"description\":\"someDescr2\"," +
                        "\"path\":\"root/nested/default/some_fake_end2\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"marketType\":\"BLUE\"" +
                        "}," +
                        "\"126\":{" +
                        "\"description\":\"someDescr3\"," +
                        "\"path\":\"root/nested/default/some_fake_end3\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"marketType\":\"BLUE\"" +
                        "}," +
                        "\"127\":{" +
                        "\"description\":\"someDescr4\"," +
                        "\"path\":\"root/nested/default/some_fake_end4\"," +
                        "\"importance\":true," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"marketType\":\"RED\"" +
                        "}" +
                        "}"
        ));
    }

    @Test
    void test_copyWithRetainMarketType_when_white() {
        PpDescriptionsAll whitePps = ppsAllTypes.copyWithRetainMarketType(MarketTypeMarker.WHITE);
        assertThat(whitePps.getPpDescriptionById().size(), is(2));
        assertThat(whitePps.getPpDescriptionById().get(123).getPath(), is("root/nested"));
        assertThat(whitePps.getPpDescriptionById().get(124).getPath(), is("root/nested/default/some_fake_end"));
    }

    @Test
    void test_copyWithRetainMarketType_when_blue() {
        PpDescriptionsAll bluePps = ppsAllTypes.copyWithRetainMarketType(MarketTypeMarker.BLUE);
        assertThat(bluePps.getPpDescriptionById().size(), is(2));
        assertThat(bluePps.getPpDescriptionById().get(125).getPath(), is("root/nested/default/some_fake_end2"));
        assertThat(bluePps.getPpDescriptionById().get(126).getPath(), is("root/nested/default/some_fake_end3"));
    }

    @Test
    void test_copyWithRetainMarketType_when_red() {
        PpDescriptionsAll redPps = ppsAllTypes.copyWithRetainMarketType(MarketTypeMarker.RED);
        assertThat(redPps.getPpDescriptionById().size(), is(1));
        assertThat(redPps.getPpDescriptionById().get(127).getPath(), is("root/nested/default/some_fake_end4"));
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
    @Test
    void test_copyWithRetainMarketType_when_nothingLeft_then_throw() {
        final EmptyPpCollectionException ex = Assertions.assertThrows(EmptyPpCollectionException.class,
                () -> ppsAllTypes.copyWithRetainMarketType(MarketTypeMarker.RED)
                        .copyWithRetainMarketType(MarketTypeMarker.BLUE));

        Assertions.assertEquals("Retained collection is empty. Target type BLUE", ex.getMessage());
    }

    @Test
    void test_generalPayloadPrecondition() {
        assertThat(ppsAllTypes.getPpDescriptionById().size(), is(5));
        assertThat(ppsAllTypes.getPpDescriptionById().get(123).getPath(), is("root/nested"));
        assertThat(ppsAllTypes.getPpDescriptionById().get(124).getPath(), is("root/nested/default/some_fake_end"));
        assertThat(ppsAllTypes.getPpDescriptionById().get(125).getPath(), is("root/nested/default/some_fake_end2"));
        assertThat(ppsAllTypes.getPpDescriptionById().get(126).getPath(), is("root/nested/default/some_fake_end3"));
        assertThat(ppsAllTypes.getPpDescriptionById().get(127).getPath(), is("root/nested/default/some_fake_end4"));
    }

    @Test
    void test_getRawDescriptions() {
        PpDescriptionsAll allTypes = PpJsonUtils.loadPpDescriptionsRaw(new StringReader(
                "{" +
                        "\"123\":{" +
                        "\"description\":\"someDesc\"," +
                        "\"path\":\"root/nested/default\"," +
                        "\"validFor\": [\"CLICKS\"]," +
                        "\"importance\":true" +
                        "}}"));

        PpDescription ppDescription = allTypes.getPpDescriptionById().get(123);
        assertThat(ppDescription.getPath(), is("root/nested/default"));
    }

}
