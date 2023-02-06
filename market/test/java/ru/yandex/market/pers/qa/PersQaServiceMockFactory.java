package ru.yandex.market.pers.qa;

import org.apache.http.client.HttpClient;
import org.jetbrains.annotations.NotNull;
import org.mockito.stubbing.Answer;
import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.dto.GradeForCommentDto;
import ru.yandex.market.pers.grade.client.dto.UserDto;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.qa.mock.NotifyServiceMockUtils;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.report.ReportService;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 21.06.18
 */
public class PersQaServiceMockFactory {
    public static void resetMocks() {
        PersTestMocksHolder.resetMocks();
    }

    public static ReportService reportServiceMock() {
        return PersTestMocksHolder.registerMock(ReportService.class, mock -> {
            when(mock.getModelsByIds(anyList())).then(invocation -> Collections.emptyMap());
        });
    }

    public static GradeClient gradeClientMock() {
        return PersTestMocksHolder.registerMock(GradeClient.class, client -> {
            // mock grade is a model grade
            // it allows to create any comment under it

            when(client.getGradeForComments(anyLong())).then(getModelBuilder(1, 1));
            when(client.getLastGradeForComments(anyLong())).then(getModelBuilder(1, 1));
            when(client.getGradesForComments(anyCollection())).thenReturn(List.of());
        });
    }

    public static void mockModelGradeWithFixId(GradeClient client, long gradeId, long userId, long modelId) {
        mockGradeWithFixId(client, gradeId, userId, GradeType.MODEL_GRADE, modelId);
    }

    public static void mockGradeSimple(GradeClient client, long gradeId, long userId, GradeType type, long modelId) {
        doMockGrade(client, gradeId, userId, type, modelId, false, null);
    }

    public static void mockGradeWithFixId(GradeClient client, long gradeId, long userId, GradeType type, long modelId) {
        doMockGrade(client, gradeId, userId, type, modelId, true, null);
    }

    public static void mockGradeWithFixId(GradeClient client, long gradeId, long userId, GradeType type, long modelId, Long fixId) {
        doMockGrade(client, gradeId, userId, type, modelId, true, fixId);
    }

    public static void mockGradeWithSameFixId(GradeClient client, long gradeId, long userId, GradeType type, long modelId) {
        doMockGrade(client, gradeId, userId, type, modelId, true, 100L);
    }

    public static void doMockGrade(GradeClient client,
                                   long gradeId,
                                   long userId,
                                   GradeType type,
                                   long modelId,
                                   boolean withFixId,
                                   Long fixId) {
        Map<Long, Long> mockedFixIds = PersTestMocksHolder.getAddition("grade_comment_mockedFixIds", HashMap::new);

        // this set of ids would be reset on on each test and when mocks are reset
        // this allows to keep results consistent (when fix_id is mocked - it works everywhere)
        if(withFixId) {
            mockedFixIds.put(gradeId, fixId);
        }

        Answer<Object> generator = invocation ->
            buildGrade(invocation.getArgument(0), userId, type, modelId, withFixId, fixId);
        when(client.getGradeForComments(eq(gradeId))).then(generator);
        when(client.getLastGradeForComments(eq(gradeId))).then(generator);

        when(client.getGradesForComments(anyCollection())).then(invocation -> {
            Collection<Long> data = invocation.getArgument(0);
            return data.stream()
                .filter(mockedFixIds::containsKey)
                .map(id -> buildGrade(id, userId, type, modelId, true, mockedFixIds.get(id)))
                .collect(Collectors.toList());
        });
    }

    @NotNull
    private static GradeForCommentDto buildGrade(long gradeId,
                                                 long userId,
                                                 GradeType type,
                                                 long modelId,
                                                 boolean addFixId,
                                                 Long fixId) {
        return new GradeForCommentDto(
            gradeId,
            addFixId ? (fixId != null ? fixId : getFixIdById(gradeId)) : null,
            type.value(),
            new UserDto(userId),
            modelId
        );
    }

    public static long getFixIdById(long id) {
        return id + 10_000_000_000L;
    }

    public static long getRealGradeId(long id) {
        return id > 10_000_000_000L ? id - 10_000_000_000L : id;
    }

    @NotNull
    public static Answer<Object> getModelBuilder(long userId, long modelId) {
        return invocation -> new GradeForCommentDto(
            invocation.getArgument(0),
            null,
            GradeType.MODEL_GRADE.value(),
            new UserDto(userId),
            modelId
        );
    }

    @NotNull
    public static Answer<Object> getShopBuilder(long userId, long shopId) {
        return invocation -> new GradeForCommentDto(
            invocation.getArgument(0),
            null,
            GradeType.SHOP_GRADE.value(),
            new UserDto(userId),
            shopId
        );
    }

    public static PersNotifyClient persNotifyClientMock() {
        return PersTestMocksHolder.registerMock(PersNotifyClient.class, NotifyServiceMockUtils::mockNoNeedToSubscribe);
    }

    public static HttpClient saasHttpClient() {
        return PersTestMocksHolder.registerMock(HttpClient.class, client -> {
            HttpClientMockUtils.mockResponse(client, req -> new ByteArrayInputStream(new byte[0]));
        });
    }

}
