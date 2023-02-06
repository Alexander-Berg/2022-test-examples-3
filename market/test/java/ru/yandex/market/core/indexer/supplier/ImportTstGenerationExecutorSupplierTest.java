package ru.yandex.market.core.indexer.supplier;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.indexer.db.generation.DbGenerationService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.indexer.IndexerService;
import ru.yandex.market.indexer.listener.UpdatePlainshiftSupplierFeedStatusListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImportTstGenerationExecutorSupplierTest extends AbstractImportGenerationExecutorTest {
    @Autowired
    private DbGenerationService testGenerationService;

    @Autowired
    private UpdatePlainshiftSupplierFeedStatusListener plainshiftSupplierFeedStatusListener;

    @BeforeEach
    void setUp() {
        initImportGenerationsExecutor(
                testGenerationService,
                List.of(plainshiftSupplierFeedStatusListener),
                IndexerService.IndexerType.PLANESHIFT
        );
    }

    /**
     * Проверяем, что состояние фида поставщика из тестового поколения индексатора сохранится в базу.
     */
    @Test
    @DbUnitDataSet(
            before = "importGenerationExecutorSupplierTest.before.csv",
            after = "importTstGenerationExecutorSupplierTest.after.csv"
    )
    void testSupplierFeedLoadTestGeneration() throws Exception {
        checkFeedIndxLoad("ok");

        var queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(paramService).setSystemParam(eq(ParamType.IS_IN_TEST_INDEX), queryCaptor.capture());
        verify(paramService, never()).setSystemParam(eq(ParamType.IS_IN_INDEX), anyString());
        assertThat(queryCaptor.getValue())
                .as("надо смотреть на линки, правильные feed id там")
                .containsIgnoringCase("SHOPS_WEB.PARTNER_FF_SERVICE_LINK")
                .containsIgnoringCase("SHOPS_WEB.TEST_SUPPLIER_FEED_STATE")
                .containsIgnoringCase("SHOPS_WEB.V_LAST_TEST_GENERATION");
    }

}
