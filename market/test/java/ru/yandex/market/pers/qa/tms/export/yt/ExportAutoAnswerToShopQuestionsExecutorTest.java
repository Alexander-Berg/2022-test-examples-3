package ru.yandex.market.pers.qa.tms.export.yt;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Answer;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.tms.export.yt.model.AutoAnswerView;
import ru.yandex.market.pers.qa.tms.questions.AutoAnswerToShopQuestionsExecutor;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.util.db.ConfigurationService;
import ru.yandex.yt.ytclient.tables.TableSchema;

class ExportAutoAnswerToShopQuestionsExecutorTest extends PersQaTmsTest {

    @Autowired
    private ExportAutoAnswerToShopQuestionsExecutor exportAutoAnswerToShopQuestionsExecutor;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private YtClientProvider ytClientProvider;

    @Captor
    private ArgumentCaptor<List<?>> listArgumentCaptor;
    @Captor
    private ArgumentCaptor<YPath> objectYPathArgumentCaptor1;
    @Captor
    private ArgumentCaptor<YPath> objectYPathArgumentCaptor2;
    @Captor
    private ArgumentCaptor<TableSchema> tableSchemaArgumentCaptor;

    @Value("${pers.market.public.url}")
    private String marketUrl;

    private YtClient ytClient;

    @Override
    protected void resetMocks() {
        super.resetMocks();
        MockitoAnnotations.initMocks(this);
        ytClient = ytClientProvider.getClient(YtClusterType.HAHN);
    }

    @Test
    void exportAutoAnswerToYt() {
        long questionAuthor = 123;
        long modelId = 1234;
        String answerText = "answer_text";
        String questionText = "question_text";
        Question question = questionService.createModelQuestion(questionAuthor, questionText, modelId);
        Answer answer = answerService.createAnswer(AutoAnswerToShopQuestionsExecutor.FAKE_USER, answerText, question.getId());

        exportAutoAnswerToShopQuestionsExecutor.exportAutoAnswerToYt();
        Mockito.verify(ytClient).createTable(objectYPathArgumentCaptor1.capture(), tableSchemaArgumentCaptor.capture());
        Mockito.verify(ytClient).append(objectYPathArgumentCaptor2.capture(), listArgumentCaptor.capture());

        Assertions.assertEquals(objectYPathArgumentCaptor1.getValue(), objectYPathArgumentCaptor2.getValue());
        Assertions.assertEquals(AutoAnswerView.getTableSchema(), tableSchemaArgumentCaptor.getValue());
        Assertions.assertEquals(answer.getId(), (long) configurationService.getValueAsLong(ExportAutoAnswerToShopQuestionsExecutor.AUTO_ANSWER_LAST_EXPORTED_ID_KEY));
        Assertions.assertEquals(1, listArgumentCaptor.getValue().size());
        AutoAnswerView autoAnswerView = (AutoAnswerView) listArgumentCaptor.getValue().get(0);
        Assertions.assertEquals(answer.getId(), autoAnswerView.getAnswerId());
        Assertions.assertEquals(answer.getQuestionId(), autoAnswerView.getQuestionId());
        Assertions.assertEquals(answerText, autoAnswerView.getAnswerText());
        Assertions.assertEquals(questionText, autoAnswerView.getQuestionText());
        Assertions.assertEquals(String.format(marketUrl + "/product/%d/question/%d", modelId, question.getId()), autoAnswerView.getQuestionUrl());

        question = questionService.createModelQuestion(questionAuthor, "another_question_text", modelId);
        answer = answerService.createAnswer(AutoAnswerToShopQuestionsExecutor.FAKE_USER, "answer_text", question.getId());
        exportAutoAnswerToShopQuestionsExecutor.exportAutoAnswerToYt();
        Assertions.assertEquals(answer.getId(), (long) configurationService.getValueAsLong(ExportAutoAnswerToShopQuestionsExecutor.AUTO_ANSWER_LAST_EXPORTED_ID_KEY));
    }
}
