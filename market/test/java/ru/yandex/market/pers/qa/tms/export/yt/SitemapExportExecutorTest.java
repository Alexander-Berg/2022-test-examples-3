package ru.yandex.market.pers.qa.tms.export.yt;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Versus;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.VersusService;
import ru.yandex.market.pers.qa.tms.export.yt.model.SitemapEntity;
import ru.yandex.market.pers.qa.tms.export.yt.model.SitemapVersusEntity;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.pers.yt.YtClusterType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

class SitemapExportExecutorTest extends PersQaTmsTest {

    @Autowired
    SitemapExportExecutor sitemapExportExecutor;

    @Autowired
    YtClientProvider ytClientProvider;

    @Autowired
    QuestionService questionService;

    @Autowired
    private VersusService versusService;

    @Captor
    private ArgumentCaptor<List<?>> entitiesToUploadCaptor;

    @Captor
    private ArgumentCaptor<YPath> createTableCaptor;

    @Captor
    private ArgumentCaptor<YPath> createLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> currentLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> appendCaptor;

    @Test
    void testExportQaSitemap() {
        YtClient ytClient = ytClientProvider.getClient(YtClusterType.HAHN);

        int count = 10;
        for (int i = 0; i < count; i++) {
            questionService.createModelQuestion(i, "text" + i, count - i);
        }

        sitemapExportExecutor.exportQaSitemap();

        Mockito.verify(ytClient).createTable(isNull(), createTableCaptor.capture(), eq(SitemapEntity.tableSchema()));
        Mockito.verify(ytClient).append(isNull(), appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).updateLinks(createLinkCaptor.capture(), currentLinkCaptor.capture());

        Assertions.assertEquals(count, entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }

    @Test
    void testExportVersusSitemap() {
        YtClient ytClient = ytClientProvider.getClient(YtClusterType.HAHN);

        int count = 10;
        for (int i = 0; i < count; i++) {
            versusService.createVersus(Versus.byModels(i, i, i + 1));
        }

        sitemapExportExecutor.exportVersusSitemap();

        Mockito.verify(ytClient).createTable(isNull(), createTableCaptor.capture(), eq(SitemapVersusEntity.tableSchema()));
        Mockito.verify(ytClient).append(isNull(), appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).updateLinks(createLinkCaptor.capture(), currentLinkCaptor.capture());

        Assertions.assertEquals(count, entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }
}
