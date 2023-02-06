package ru.yandex.market.pers.qa.tms.imp.yandex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.AnswerFilter;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

public class YandexModelQuestionAnswerImportExecutorTest extends PersQaTmsTest {

    private static final String TEXT1 = "первый ответ";
    private static final String TEXT2 = "второй ответ";

    @Autowired
    YtClientProvider ytClientProvider;
    @Autowired
    @Qualifier("yqlJdbcTemplate")
    JdbcTemplate yqlJdbcTemplate;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    JdbcTemplate pgJdbcTemplate;
    @Autowired
    YandexModelQuestionAnswerImportExecutor executor;
    @Autowired
    QuestionService questionService;
    @Autowired
    AnswerService answerService;

    private List<Answer> prepareYandexVendorAnswers(long questionId1, long questionId2) {
        return Arrays.asList(
            Answer.buildBasicAnswer(YandexVendorRepliesImportExecutor.FAKE_USER, TEXT1, questionId1),
            Answer.buildBasicAnswer(YandexVendorRepliesImportExecutor.FAKE_USER, TEXT2, questionId2)
        );
    }

    @Test
    void testImportYandexAnswers() {
        YtClient ytClient = ytClientProvider.getDefaultClient();
        Mockito.when(ytClient.list(any())).thenReturn(Arrays.asList("1", "2", "3"), Collections.singletonList("3"),
            Arrays.asList("1_media", "2_media", "3_media"), Collections.singletonList("3_media"));
        pgJdbcTemplate.update("insert into qa.yandex_vendor_tables(type, table_name) values (?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?)",
            YandexImportType.ANSWER.getValue(), "1",
            YandexImportType.ANSWER.getValue(), "2",
            YandexImportType.COMMENT.getValue(), "3",
            YandexImportType.ANSWER.getValue(), "1_media",
            YandexImportType.ANSWER.getValue(), "2_media",
            YandexImportType.COMMENT.getValue(), "3_media");


        long questionId1 = questionService.createModelQuestion(12345L, "вопрос1", 123L).getId();
        long questionId2 = questionService.createModelQuestion(12345L, "вопрос1", 1234L).getId();
        List<Answer> expectedAnswers = prepareYandexVendorAnswers(questionId1, questionId2);
        Mockito.when(yqlJdbcTemplate.query(
            (String) argThat(argument -> {
                String arg = (String) argument;
                return arg.contains("3") && arg.contains(executor.getImportSubPath()) ||
                    arg.contains("3_media") && arg.contains(executor.getImportSubPath());
            }), any(RowMapper.class))).thenReturn(expectedAnswers);

        executor.importYandexAnswers();

        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("1")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("2")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("1_media")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("2_media")));

        List<Answer> vendorAnswers = answerService.getAnswers(new AnswerFilter().userUid(YandexVendorRepliesImportExecutor.FAKE_USER));
        Assertions.assertEquals(2, vendorAnswers.size());
        Answer answerForQuestion1 = vendorAnswers.get(0);
        Answer answerForQuestion2 = vendorAnswers.get(1);
        if (answerForQuestion1.getQuestionId() != questionId1) {
            answerForQuestion2 = answerForQuestion1;
            answerForQuestion1 = vendorAnswers.get(1);
        }
        Assertions.assertEquals(questionId1, answerForQuestion1.getQuestionId());
        Assertions.assertEquals(TEXT1, answerForQuestion1.getText());

        Assertions.assertEquals(questionId2, answerForQuestion2.getQuestionId());
        Assertions.assertEquals(TEXT2, answerForQuestion2.getText());

        Assertions.assertTrue(pgJdbcTemplate.queryForObject("select true from qa.yandex_vendor_tables where type = ? and table_name = ?", Boolean.class, YandexImportType.ANSWER.getValue(), "3"));
        Assertions.assertTrue(pgJdbcTemplate.queryForObject("select true from qa.yandex_vendor_tables where type = ? and table_name = ?", Boolean.class, YandexImportType.ANSWER.getValue(), "3_media"));
    }
}
