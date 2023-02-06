package ru.yandex.market.pers.qa.tms.toloka.upload;

import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.tms.toloka.TolokaTestsBase;
import ru.yandex.market.report.ReportService;
import ru.yandex.yt.ytclient.tables.TableSchema;

abstract class TolokaUploadTasksExecutorTest extends TolokaTestsBase {
    static final int MOD1 = 13;
    static final int MOD2 = 17;

    @Autowired
    ReportService reportService;
    @Autowired
    QuestionService questionService;
    @Autowired
    AnswerService answerService;

    @Captor
    ArgumentCaptor<GUID> txArgumentCaptor1;
    @Captor
    ArgumentCaptor<GUID> txArgumentCaptor2;
    @Captor
    ArgumentCaptor<List<?>> listArgumentCaptor;
    @Captor
    ArgumentCaptor<YPath> objectYPathArgumentCaptor1;
    @Captor
    ArgumentCaptor<YPath> objectYPathArgumentCaptor2;
    @Captor
    ArgumentCaptor<TableSchema> tableSchemaArgumentCaptor;

    @Override
    protected void resetMocks() {
        super.resetMocks();

        MockitoAnnotations.initMocks(this);
    }
}
