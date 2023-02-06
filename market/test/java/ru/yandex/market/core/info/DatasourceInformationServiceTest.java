package ru.yandex.market.core.info;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.ds.info.DatasourceInformationService;
import ru.yandex.market.core.ds.info.ShopInformation;
import ru.yandex.market.core.ds.info.UniShopInformation;
import ru.yandex.market.core.testing.ShopProgram;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест на логику работы {@link ru.yandex.market.core.ds.info.DatasourceInformationService}
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "DatasourceInformationServiceTest.before.csv")
public class DatasourceInformationServiceTest extends FunctionalTest {

    @Autowired
    private DatasourceInformationService datasourceInformationService;

    /**
     * Проверяем, что наличие незаполненного GENERAL-параметра (domain) влияет и на CPA, и на CPC.
     */
    @Test
    public void testMissedShopParams() {
        List<UniShopInformation> cpaMissedParams = datasourceInformationService.getMissedDatasourceInfo(775,
                ShopProgram.CPA);
        List<UniShopInformation> cpcMissedParams = datasourceInformationService.getMissedDatasourceInfo(775,
                ShopProgram.CPC);
        assertTrue(cpaMissedParams.size() == 1);
        assertTrue(cpcMissedParams.size() == 1);
        assertTrue(cpcMissedParams.contains(new UniShopInformation("datasource-domain", true)));

        cpaMissedParams = datasourceInformationService.getMissedDatasourceInfo(776, ShopProgram.CPA);
        cpcMissedParams = datasourceInformationService.getMissedDatasourceInfo(776, ShopProgram.CPC);
        assertTrue(cpcMissedParams.isEmpty());
        assertTrue(cpaMissedParams.isEmpty());

        cpaMissedParams = datasourceInformationService.getMissedDatasourceInfo(778, ShopProgram.CPA);
        cpcMissedParams = datasourceInformationService.getMissedDatasourceInfo(778, ShopProgram.CPC);
        assertTrue(cpaMissedParams.size() == 1);
        assertTrue(cpaMissedParams.contains(new UniShopInformation("feed-missed")));
        assertTrue(cpcMissedParams.size() == 1);
        assertTrue(cpcMissedParams.contains(new UniShopInformation("feed-missed")));
    }

    /**
     * Проверяем корректность выдачи массового метода
     * {@link DatasourceInformationService#getMissedDatasourceInfo(java.util.Collection)}
     */
    @Test
    public void testBulkMissedShopParams() {

        //Проверить пустой список
        assertTrue(datasourceInformationService.getMissedDatasourceInfo(Collections.emptyList()).isEmpty());

        //Проверить на данных
        assertThat(
                datasourceInformationService.getMissedDatasourceInfo(Arrays.asList(774L, 775L, 776L)).entrySet(),
                everyItem(isIn(Stream.of(
                        new Object[]{775L, Arrays.asList(
                                new UniShopInformation("datasource-domain", true))
                        },
                        new Object[]{778L, Collections.singletonList(new UniShopInformation("feed-missed"))}
                ).collect(Collectors.toMap(o -> (long) o[0], o -> (List<ShopInformation>) o[1])).entrySet()))
        );
    }
}
