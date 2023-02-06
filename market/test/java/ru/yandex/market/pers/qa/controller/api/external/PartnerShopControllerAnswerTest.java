package ru.yandex.market.pers.qa.controller.api.external;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.mock.mvc.AnswerMvcMocks;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.QuestionService;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.qa.client.model.SortField.DATE;
import static ru.yandex.market.pers.qa.client.model.SortField.ID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.04.2019
 */
public class PartnerShopControllerAnswerTest extends QAControllerTest {
    private static final long SHOP_ID = 3458276452L;
    private static final long SHOP_ID_OTHER = 239859235L;

    private static final int FIRST_PAGE = 1;
    private static final int SECOND_PAGE = 2;

    @Autowired
    protected AnswerService answerService;
    @Autowired
    protected QuestionService questionService;
    @Autowired
    protected CommentService commentService;

    @Test
    void testCreatePartnerShopAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createPartnerShopAnswer(SHOP_ID, questionId, "test");

        // check internally created
        assertEquals(SHOP_ID, answerService.getAnswerById(answerId).getShopId().longValue());

        // check available externally
        List<AnswerDto> answers = getAnswersForPartnerShop(questionId, SHOP_ID, Sort.desc(ID));

        assertEquals(1, answers.size());
        assertEquals(SHOP_ID, answers.get(0).getShopId().longValue());
    }

    @Test
    void testCreatePartnerShopAnswerWithIllegalUrl() throws Exception {
        long questionId = createQuestion();
        createPartnerShopAnswer(SHOP_ID, UID, questionId, "test https://illegal.url.com/ test", status().is4xxClientError());
        createPartnerShopAnswer(SHOP_ID, UID, questionId, "test https://market.yandex.ru/legal and https://illegal.url.com/ test", status().is4xxClientError());

        long answerWithLegalMarketUrl =
            createPartnerShopAnswer(SHOP_ID, questionId, "test https://market.yandex.ru/ test");

        // check internally created
        assertEquals(SHOP_ID, answerService.getAnswerById(answerWithLegalMarketUrl).getShopId().longValue());
        // check available externally
        List<AnswerDto> answers = getAnswersForPartnerShop(questionId, SHOP_ID, Sort.desc(ID));

        assertEquals(1, answers.size());
        assertEquals(SHOP_ID, answers.get(0).getShopId().longValue());
    }

    @Test
    void testCreatePartnerShopAnswerWithUrl() throws Exception {
        long questionId = createQuestion();
        String result = createPartnerShopAnswer(
            SHOP_ID,
            UID,
            questionId,
            "Some text with t.com inside",
            status().is4xxClientError());

        assertTrue(result.contains("Text contains illegal URL"));
        assertTrue(result.contains("\"result\":58"));
        assertTrue(result.contains("\"code\":\"ILLEGAL_URL_FOUND\""));
    }

    @Test
    void testCreateNonPartnerShopAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId, "test");

        assertNull(answerService.getAnswerById(answerId).getShopId());
    }

    @Test
    void testLoadAnswerForPartnerShopSortingById() throws Exception {
        long questionId = createQuestion();

        // create some answers
        createAnswersAndReturnPublic(questionId);

        final Comparator<AnswerDto> idCmp = Comparator.comparing(AnswerDto::getAnswerId);

        // check id forward sort works well
        checkSorted(getAnswersForPartnerShop(questionId, SHOP_ID, Sort.asc(ID)), idCmp);

        // check id backward sort works well
        checkSorted(getAnswersForPartnerShop(questionId, SHOP_ID, Sort.desc(ID)), idCmp.reversed());
    }

    @Test
    void testLoadAnswerForPartnerShopSortingByDate() throws Exception {
        final int answerCount = 3;
        long questionId = createQuestion();

        // create 3 answers with different dates to strictly check sorting
        final Instant now = Instant.now();

        // create answers with date in descending order
        for (int i = 0; i < answerCount; i++) {
            final Long id = tryCreateAnswer(questionId, "Test answer + " + i);
            qaJdbcTemplate.update("update qa.question set cr_time = ? where id = ?",
                new java.sql.Timestamp(now.plus(answerCount - i, MINUTES).toEpochMilli()), id);
        }

        final Comparator<AnswerDto> dateCmp = Comparator.comparing(AnswerDto::getCreationDate);

        // check date forward sort works well
        checkSorted(getAnswersForPartnerShop(questionId, SHOP_ID, Sort.asc(DATE)), dateCmp);

        // check date backward sort works well
        checkSorted(getAnswersForPartnerShop(questionId, SHOP_ID, Sort.desc(DATE)), dateCmp.reversed());
    }

    @Test
    void testLoadAnswerForPartnerShopPaging() throws Exception {
        final int answerCountForTest = 13;
        final int pageSize = 6;

        final int pagesCount = 3; // = 13 div 6
        final int firstPageNum = 1;
        final int firstPageSize = 6;

        final int secondPageNum = 2;
        final int secondPageSize = 6;

        final int lastPageNum = 3;
        final int lastPageSize = 1;

        final Sort sort = Sort.asc(ID);
        final long questionId = createQuestion();

        final List<Long> answerIds = IntStream.range(0, answerCountForTest)
            .mapToObj(i -> tryCreateAnswer(questionId, "Test answer text " + i))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // check created sufficient count of entities
        assertEquals(answerCountForTest, answerIds.size());

        // check pages content
        final QAPager<AnswerDto> firstPage = getAnswersForPartnerShop(questionId,
            SHOP_ID,
            sort,
            firstPageNum,
            pageSize);
        checkPager(firstPage, firstPageNum, firstPageSize, pagesCount, answerCountForTest);
        assertTheSameAnswerIds(answerIds.subList(0, firstPageSize), firstPage.getData());

        final QAPager<AnswerDto> secondPage = getAnswersForPartnerShop(questionId,
            SHOP_ID,
            sort,
            secondPageNum,
            pageSize);
        checkPager(secondPage, secondPageNum, secondPageSize, pagesCount, answerCountForTest);
        assertTheSameAnswerIds(answerIds.subList(firstPageSize, firstPageSize + secondPageSize), secondPage.getData());

        final QAPager<AnswerDto> lastPage = getAnswersForPartnerShop(questionId, SHOP_ID, sort, lastPageNum, pageSize);
        checkPager(lastPage, lastPageNum, lastPageSize, pagesCount, answerCountForTest);
        assertTheSameAnswerIds(answerIds.subList(firstPageSize + secondPageSize, answerIds.size()), lastPage.getData());
    }

    @Test
    void testLoadAnswerForPartnerShopNegative() throws Exception {
        final int pageSize = 3;
        final int firstPageNum = 1;
        final Sort sort = Sort.asc(ID);
        final long questionId = createQuestion();

        // create some answers
        createAnswersAndReturnPublic(questionId);

        // ok to load first page
        assertEquals(pageSize,
            getAnswersForPartnerShop(questionId, SHOP_ID, sort, firstPageNum, pageSize).getData().size());

        // can't load negative or zero page
        assertThrows(AssertionError.class, () -> getAnswersForPartnerShop(questionId, SHOP_ID, sort, 0, pageSize));
        assertThrows(AssertionError.class, () -> getAnswersForPartnerShop(questionId, SHOP_ID, sort, -1, pageSize));

        // can't load negative or zero data count
        assertThrows(AssertionError.class, () -> getAnswersForPartnerShop(questionId, SHOP_ID, sort, 1, 0));
        assertThrows(AssertionError.class, () -> getAnswersForPartnerShop(questionId, SHOP_ID, sort, 1, -1));

        // can't load too many data
        assertThrows(AssertionError.class, () -> getAnswersForPartnerShop(questionId, SHOP_ID, sort, 1, 1000));
    }

    @Test
    void testRemovePartnerShopAnswer() throws Exception {
        final long questionId = createQuestion();

        final long answerId = createPartnerShopAnswer(SHOP_ID, questionId, "Partner answer");
        final long answerIdOld = createPartnerShopAnswer(SHOP_ID, questionId, "Partner answer very-very old");

        qaJdbcTemplate.update(
            "update qa.answer\n" +
                "set cr_time = now() - interval '12' day\n" +
                "where id = ?",
            answerIdOld
        );

        // should fail - can't delete partner answer from regular user (even with same UID)
        assertThrows(AssertionError.class, () -> deleteAnswer(answerId));

        // check can delete
        List<AnswerDto> partnerAnswers = getAnswersForPartnerShop(questionId, SHOP_ID, Sort.asc(ID));
        partnerAnswers.forEach(x ->
            assertTrue(x.isCanDelete())
        );

        // check delete is ok
        checkPartnerShopDeleteOk(SHOP_ID, answerId);
        checkPartnerShopDeleteOk(SHOP_ID, answerIdOld);
    }

    private void checkPartnerShopDeleteOk(long shopId, long answerId) throws Exception {
        assertEquals(State.NEW, answerService.getAnswerByIdInternal(answerId).getState());
        deletePartnerShopAnswer(answerId, shopId);
        assertEquals(State.DELETED, answerService.getAnswerByIdInternal(answerId).getState());
    }

    @Test
    void testEditPartnerShopAnswer() throws Exception {
        final long questionId = createQuestion();

        final long partnerAnswerId = createPartnerShopAnswer(SHOP_ID, questionId, "Partner answer");

        // check text is as given
        Answer partnerAnswer = answerService.getAnswerById(partnerAnswerId);
        assertEquals("Partner answer", partnerAnswer.getText());

        // try to edit to same value (ok)
        editPartnerShopAnswer(partnerAnswerId, SHOP_ID, "Partner answer");

        // try to change
        editPartnerShopAnswer(partnerAnswerId, SHOP_ID, "New updated text");

        // check text changed
        assertEquals("New updated text", answerService.getAnswerById(partnerAnswerId).getText());

        // try to change back to original
        editPartnerShopAnswer(partnerAnswerId, SHOP_ID, "Partner answer");

        // check text changed back
        assertEquals("Partner answer", answerService.getAnswerById(partnerAnswerId).getText());
    }

    @Test
    void testEditPartnerShopAnswerWithUrl() throws Exception {
        final long questionId = createQuestion();

        final long partnerAnswerId = createPartnerShopAnswer(SHOP_ID, questionId, "Partner answer");

        // try to edit to same value (ok)
        String result = editPartnerShopAnswer(partnerAnswerId, SHOP_ID, "Partner answer edited with http://some.ru/link", status().is4xxClientError());

        assertTrue(result.contains("Text contains illegal URL"));
        assertTrue(result.contains("\"result\":58"));
        assertTrue(result.contains("\"code\":\"ILLEGAL_URL_FOUND\""));
    }

    protected List<AnswerDto> getAnswersForPartnerShop(long questionId, long shopId, Sort sort) throws Exception {
        return getAnswersForPartnerShop(questionId, shopId, sort, FIRST_PAGE, DEF_PAGE_SIZE).getData();
    }

    protected QAPager<AnswerDto> getAnswersForPartnerShop(long questionId,
                                                          long shopId,
                                                          Sort sort,
                                                          int page,
                                                          int pageSize) throws Exception {
        return objectMapper.readValue(invokeAndRetrieveResponse(
            get("/partner/shop/" + shopId + "/question/" + questionId + "/answers")
                .param("userId", String.valueOf(UID))
                .param("sortField", sort.getField().toString())
                .param("asc", Boolean.toString(sort.isAscending()))
                .param("pageNum", String.valueOf(page))
                .param("pageSize", String.valueOf(pageSize)),
            status().is2xxSuccessful()
        ), new TypeReference<QAPager<AnswerDto>>() {
        });
    }

    protected void deletePartnerShopAnswer(long id, long shopId) throws Exception {
        invokeAndRetrieveResponse(
            delete(String.format("/partner/shop/%s/answer/%d", shopId, id))
                .param("userId", String.valueOf(UID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

    protected void editPartnerShopAnswer(long answerId, long shopId, String text) throws Exception {
        final String response = editPartnerShopAnswer(answerId, shopId, text, status().is2xxSuccessful());
    }

    protected String editPartnerShopAnswer(long answerId,
                                           long shopId,
                                           String text,
                                           ResultMatcher resultMatcher) throws Exception {
        return invokeAndRetrieveResponse(
            patch(String.format("/partner/shop/%s/answer/%s", shopId, answerId))
                .param("userId", String.valueOf(UID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(AnswerMvcMocks.ANSWER_BODY, text))
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

}
