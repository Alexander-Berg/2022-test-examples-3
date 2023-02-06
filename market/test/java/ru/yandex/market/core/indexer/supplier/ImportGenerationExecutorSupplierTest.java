package ru.yandex.market.core.indexer.supplier;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.indexer.db.generation.DbGenerationService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.indexer.IndexerService;
import ru.yandex.market.indexer.listener.UpdateSupplierMainFeedStatusListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Проверяем импорт данных фида поставщика.
 */
class ImportGenerationExecutorSupplierTest extends AbstractImportGenerationExecutorTest {

    @Autowired
    private DbGenerationService generationService;

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    private UpdateSupplierMainFeedStatusListener updateSupplierMainFeedStatusListener;

    @BeforeEach
    void setUp() {
        initImportGenerationsExecutor(
                generationService,
                List.of(updateSupplierMainFeedStatusListener),
                IndexerService.IndexerType.MAIN
        );
    }

    /**
     * Проверяем, что состояние фида поставщика из поколения индексатора сохранится в базу.
     */
    @Test
    @DbUnitDataSet(
            before = "importGenerationExecutorSupplierTest.before.csv",
            after = "importGenerationExecutorSupplierTest.after.csv" // ie no changes
    )
    void testSupplierFeedLoad() throws Exception {
        checkFeedIndxLoad("ok");

        var queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(paramService).setSystemParam(eq(ParamType.IS_IN_INDEX), queryCaptor.capture());
        verify(paramService, never()).setSystemParam(eq(ParamType.IS_IN_TEST_INDEX), anyString());
        assertThat(queryCaptor.getValue())
                .as("надо смотреть на линки, правильные feed id там")
                .containsIgnoringCase("SHOPS_WEB.PARTNER_FF_SERVICE_LINK")
                .containsIgnoringCase("SHOPS_WEB.SUPPLIER_FEED_STATE")
                .containsIgnoringCase("SHOPS_WEB.V_LAST_GENERATION");
    }

    /**
     * Проверяем, что не упадем на индексации со статусом новая
     */
    @Test
    @DbUnitDataSet(
            before = "importGenerationExecutorSupplierTest.before.csv",
            after = "importGenerationExecutorSupplierTest.before.csv"
    )
    void testSupplierFeedLoadNewIndexer() throws Exception {
        checkFeedIndxLoad("new");

        verify(paramService, never()).setSystemParam(eq(ParamType.IS_IN_INDEX), anyString());
        verify(paramService, never()).setSystemParam(eq(ParamType.IS_IN_TEST_INDEX), anyString());
    }

    /**
     * Проверяем, что при наличии хотя бы одного валидного оффера создается тикет на модерацию в або
     */
    @Test
    @DbUnitDataSet(before = "importGenerationExecutorDropshipModerationTest.before.csv",
            after = "importGenerationExecutorDropshipModerationTest.after.csv")
    void testDropshipStartModeration() {
        checkFeedIndxLoad("ok");
        verify(aboPublicRestClient, times(1))
                .startSupplierModeration(eq(465984L), eq(RatingPartnerType.DROPSHIP));
    }

    /**
     * Проверяем, что не создаем тикет на модерацию в СКК, если партнер удален
     */
    @Test
    @DbUnitDataSet(before = "importGenerationExecutorDropshipModerationTestDeletedPartner.before.csv")
    void testNoAssortmentCheckWhenDeletedPartner() {
        checkFeedIndxLoad("ok");
        verify(aboPublicRestClient, never()).startSupplierModeration(anyLong(), eq(RatingPartnerType.DROPSHIP));
    }

    /**
     * Проверяем, что если проверка ассортимента уже была запрошена, она не перезапросится еще раз
     */
    @Test
    @DbUnitDataSet(before = "importGenerationExecutorDropshipModerationTest2.before.csv")
    void testAssortmentCheckNotInitiatedWhenHasAlreadyTakenPlace() {
        checkFeedIndxLoad("ok");
        verify(aboPublicRestClient, never()).startSupplierModeration(anyLong(), eq(RatingPartnerType.DROPSHIP));
    }

    /**
     * Проверяем, что если маркетплейс фича зафейлена, то проверка ассортимента не запрашивается
     */
    @Test
    @DbUnitDataSet(before = "importGenerationExecutorDropshipModerationTest3.before.csv")
    void testAssortmentCheckNotInitiatedWhenMarketplaceIsFailed() {
        checkFeedIndxLoad("ok");
        verify(aboPublicRestClient, never()).startSupplierModeration(anyLong(), eq(RatingPartnerType.DROPSHIP));
    }
}
