package ru.yandex.market.pers.tms.moderation;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.dto.CleanWebResponseDto;
import ru.yandex.market.cleanweb.dto.VerdictDto;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.author.AuthorService;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.GradeCleanWebService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.02.2019
 */
public abstract class AbstractAutoModerationTest extends MockedPersTmsTest {

    protected static final long AUTHOR_ID = -1116L;
    protected static final String COMMENT_TEXT =
        "чрезвычайно длинный комментарий, несомненно больше, чем положено настройками автоматической модерации. " +
            "К сожалению, в нём есть немного \"мат\"-ерных слов. С этим придётся как-то смириться.";


    @Autowired
    protected CleanWebClient cleanWebClient;

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    @Qualifier("shopNames4Moderation")
    private Supplier<List<String>> shopNamePatterns;

    @Autowired
    protected AuthorService authorService;

    @Autowired
    protected DbGradeAdminService dbGradeAdminService;

    @Autowired
    protected GradeModeratorModificationProxy gradeModeratorModificationProxy;

    protected void mockCleanWebVerdict(long gradeId) {
        commonMockCleanWebVerdict(gradeId, new VerdictDto[]{});
    }

    protected void mockCleanWebVerdict(long gradeId, String verdictValue) {
        VerdictDto verdictDto = new VerdictDto();
        verdictDto.setKey(String.valueOf(gradeId));
        verdictDto.setName("text_auto_obscene");
        verdictDto.setValue(verdictValue);

        commonMockCleanWebVerdict(gradeId, new VerdictDto[]{verdictDto});
    }

    private void commonMockCleanWebVerdict(long gradeId, VerdictDto[] verdictDtos) {
        CleanWebResponseDto dto = new CleanWebResponseDto(String.valueOf(gradeId), verdictDtos);
        configurationService.tryGetOrMergeVal(GradeCleanWebService.ENABLE_KEY, Boolean.class, true);
        when(cleanWebClient.sendContent(anyList(), anyBoolean())).thenReturn(new CleanWebResponseDto[]{dto});
    }

    protected void mockShopNames4Moderation(String shopNamePattern) {
        when(shopNamePatterns.get()).thenReturn(singletonList(shopNamePattern));
    }

    protected ModState getGradeModState(long gradeId) {
        return ModState.byValue(pgJdbcTemplate.queryForObject("SELECT mod_state FROM grade WHERE id=" + gradeId,
            Integer.class));
    }

    protected void checkGradeModState(ModState modState, Long gradeId) {
        final int modStateFromDb =
            pgJdbcTemplate.queryForObject("SELECT mod_state FROM grade WHERE id = " + (long) gradeId,
            Integer.class);
        assertEquals(modState.value(), modStateFromDb);
    }

    protected String getGradeFailedFilterDescription(long gradeId) {
        return pgJdbcTemplate.queryForObject(
                "SELECT filter_description FROM auto_mod_result WHERE grade_id = " + gradeId,
                String.class
        );
    }

    protected List<String> getGradesFailedFilterDescription(long gradeId) {
        return pgJdbcTemplate.queryForList(
                "SELECT filter_description FROM auto_mod_result WHERE grade_id = " + gradeId,
                String.class
        );
    }

    protected void makeGradeOlder(Long gradeId) {
        pgJdbcTemplate.update("UPDATE grade SET cr_time = now() - make_interval(days := 1) WHERE id = " + gradeId);
    }

    protected void makeModerationOlder(Long gradeId) {
        pgJdbcTemplate.update(
            "update mod_grade_last mgl set mod_time = mgl.mod_time - ? * interval '1' minute where grade_id = ?",
            AbstractAutomoderation.MOD_TIME_INTERVAL_MINUTES + 1, gradeId
        );
    }

    protected void validateIndexingQueueIsEmpty(Long gradeId) {
        int count = pgJdbcTemplate.queryForObject("select count(*) as cnt from grade.saas_indexing i where i.grade_id = ?", int.class, gradeId);
        assertEquals(0, count);
    }
}
