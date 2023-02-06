package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.puncher.PuncherClient;
import ru.yandex.market.tsum.clients.puncher.models.PuncherSuggest;
import ru.yandex.market.tsum.clients.puncher.models.PuncherTitle;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreatePuncherRulesJobTest {
    @InjectMocks
    private final CreatePuncherRulesJob job = new CreatePuncherRulesJob();

    @Mock
    Issue issue = BalancerPipelineTestFactory.getIssue();

    @Mock
    PuncherClient puncherClient;

    public CreatePuncherRulesJobTest() throws URISyntaxException {
    }

    @Test
    public void testParseRequestIdsOneComment() {
        assertEquals(
            Set.of(
                "5af57661d89cb016bb7d44ed",
                "5af57664d89cb016bb7d44ee"
            ),
            job.parseRequestIds(
                List.of(
                    "Заявки в puncher созданы\n" +
                        "https://puncher.yandex-team.ru/tasks?id=5af57661d89cb016bb7d44ed\n" +
                        "https://puncher.yandex-team.ru/tasks?id=5af57664d89cb016bb7d44ee\n" +
                        "<{PUNCHER_REQUEST_IDS_FOR_TICKET_PARSER_test1.vs.market.yandex.net\n" +
                        "5af57661d89cb016bb7d44ed\n" +
                        "5af57664d89cb016bb7d44ee\n" +
                        "}>\n" +
                        "**Подтвердите доступ вручную, перейдя по указанным выше ссылкам.**",
                    "Заявки в puncher созданы\n" +
                        "https://puncher.yandex-team.ru/tasks?id=5af57668d89cb016bb7d44ef\n" +
                        "<{PUNCHER_REQUEST_IDS_FOR_TICKET_PARSER_test2.vs.market.yandex.net\n" +
                        "5af57668d89cb016bb7d44ef\n" +
                        "}>\n" +
                        "**Подтвердите доступ вручную, перейдя по указанным выше ссылкам.**"
                ),
                "test1.vs.market.yandex.net"
            )
        );
    }

    @Test
    public void testParseRequestIdsTwoComments() {
        assertEquals(
            Set.of(
                "5af57661d89cb016bb7d44ed",
                "5af57664d89cb016bb7d44ee",
                "5af57668d89cb016bb7d44ef"
            ),
            job.parseRequestIds(
                List.of(
                    "Заявки в puncher созданы\n" +
                        "https://puncher.yandex-team.ru/tasks?id=5af57661d89cb016bb7d44ed\n" +
                        "https://puncher.yandex-team.ru/tasks?id=5af57664d89cb016bb7d44ee\n" +
                        "<{PUNCHER_REQUEST_IDS_FOR_TICKET_PARSER_test.vs.market.yandex.net\n" +
                        "5af57661d89cb016bb7d44ed\n" +
                        "5af57664d89cb016bb7d44ee\n" +
                        "}>\n" +
                        "**Подтвердите доступ вручную, перейдя по указанным выше ссылкам.**",
                    "Заявки в puncher созданы\n" +
                        "https://puncher.yandex-team.ru/tasks?id=5af57668d89cb016bb7d44ef\n" +
                        "<{PUNCHER_REQUEST_IDS_FOR_TICKET_PARSER_test.vs.market.yandex.net\n" +
                        "5af57668d89cb016bb7d44ef\n" +
                        "}>\n" +
                        "**Подтвердите доступ вручную, перейдя по указанным выше ссылкам.**"
                ),
                "test.vs.market.yandex.net"
            )
        );
    }

    @Test
    public void testGetPuncherMacrosNoFound() {
        when(issue.comment(anyString())).thenReturn(new Gson().fromJson("{\"id\": 12345}", Comment.class));
        when(puncherClient.suggestSources("Group 0")).thenReturn(Collections.emptyList());
        assertEquals(
            Collections.emptyList(),
            job.getPuncherMacros(List.of("Group 0"), issue)
        );
    }

    @Test
    public void testGetPuncherMacrosOnlyOne() {
        when(issue.comment(anyString())).thenReturn(new Gson().fromJson("{\"id\": 12345}", Comment.class));
        when(puncherClient.suggestSources("Group 1")).thenReturn(List.of(
            new PuncherSuggest("@group_1@", new PuncherTitle("Group 1"))
        ));
        assertEquals(
            List.of("@group_1@"),
            job.getPuncherMacros(List.of("Group 1"), issue)
        );
    }

    @Test
    public void testGetPuncherMacrosMultiple() {
        when(issue.comment(anyString())).thenReturn(new Gson().fromJson("{\"id\": 12345}", Comment.class));
        when(puncherClient.suggestSources("Group 2")).thenReturn(List.of(
            new PuncherSuggest("@group_2@", new PuncherTitle("Group 2")),
            new PuncherSuggest("@group_2_1@", new PuncherTitle("Sub Group 2"))
        ));
        assertEquals(
            List.of("@group_2@"),
            job.getPuncherMacros(List.of("Group 2"), issue)
        );
    }
}
