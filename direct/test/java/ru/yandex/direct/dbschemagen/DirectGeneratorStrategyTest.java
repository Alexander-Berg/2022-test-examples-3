package ru.yandex.direct.dbschemagen;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(Parameterized.class)
public class DirectGeneratorStrategyTest {
    @Parameterized.Parameter(0)
    public String origName;

    @Parameterized.Parameter(1)
    public String expectedResult;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Object[][] testData() {
        return new String[][]{
                {"bid", "BID"},
                {"BannerID", "BANNER_ID"},
                {"statusShow", "STATUS_SHOW"},
                {"VCard_Id", "VCARD_ID"},
                {"banner_type", "BANNER_TYPE"},
        };
    }

    @Test
    public void test() throws Exception {
        assertThat(DirectGeneratorStrategy.makeUnderscoreName(origName)).isEqualTo(expectedResult);
    }
}
