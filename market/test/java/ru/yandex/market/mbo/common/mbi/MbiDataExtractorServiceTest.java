package ru.yandex.market.mbo.common.mbi;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Unit-тесты для {@link MbiDataExtractorService}.
 *
 * @author Vladislav Bauer
 */
@RunWith(Parameterized.class)
public class MbiDataExtractorServiceTest {

    private final String file;
    private final String expectedUrl;

    public MbiDataExtractorServiceTest(final String file, final String expectedUrl) {
        this.file = file;
        this.expectedUrl = expectedUrl;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][] {
            {
                MbiDataExtractorService.DELIVERY_SERVICES_FILE,
                "https://s3.mdst.yandex.net/market-mbi-dev/delivery-services/current_delivery-services.tsv"
            },
            {
                MbiDataExtractorService.CUT_OFF_SHOPS_FILE_NAME,
                "https://s3.mdst.yandex.net/market-mbi-dev/cutoffs/current_cutoffs.txt"
            },
            {
                MbiDataExtractorService.SHOPS_DAT_FILE,
                "https://s3.mdst.yandex.net/market-mbi-dev/shops/current_shops.dat"
            }
        };
    }

    @Test
    public void testGetFile() throws Exception {
        final String endpoint = "https://s3.mdst.yandex.net";
        final String bucket = "market-mbi-dev";
        final MbiDataExtractorService service = new MbiDataExtractorService(endpoint, bucket);

        Assert.assertThat(service.getFileURL(file).toString(), Matchers.equalTo(expectedUrl));
    }

}
