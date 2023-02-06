package ru.yandex.market.sre.services.tms.tasks.startrek;

import java.io.IOException;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultListF;
import ru.yandex.market.sre.services.tms.dao.entity.starttrek.ActionItemProcessing;
import ru.yandex.market.sre.services.tms.tasks.startrek.model.UserRefExt;
import ru.yandex.startrek.client.error.ForbiddenException;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.LocalLink;
import ru.yandex.startrek.client.model.Relationship;
import ru.yandex.startrek.client.model.SearchRequest;
import ru.yandex.startrek.client.model.UserRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActionItemsControlCronTaskTest extends StartekTestPreset {

    @InjectMocks
    protected ActionItemsControlCronTask actionItemsControlCronTask;

    ObjectMapper mapper;

    @Before
    public void setUp() {
        expectedException = ExpectedException.none();
        actionItemsControlCronTask.startrekSession = mockStartrekSession;
        actionItemsControlCronTask.startrekUiUrl = TEST_STARTEK_UI_LINK;
        actionItemsControlCronTask.incidentQueueName = TEST_INCIDENT_QUEUE_NAME;
        mapper = createMapper();
    }

    private Issue prepareIssue(String descriptionFile, String linksFile) throws IOException {
        Issue issue = mock(Issue.class);

        when(issue.getDescription()).thenReturn(Option.of(loadTextResource(descriptionFile)));
        when(issue.getKey()).thenReturn(StartekTestPreset.TEST_STARTREK_INC_ISSUE);
        when(issue.getTags()).thenReturn(Cf.list());
        if (linksFile == null) {
            return issue;
        }

        //add links to issue
        String linksAsJson = loadTextResource(linksFile);
        TypeReference<ListF<LocalLink>> typeRef = new TypeReference<ListF<LocalLink>>() {
        };
        ListF<LocalLink> links = mapper.readValue(linksAsJson, typeRef);
        when(issue.getLinks()).thenReturn(DefaultListF.wrap(links));
        return issue;
    }


    @Test
    public void cronExpression() {
        assertNotNull(actionItemsControlCronTask.cronExpression());
    }

    @Test
    public void prepareSearchRequest() {
        SearchRequest request = actionItemsControlCronTask.prepareSearchRequest();
        assertNotNull(request);
        assertTrue("Необходимо фильтровать по названию очереди",
                request.toString().contains(actionItemsControlCronTask.incidentQueueName));
    }

    /**
     * Инцидент No1, зависит от запроса No4
     * В ActionItem ссылка на запрос No4
     * Ожидаемый результат - добавление связи на инцидент No2
     * и добавление тэга PostMortemActionItemWithLosses
     */
    @Test
    public void processIssue_addsNewLink() throws IOException {
        Issue issue = prepareIssue("description/ActionItem-4.txt", "links/DependsOn4.txt");
        when(actionItemProcessingDao.getByKey(anyString())).thenReturn(new ActionItemProcessing(TEST_STARTREK_INC_ISSUE, Long.MAX_VALUE));
        actionItemsControlCronTask.processIssue(issue);
        Mockito.verify(issue, times(5)).getLinks();
        Mockito.verify(mockStartrekSession.links(), times(2)).create(eq(TEST_STARTREK_INC_ISSUE), endsWith("-2"),
                eq(Relationship.DEPENDS_ON));
        Mockito.verify(mockStartrekSession.issues(), times(1)).update(eq(issue), any(), eq(false), eq(false));
        //добавили тэг action-items-parsed
        Mockito.verify(mockStartrekSession.issues(), times(4)).get(TEST_STARTREK_REL_ISSUE2);
        Mockito.verify(mockStartrekSession.links(), times(0)).delete(any(), (LocalLink) any());
        verify(actionItemProcessingDao, times(1)).save(argThat(item -> TEST_STARTREK_INC_ISSUE.equals(item.getIssueKey()))); //сохранили в базу информацию
    }

    /**
     * Инцидент No1, зависит от запроса No3
     * В ActionItem пусто
     * Ожидаемый результат - новые связи не будут добавлены, старые не будут удалены
     */
    @Test
    public void processIssue_ignoresEmptyActions() throws IOException {
        Issue issue = prepareIssue("description/NoActionItemTag.txt", "links/DependsOn3.txt");
        actionItemsControlCronTask.processIssue(issue);
        Mockito.verify(issue, times(1)).getLinks();
        Mockito.verify(mockStartrekSession.links(), times(1)).create(any(), (String) any(), any());
        Mockito.verify(mockStartrekSession.issues(), times(1)).update(eq(issue), any(), eq(false), eq(false));
        //добавили тэг action-items-parsed
        Mockito.verify(mockStartrekSession.links(), times(0)).delete(any(), (LocalLink) any());
        verify(actionItemProcessingDao, times(1)).save(argThat(item -> TEST_STARTREK_INC_ISSUE.equals(item.getIssueKey()))); //сохранили в базу информацию
    }

    /**
     * Инцидент No1, зависит от запроса No3
     * В ActionItem ссылка на запрос No3
     * Ожидаемый результат - новые связи не будут добавлены, старые не будут удалены
     */
    @Test
    public void processIssue_ignoresGoodRelation() throws IOException {
        Issue issue = prepareIssue("description/ActionItem-3.txt", "links/DependsOn3.txt");
        actionItemsControlCronTask.processIssue(issue);
        Mockito.verify(issue, times(2)).getLinks();
        Mockito.verify(mockStartrekSession.links(), times(1)).create(any(), (String) any(), any());
        Mockito.verify(mockStartrekSession.issues(), times(1)).update(eq(issue), any(), eq(false), eq(false));
        //добавили тэг action-items-parsed
        Mockito.verify(mockStartrekSession.links(), times(0)).delete(any(), (LocalLink) any());
        verify(actionItemProcessingDao, times(1)).save(argThat(item -> TEST_STARTREK_INC_ISSUE.equals(item.getIssueKey()))); //сохранили в базу информацию
    }

    /**
     * Инцидент No1, блокирует запрос No3 и No4
     * В ActionItem запрос No3
     * Ожидаемый результат - тип свзяи No3 изменится с блока на 'зависит от'
     */
    @Test
    public void processIssue_changesBadRelation() throws IOException {
        Issue issue = prepareIssue("description/ActionItem-3.txt", "links/Blocks3And4.txt");
        actionItemsControlCronTask.processIssue(issue);
        Mockito.verify(issue, times(2)).getLinks();
        Mockito.verify(mockStartrekSession.links(), times(1)).create(eq(TEST_STARTREK_INC_ISSUE),
                eq(TEST_STARTREK_REL_ISSUE3), eq(Relationship.DEPENDS_ON));
        Mockito.verify(mockStartrekSession.issues(), times(1)).update(eq(issue), any(), eq(false), eq(false));
        //добавили тэг action-items-parsed
        Mockito.verify(mockStartrekSession.links(), times(1)).delete(eq(issue), (LocalLink) any());
        verify(actionItemProcessingDao, times(1)).save(argThat(item -> TEST_STARTREK_INC_ISSUE.equals(item.getIssueKey()))); //сохранили в базу информацию
    }

    /**
     * Инцидент No1, зависит от запроса No3
     * В ActionItem ссылка на запрос No2, но нет прав на создание связи
     * Ожидаемый результат - оповестит Давида и оветственного за запрос
     */
    @Test
    public void processIssue_notifiesWhenAccessDenied() throws IOException {
        Issue issue = prepareIssue("description/ActionItem-2.txt", "links/DependsOn3.txt");
        when(actionItemProcessingDao.getByKey(anyString())).thenReturn(new ActionItemProcessing(TEST_STARTREK_INC_ISSUE, Long.MAX_VALUE));
        ForbiddenException mockException = mock(ForbiddenException.class);
        when(mockStartrekSession.links().create(eq(TEST_STARTREK_INC_ISSUE), endsWith("-2"),
                eq(Relationship.DEPENDS_ON))).thenThrow(mockException);
        UserRef testAssignee = new UserRefExt(TEST_USER_LOGIN);
        when(issue.getAssignee()).thenReturn(Option.of(testAssignee));
        actionItemsControlCronTask.usersToNotify = TEST_USER_LOGIN;
        actionItemsControlCronTask.processIssue(issue);
        Mockito.verify(issue, times(2)).getLinks();
        Mockito.verify(mockStartrekSession.links(), times(1)).create(eq(TEST_STARTREK_INC_ISSUE), endsWith("-2"),
                eq(Relationship.DEPENDS_ON));
        Mockito.verify(mockStartrekSession.issues(), times(1)).update(eq(issue), any(), eq(false), eq(false));
        //добавили тэг action-items-parsed
        Mockito.verify(mockStartrekSession.issues(), times(2)).get(TEST_STARTREK_REL_ISSUE2);
        Mockito.verify(mockStartrekSession.links(), times(0)).delete(any(), (LocalLink) any());
        Mockito.verify(issue, times(1)).comment(anyString(), (UserRef) any());
        verify(actionItemProcessingDao, times(1)).save(argThat(item -> TEST_STARTREK_INC_ISSUE.equals(item.getIssueKey()))); //сохранили в базу информацию
    }

    @Test
    public void processIssue_notifiesWhenTagCreationDenied() throws IOException {
        Issue issue = prepareIssue("description/ActionItem-2.txt", "links/DependsOn3.txt");
        when(actionItemProcessingDao.getByKey(anyString())).thenReturn(new ActionItemProcessing(TEST_STARTREK_INC_ISSUE, Long.MAX_VALUE));
        ForbiddenException mockException = mock(ForbiddenException.class);
        when(mockStartrekSession.issues().update(eq(issue),
                argThat(update -> update.toString().contains("PostMortemActionItemWithLosses")),
                eq(false), eq(false))).thenThrow(mockException);
        UserRef testAssignee = new UserRefExt(TEST_USER_LOGIN);
        when(issue.getAssignee()).thenReturn(Option.of(testAssignee));
        actionItemsControlCronTask.usersToNotify = TEST_USER_LOGIN;
        when(mockStartrekSession.issues().get(TEST_STARTREK_REL_ISSUE2)).thenReturn(issue); //не очень красиво, но
        // для теста подходит
        actionItemsControlCronTask.processIssue(issue);
        Mockito.verify(issue, times(2)).getLinks();
        Mockito.verify(mockStartrekSession.issues(), times(2)).update(eq(issue), any(), eq(false), eq(false));
        //попытались добавить про факап и добавили тэг
        // action-items-parsed
        Mockito.verify(mockStartrekSession.issues(), times(1)).get(TEST_STARTREK_REL_ISSUE2);
        Mockito.verify(mockStartrekSession.links(), times(0)).delete(any(), (LocalLink) any());
        Mockito.verify(issue, times(1)).comment(contains("тэг"), (UserRef) any());
        verify(actionItemProcessingDao, times(1)).save(argThat(item -> TEST_STARTREK_INC_ISSUE.equals(item.getIssueKey()))); //сохранили в базу информацию
    }

    @Test
    public void regexp_test() {
        ActionItemsControlCronTask task = new ActionItemsControlCronTask(null);
        Matcher m = StartrekUtils.TICKET_PATTERN.matcher("text https://st.yandex-team.ru/MARKETINCIDENTS-4701 another" +
                " text");
        assertTrue(m.find());
        assertEquals("MARKETINCIDENTS-4701", m.group(1));

        m = StartrekUtils.TICKET_PATTERN.matcher("text MARKETINCIDENTS-4701 another text");
        assertTrue(m.find());
        assertEquals("MARKETINCIDENTS-4701", m.group(1));

        m = StartrekUtils.TICKET_PATTERN.matcher("text что-1 another text");
        assertFalse(m.find());

        m = StartrekUtils.TICKET_PATTERN.matcher("text chto-to another text");
        assertFalse(m.find());

        m = StartrekUtils.TICKET_PATTERN.matcher("text chto- 1 to another text");
        assertFalse(m.find());
    }
}
