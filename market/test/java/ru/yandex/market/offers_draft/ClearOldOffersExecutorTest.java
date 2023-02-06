package ru.yandex.market.offers_draft;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.quartz.JobExecutionContext;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.market.core.yt.indexer.YtFactory;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ClearOldOffersExecutorTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    @Test
    public void clearTest() {
        LocalDate nowDate = LocalDate.now();
        LocalDate oldDate = LocalDate.now()
                .minus(31, ChronoUnit.DAYS);

        YtTemplate mockYtTemplate = mockedYtTemplate(
                invocation -> {
                    String catalog = invocation.getArgument(0).toString();
                    Assert.assertEquals("//home/market/testing/mbi/offers", catalog);
                    return new ArrayListF<>(List.of(
                            new YTreeStringNodeImpl("offers_".concat(nowDate.format(FORMATTER)), null),
                            new YTreeStringNodeImpl("metadata_".concat(oldDate.format(FORMATTER)), null)
                    ));
                },
                invocation -> {
                    String date = invocation.getArgument(0).toString();
                    Assert.assertEquals("//home/market/testing/mbi/offers/".concat("metadata_".concat(oldDate.format(FORMATTER))), date);
                    return null;
                });

        YtFactory ytFactory = mock(YtFactory.class);
        doReturn("hahn")
                .when(ytFactory).getYtCluster("stratocaster");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("offersCatalog", "//home/market/testing/mbi/offers");
        Map<String, YtTemplate> ytTemplateMap = new HashMap<>();
        ytTemplateMap.put("stratocaster", mockYtTemplate);
        ClearOldOffersExecutor clearOldOffersExecutor =
                new ClearOldOffersExecutor(parameters, ytTemplateMap, Collections.singletonList("stratocaster"));
        clearOldOffersExecutor.doJob(mock(JobExecutionContext.class));

        verify(mockYtTemplate).runInYt(any());
    }

    private YtTemplate mockedYtTemplate(Answer list, Answer remove) {
        Yt ytMock = mock(Yt.class);
        Cypress cypressMock = mock(Cypress.class);
        doReturn(cypressMock)
                .when(ytMock).cypress();
        doAnswer(list).when(cypressMock).list(any(YPath.class));
        doAnswer(remove).when(cypressMock).remove(any(YPath.class));

        return spy(new YtTemplate(new YtCluster[]{
                new YtCluster(".", ytMock)
        }));
    }
}
