package ru.yandex.market.pers.qa.tms.export.yt;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class YtExportCleanerExecutorTest extends PersQaTmsTest {

    @Autowired
    private YtClientProvider ytClientProvider;

    @Autowired
    private YtExportCleanerExecutor ytExportCleanerExecutor;

    @Test
    void clean() {
        // more complex tests in yt-client lib

        //when
        YtExportCleanerExecutor.CLEAN_CLUSTERS.forEach(cluster -> {
            YtClient ytClient = ytClientProvider.getClient(cluster);
            //noinspection unchecked
            when(ytClient.list(any())).thenReturn(
                Arrays.asList(SitemapExportExecutor.SITEMAP_PATH, NewQuestionExportExecutor.NEW_QUESTIONS_PATH),
                Arrays.asList("1", "2", "3", "4"),
                Arrays.asList("a", "b", "c", "d", "e"));

            when(ytClient.exists(any())).thenReturn(true);
        });

        ytExportCleanerExecutor.clean();

        //then
        YtExportCleanerExecutor.CLEAN_CLUSTERS.forEach(cluster -> {
            YtClient ytClient = ytClientProvider.getClient(cluster);

            for (int i = 1; i < 4; i++) {
                String tableName = String.valueOf(i);
                Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals(tableName)));
            }

            Mockito.verify(ytClient, Mockito.never()).remove(argThat(argument -> argument.name().equals("4")));

            for (char i = 'a'; i < 'c'; i++) {
                String tableName = String.valueOf(i);
                Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals(tableName)));
            }

            Mockito.verify(ytClient, Mockito.never()).remove(argThat(argument -> argument.name().equals("d")));
            Mockito.verify(ytClient, Mockito.never()).remove(argThat(argument -> argument.name().equals("e")));
        });
    }
}
