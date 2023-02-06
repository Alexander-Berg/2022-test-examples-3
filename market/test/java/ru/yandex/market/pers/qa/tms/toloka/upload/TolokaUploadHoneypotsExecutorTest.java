package ru.yandex.market.pers.qa.tms.toloka.upload;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.service.AnswerService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.tms.toloka.TolokaTestsBase;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.report.ReportService;
import ru.yandex.yt.ytclient.tables.TableSchema;

abstract class TolokaUploadHoneypotsExecutorTest extends TolokaTestsBase {
    @Autowired
    QuestionService questionService;

    @Autowired
    AnswerService answerService;

    @Autowired
    ReportService reportService;

    @Autowired
    YtClientProvider ytClientProvider;

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

    Random rnd = ThreadLocalRandom.current();

    @Override
    protected void resetMocks() {
        super.resetMocks();

        MockitoAnnotations.initMocks(this);
    }

}
