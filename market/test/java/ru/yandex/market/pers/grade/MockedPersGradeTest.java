package ru.yandex.market.pers.grade;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.db.DbStGradeService;
import ru.yandex.market.pers.grade.core.ugc.ComplaintService;
import ru.yandex.market.util.ExecUtils;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         26.12.16
 */
@Import(PersGradeMockConfig.class)
public abstract class MockedPersGradeTest extends MockedTest {
    private static final String GRADE_VOTE_STATE_TEST_TABLE = "V_GRADE_VOTE_STAT_MVSRC";
    private static final String GRADE_VOTE_STATE_TEST_TABLE_PG = "V_GRADE_VOTE_STAT";

    protected static final int TEST_MAX_COMPLAINTS_CNT_A_DAY = 5;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DbStGradeService stGradeService;
    @Autowired
    private DbGradeVoteService voteService;
    @Autowired
    @Qualifier("mockedCacheMap")
    private Cache<String, Object> mockedCacheMap;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    protected ComplaintService complaintService;

    @Before
    public void initDbStGradeService() {
        // change votes view to load votes from view, but not from matview
        stGradeService.changeVoteStatViewPgForTests(GRADE_VOTE_STATE_TEST_TABLE_PG);
        voteService.changeVoteStatViewPgForTests(GRADE_VOTE_STATE_TEST_TABLE_PG);
    }

    @Before
    public void resetCache() {
        mockedCacheMap.invalidateAll();
    }

    protected void invoke(MockHttpServletRequestBuilder requestBuilder, ResultMatcher expected) throws Exception {
        mockMvc.perform(requestBuilder
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(expected)
            .andReturn().getResponse().getContentAsString();
    }

    protected String invokeAndRetrieveResponse(MockHttpServletRequestBuilder requestBuilder, ResultMatcher expected) {
        try {
            return mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(expected)
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw ExecUtils.silentError(e);
        }
    }

    protected <T> T parseValue(String content, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.readValue(content, valueTypeRef);
        } catch (IOException e) {
            throw ExecUtils.silentError(e);
        }
    }

    protected static String fileToString(String bodyFileName) throws IOException {
        return IOUtils.toString(MockedPersGradeTest.class.getResourceAsStream(bodyFileName), "UTF-8");
    }

}
