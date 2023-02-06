package ru.yandex.market.pers.yt;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.01.2021
 */
public class YtCleanerServiceTest {
    private static final String SITEMAP_DIR = "sitemap";
    private static final String QUESTIONS_DIR = "questions";

    private static final String LINK_CURRENT = "current";
    private static final String LINK_MONDAY = "monday";
    private static final String LINK_THURSDAY = "thursday";

    private YtCleanerService ytCleanerService;
    private YtClient ytClient;

    @BeforeEach
    public void init() {
        ytCleanerService = new YtCleanerService(Map.of(
            QUESTIONS_DIR, 2
        ));
        ytClient = mock(YtClient.class);
        YtClientMocks.baseMock(ytClient);
    }

    @Test
    void clean() {
        YPath basePath = YPath.cypressRoot().child("testPath");

        String currentLinked = "5";
        String mondayLinked = "d";
        String thursdayLinked = "e";

        when(ytClient.list(any())).thenReturn(
            Arrays.asList(SITEMAP_DIR, QUESTIONS_DIR),
            Arrays.asList("1", "2", "3", "4", currentLinked, LINK_CURRENT),
            Arrays.asList("a", "b", "c", mondayLinked, thursdayLinked, LINK_MONDAY, LINK_THURSDAY));

        when(ytClient.exists(basePath)).thenReturn(true);
        when(ytClient.exists(basePath.child(SITEMAP_DIR))).thenReturn(true);
        when(ytClient.exists(basePath.child(QUESTIONS_DIR))).thenReturn(true);

        when(ytClient.dereferenceLink(any()))
            .thenAnswer(invocation -> {
                YPath path = invocation.getArgument(0);
                String linkName = path.name();
                String value = null;
                if (linkName.equals(LINK_CURRENT)) {
                    value = currentLinked;
                } else if (linkName.equals(LINK_MONDAY)) {
                    value = mondayLinked;
                } else if (linkName.equals(LINK_THURSDAY)) {
                    value = thursdayLinked;
                } else {
                    return null;
                }

                return path.parent().child(value);
            });

        ytCleanerService.cleanAllSubDirectories(ytClient, basePath);

        // all sitemaps removed, except for last + linked + link
        for (int i = 1; i < 4; i++) {
            String tableName = String.valueOf(i);
            verify(ytClient).remove(argThat(argument -> argument.name().equals(tableName)));
        }

        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals("4")));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals(currentLinked)));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals(LINK_CURRENT)));

        // keed last 4 items + links for questions
        for (char i = 'a'; i < 'b'; i++) {
            String tableName = String.valueOf(i);
            verify(ytClient).remove(argThat(argument -> argument.name().equals(tableName)));
        }

        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals("b")));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals("c")));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals(mondayLinked)));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals(thursdayLinked)));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals(LINK_MONDAY)));
        verify(ytClient, never()).remove(argThat(argument -> argument.name().equals(LINK_THURSDAY)));
    }
}
