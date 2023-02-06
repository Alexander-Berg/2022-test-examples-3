package ru.yandex.market.pers.qa.controller.api.external;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.client.dto.QAPager;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.AnswerDto;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.QaEntityAction;
import ru.yandex.market.pers.qa.model.Sort;
import ru.yandex.market.pers.qa.model.State;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.service.QuestionService;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.qa.client.model.SortField.DATE;
import static ru.yandex.market.pers.qa.client.model.SortField.ID;

/**
 * @author varvara
 * @author ilyakis
 * 28.08.2018
 */
public class VendorApiControllerAnswerTest extends QAControllerTest {

    @Autowired
    protected AnswerService answerService;
    @Autowired
    protected QuestionService questionService;
    @Autowired
    protected CommentService commentService;

    @Test
    void testCreateVendorAnswer() throws Exception {
        long questionId = createQuestion();
        long vendorId = 1243;
        long answerId = createVendorAnswer(vendorId, questionId, "42!!!!");

        long answerBrandId = answerService.getAnswerById(answerId).getBrandId();
        assertEquals(vendorId, answerBrandId);
    }

    @Test
    void testCreateNonVendorAnswer() throws Exception {
        long questionId = createQuestion();
        long answerId = createAnswer(questionId, "42!!!!");

        Long answerBrandId = qaJdbcTemplate.queryForObject(
            "select brand_id from qa.answer where id = ?",
            Long.class,
            answerId);
        assertNull(answerBrandId);
    }

    @Test
    void testLoadAnswerForVendorSortingById() throws Exception {
        final long brandId = 12345;
        long questionId = createQuestion();

        // create some answers
        createAnswersAndReturnPublic(questionId);

        final Comparator<AnswerDto> idCmp = Comparator.comparing(AnswerDto::getAnswerId);

        // check id forward sort works well
        checkSorted(getAnswersForVendor(questionId, brandId, ID, true), idCmp);

        // check id backward sort works well
        checkSorted(getAnswersForVendor(questionId, brandId, ID, false), idCmp.reversed());
    }

    @Test
    void testLoadAnswerForVendorSortingByDate() throws Exception {
        final long brandId = 12345;
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
        checkSorted(getAnswersForVendor(questionId, brandId, DATE, true), dateCmp);

        // check date backward sort works well
        checkSorted(getAnswersForVendor(questionId, brandId, DATE, false), dateCmp.reversed());
    }

    @Test
    void testLoadAnswerForVendorPaging() throws Exception {
        final long brandId = 12345;
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

        final List<Long> answerIds = IntStream.iterate(0, i -> i + 1).limit(answerCountForTest)
            .mapToObj(i -> tryCreateAnswer(questionId, "Test answer text " + i))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // check created sufficient count of entities
        assertEquals(answerCountForTest, answerIds.size());

        // check pages content
        final QAPager<AnswerDto> firstPage = getAnswersForVendor(questionId, brandId, sort, firstPageNum, pageSize);
        checkPager(firstPage, firstPageNum, firstPageSize, pagesCount, answerCountForTest);
        assertTheSameAnswerIds(answerIds.subList(0, firstPageSize), firstPage.getData());

        final QAPager<AnswerDto> secondPage = getAnswersForVendor(questionId, brandId, sort, secondPageNum, pageSize);
        checkPager(secondPage, secondPageNum, secondPageSize, pagesCount, answerCountForTest);
        assertTheSameAnswerIds(answerIds.subList(firstPageSize, firstPageSize + secondPageSize), secondPage.getData());

        final QAPager<AnswerDto> lastPage = getAnswersForVendor(questionId, brandId, sort, lastPageNum, pageSize);
        checkPager(lastPage, lastPageNum, lastPageSize, pagesCount, answerCountForTest);
        assertTheSameAnswerIds(answerIds.subList(firstPageSize + secondPageSize, answerIds.size()), lastPage.getData());
    }

    @Test
    void testLoadAnswerForVendorNegative() throws Exception {
        final long brandId = 12345;
        final int pageSize = 3;
        final int firstPageNum = 1;
        final Sort sort = Sort.asc(ID);
        final long questionId = createQuestion();

        // create some answers
        createAnswersAndReturnPublic(questionId);

        // ok to load first page
        assertEquals(pageSize, getAnswersForVendor(questionId, brandId, sort, firstPageNum, pageSize).getData().size());

        // can't load negative or zero page
        assertThrows(AssertionError.class, () -> getAnswersForVendor(questionId, brandId, sort, 0, pageSize));
        assertThrows(AssertionError.class, () -> getAnswersForVendor(questionId, brandId, sort, -1, pageSize));

        // can't load negative or zero data count
        assertThrows(AssertionError.class, () -> getAnswersForVendor(questionId, brandId, sort, 1, 0));
        assertThrows(AssertionError.class, () -> getAnswersForVendor(questionId, brandId, sort, 1, -1));

        // can't load too many data
        assertThrows(AssertionError.class, () -> getAnswersForVendor(questionId, brandId, sort, 1, 1000));
    }

    @Test
    void testRemoveVendorAnswer() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long answerId = createVendorAnswer(brandId, questionId, "Vendor answer");
        final long answerIdOld = createVendorAnswer(brandId, questionId, "Vendor answer very-very old");

        qaJdbcTemplate.update(
            "update qa.answer\n" +
                "set cr_time = now() - interval '12' day\n" +
                "where id = ?",
            answerIdOld
        );

        // should fail - can't delete vendor answer from regular user (even with same UID)
        assertThrows(AssertionError.class, () -> deleteAnswer(answerId));

        // check can delete
        QAPager<AnswerDto> vendorAnswers = getAnswersForVendor(questionId, brandId, Sort.asc(ID));
        vendorAnswers.getData().forEach(x ->
            assertTrue(x.isCanDelete())
        );

        // check delete is ok
        checkVendorDeleteOk(brandId, answerId);
        checkVendorDeleteOk(brandId, answerIdOld);
    }

    private void checkVendorDeleteOk(long brandId, long answerId) throws Exception {
        assertEquals(State.NEW, answerService.getAnswerByIdInternal(answerId).getState());
        deleteVendorAnswer(answerId, brandId);
        assertEquals(State.DELETED, answerService.getAnswerByIdInternal(answerId).getState());
    }

    @Test
    void testEditVendorAnswer() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long vendorAnswerId = createVendorAnswer(brandId, questionId, "Vendor answer");

        // check text is as given
        Answer vendorAnswer = answerService.getAnswerById(vendorAnswerId);
        assertEquals("Vendor answer", vendorAnswer.getText());

        // try to edit to same value (ok)
        editVendorAnswer(vendorAnswerId, brandId, "Vendor answer");

        // try to change
        editVendorAnswer(vendorAnswerId, brandId, "New updated text");

        // check text changed
        assertEquals("New updated text", answerService.getAnswerById(vendorAnswerId).getText());

        // try to change back to original
        editVendorAnswer(vendorAnswerId, brandId, "Vendor answer");

        // check text changed back
        assertEquals("Vendor answer", answerService.getAnswerById(vendorAnswerId).getText());
    }

    @Test
    void testVendorActivityBase() throws Exception {
        final long questionId = createQuestion();

        // no activity for new question
        assertEquals(0, questionService.getVendorActivity(questionId));

        createAnswer(questionId);

        // no vendor activity - answer is not from vendor
        assertEquals(0, questionService.getVendorActivity(questionId));
    }

    @Test
    void testVendorActivityByAnswer() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long vendorAnswerId = createVendorAnswer(brandId, questionId, "Vendor answer");

        // found activity
        assertEquals(1, questionService.getVendorActivity(questionId));

        answerService.deleteVendorAnswer(vendorAnswerId, brandId);

        // vendor answer removed -> again no activity
        assertEquals(0, questionService.getVendorActivity(questionId));
    }

    @Test
    void testVendorActivityByComment() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long answerId = createAnswer(questionId);

        // no vendor activity yet
        assertEquals(0, questionService.getVendorActivity(questionId));

        long commentId = commentService.createVendorQaComment(UID, UUID.randomUUID().toString(), answerId, brandId);

        // vendor answered
        assertEquals(1, questionService.getVendorActivity(questionId));

        commentService.banCommentByManager(commentId);

        // vendor answer removed -> again no activity
        assertEquals(0, questionService.getVendorActivity(questionId));
    }

    @Test
    void testVendorActivityByCommentOld() throws Exception {
        final int brandId = 24525;
        final long questionId = createQuestion();

        final long answerId = createAnswer(questionId);

        // no vendor activity yet
        assertEquals(0, questionService.getVendorActivity(questionId));

        long commentId = commentService.createVendorQaComment(UID, UUID.randomUUID().toString(), answerId, brandId);

        // vendor answered
        assertEquals(1, questionService.getVendorActivity(questionId));

        commentAnswerByVendor(commentId, answerId, brandId, QaEntityAction.SAVE);

        // vendor commented + got signal
        assertEquals(2, questionService.getVendorActivity(questionId));

        commentService.banCommentByManager(commentId);

        // vendor commented removed, but still 1 because of signal
        assertEquals(1, questionService.getVendorActivity(questionId));

        commentAnswerByVendor(commentId, answerId, brandId, QaEntityAction.REMOVE);

        // vendor answer removed, not signals -> again no activity
        assertEquals(0, questionService.getVendorActivity(questionId));
    }
}
