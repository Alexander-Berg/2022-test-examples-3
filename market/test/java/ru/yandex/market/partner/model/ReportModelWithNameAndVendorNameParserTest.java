package ru.yandex.market.partner.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.core.model.ModelWithNameAndVendorName;
import ru.yandex.market.core.model.ReportModelWithNameAndVendorNameParser;

/**
 * @author zoom
 */
public class ReportModelWithNameAndVendorNameParserTest extends Assert {

    @Test
    public void shouldParseWell() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(getClass().getSimpleName() + ".xml")) {
            ReportModelWithNameAndVendorNameParser parser = new ReportModelWithNameAndVendorNameParser();
            parser.parse(inputStream);
            assertEquals(
                    Arrays.asList(
                            new ModelWithNameAndVendorName(7894171, "Philips SHE3590", "Philips"),
                            new ModelWithNameAndVendorName(8444109, "Nokia Lumia 820", "Nokia")),
                    parser.getResult());
        }
    }

}