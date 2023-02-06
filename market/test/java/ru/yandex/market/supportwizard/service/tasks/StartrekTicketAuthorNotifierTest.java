package ru.yandex.market.supportwizard.service.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.market.supportwizard.SimpleUserRef;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.CollectionUpdate;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.IssueUpdate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link StartrekTicketAuthorNotifier}.
 *
 * @author Vadim Lyalin
 */
public class StartrekTicketAuthorNotifierTest extends BaseFunctionalTest {
    private static final int ISSUES_COUNT = 12;

    @Autowired
    private StartrekTicketAuthorNotifier mbiBlueSupportTailNotifier;

    @Autowired
    private Session startrekSession;

    @Test
    void test() {
        Issues issues = mock(Issues.class);
        ArgumentCaptor<IssueRef> issueRefCaptor = ArgumentCaptor.forClass(IssueRef.class);
        ArgumentCaptor<IssueUpdate> issueUpdateCaptor = ArgumentCaptor.forClass(IssueUpdate.class);
        when(startrekSession.issues()).thenReturn(issues);
        when(issues.update(issueRefCaptor.capture(), issueUpdateCaptor.capture())).thenReturn(null);

        List<Issue> issueList = getIssues(startrekSession);
        IteratorF<Issue> iteratorF = DefaultIteratorF.wrap(issueList.iterator());
        when(issues.find(anyString())).thenReturn(iteratorF);

        mbiBlueSupportTailNotifier.doJob(null);

        // Проверяем, что апдейтятся тикеты MBI-2 и MBI-1
        assertThat(issueRefCaptor.getAllValues(), Matchers.contains(issueList.get(1), issueList.get(0)));

        assertThat(issueUpdateCaptor.getAllValues(), hasSize(2));
        IssueUpdate issueUpdate = issueUpdateCaptor.getAllValues().get(0);
        // проверяем, что добавляем коммент
        CommentCreate commentCreate = issueUpdate.getComment().get();
        assertThat(commentCreate.getComment().get(), is("" +
                "Сейчас ваш тикет 11-й в очереди. До его разрешения минимум 21 день. Чтобы решшить его быстрее, " +
                "измените вес вместе с продуктовым менеджером @fedvasek. Список конкурирующих тикетов можно " +
                "посмотреть по ссылке https://st.yandex-team.ru/dashboard/30226"));
        // проверяем, что добавляем тег
        CollectionUpdate<String> tags = (CollectionUpdate<String>) issueUpdate.getValues().getO("tags").get();
        ListF<String> setTags = tags.getSet();
        assertThat(setTags, hasSize(1));
        assertThat(setTags.get(0), is("support_tail"));

        verify(startrekSession, times(3)).issues();
        verify(issues, times(2)).update(any(IssueRef.class), any(IssueUpdate.class));
        verify(issues, times(1)).find(anyString());

        verifyNoMoreInteractions(startrekSession, issues);
    }

    /**
     * @return возвращает тикеты MBI-1 ... MBI-12 с весами 1 ... 12
     */
    private List<Issue> getIssues(Session session) {
        return IntStream.range(1, ISSUES_COUNT + 1)
                .mapToObj(i -> {
                    Map<String, Object> values = Map.of(
                            "createdBy", new SimpleUserRef("pupkin"),
                            "weight", Option.of((double) i)
                    );
                    return new Issue("MBI-" + i, null, null, null, 0, DefaultMapF.wrap(values), session);
                })
                .collect(toList());
    }
}
