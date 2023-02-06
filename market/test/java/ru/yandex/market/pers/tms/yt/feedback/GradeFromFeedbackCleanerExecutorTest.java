package ru.yandex.market.pers.tms.yt.feedback;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.GradeSource;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.pers.tms.yt.feedback.GradeFromFeedbackCleanerExecutor.ACTIVE_KEY;

public class GradeFromFeedbackCleanerExecutorTest extends MockedPersTmsTest {
    private final long AUTHOR_ID = 123L;
    private final int SHOP_ID_1 = 321;
    private final int SHOP_ID_2 = 432;
    private final int SHOP_ID_3 = 543;
    private final int SHOP_ID_4 = 654;
    private final String ORDER_ID_1 = "1111";
    private final String ORDER_ID_2 = "2222";
    private final String ORDER_ID_3 = "3333";
    private final String ORDER_ID_4 = "4444";


    @Autowired
    ConfigurationService configurationService;
    @Autowired
    GradeFromFeedbackCleanerExecutor executor;
    @Autowired
    @Qualifier("ytJdbcTemplate")
    JdbcTemplate ytJdbcTemplate;
    @Autowired
    DbGradeService gradeService;
    @Autowired
    private GradeCreator gradeCreator;

    @Captor
    private ArgumentCaptor<ResultSetExtractor<Long>> captor;

    @Test
    public void testImportOff() throws Exception {
        configurationService.mergeValue(ACTIVE_KEY, "false");
        executor.runTmsJob();
        Mockito.verifyZeroInteractions(ytJdbcTemplate);
    }

    @Test
    public void testExecutor() throws Exception {
        long goodGradeNotExcludedId = createShopGrade(SHOP_ID_1, 2, ORDER_ID_1);
        long badGradeNotExcludedId = createShopGrade(SHOP_ID_2, 0, ORDER_ID_2);
        long goodGradeExcludedId = createShopGrade(SHOP_ID_3, 2, ORDER_ID_3);
        long badGradeExcludedId = createShopGrade(SHOP_ID_4, 0, ORDER_ID_4);

        Mockito.when(ytJdbcTemplate.queryForList(contains("partner_rating_exclusion"), eq(Long.class), eq(GradeSource.FEEDBACK.value())))
            .thenReturn(List.of(goodGradeExcludedId, badGradeExcludedId));

        configurationService.mergeValue(ACTIVE_KEY, "true");
        executor.runTmsJob();

        assertEquals(GradeState.LAST, gradeService.getGrade(goodGradeNotExcludedId).getState());
        assertEquals(GradeState.LAST, gradeService.getGrade(badGradeNotExcludedId).getState());
        assertEquals(GradeState.LAST, gradeService.getGrade(goodGradeExcludedId).getState());
        assertEquals(GradeState.DELETED, gradeService.getGrade(badGradeExcludedId).getState());
    }

    private long createShopGrade(int shop_id, int grade, String order_id) {
        ShopGrade shopGrade = new ShopGrade(AUTHOR_ID, shop_id, new Date());
        shopGrade.setGr0(grade);
        shopGrade.setOrderId(order_id);
        shopGrade.setRealSource(GradeSource.FEEDBACK.value());
        shopGrade.setSource(GradeSource.FEEDBACK.value());
        shopGrade.setModState(ModState.UNMODERATED);
        return gradeCreator.createGrade(shopGrade);
    }
}
