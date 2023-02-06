package ru.yandex.market.partner.indexer;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.mbi.util.MbiMatchers.jsonPath;
import static ru.yandex.market.mbi.util.MbiMatchers.transformedBy;

/**
 * Функциональные тесты для {@link GetDatasourceIndexStateServantlet}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "csv/GetDatasourceIndexStateServantletTest.before.csv")
class GetDatasourceIndexStateServantletTest extends FunctionalTest {

    /**
     * Тест так же учитывает использование именно полного поколения, несмотря на наличие более свежих диффов.
     */
    @Test
    @DbUnitDataSet(before = "csv/totalOffersCountTest.before.csv")
    void totalOffersCountTest() {
        ResponseEntity<String> response = FunctionalTestHelper
                .get(baseUrl + "/getDatasourceIndexState?id={campaignId}&format=json", 10774L);

        assertOffersCount(response, 1050L, 200L, 850L);
    }

    @Test
    @DbUnitDataSet(before = "csv/totalOffersCountDuringImportTest.before.csv")
    void totalOffersCountDuringImportTest() {
        ResponseEntity<String> response = FunctionalTestHelper
                .get(baseUrl + "/getDatasourceIndexState?id={campaignId}&format=json", 10774L);

        assertOffersCount(response, 1050L, 500L, 550L);
    }

    private void assertOffersCount(ResponseEntity<String> response,
                                   long totalOffersCount, long cpcTotalOffers, long cpaTotalOffers) {
        assertThat(response.getBody(),
                jsonPath("$.result[0].totalOffersCount",
                        transformedBy(Long::parseLong, totalOffersCount)));
        assertThat(response.getBody(),
                jsonPath("$.result[0].cpcRealOffersCount",
                        transformedBy(Long::parseLong, cpcTotalOffers)));
        assertThat(response.getBody(),
                jsonPath("$.result[0].cpaRealOffersCount",
                        transformedBy(Long::parseLong, cpaTotalOffers)));
    }

}
