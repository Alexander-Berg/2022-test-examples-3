package ru.yandex.market.pers.tms.timer.article;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.mbo.cms.ArticleAuthor;
import ru.yandex.market.mbo.cms.ArticleInfo;
import ru.yandex.market.mbo.cms.ArticleInfoWrapper;
import ru.yandex.market.mbo.cms.Entrypoint;
import ru.yandex.market.pers.grade.core.article.model.ArticleModState;
import ru.yandex.market.pers.grade.core.article.service.ArticleModerationService;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

public class ArticlePublishExecutorTest extends MockedPersTmsTest {
    private static final String TICKET_KEY1 = "MARKETRED-1";
    private static final String TICKET_KEY2 = "MARKETRED-2";
    private static final Issue ISSUE1 = new Issue("1", null, TICKET_KEY1, null, 1, new EmptyMap(), null);
    private static final Issue ISSUE2 = new Issue("2", null, TICKET_KEY2, null, 1, new EmptyMap(), null);
    private static final Long FAKE_USER_ID1 = 123L;
    private static final Long FAKE_USER_ID2 = 1234L;

    @Autowired
    ArticlePublishExecutor articlePublishExecutor;

    @Autowired
    ArticleModerationService articleModerationService;

    @Autowired
    StartrekService startrekService;

    @Autowired
    @Qualifier("mboCardRestTemplate")
    RestTemplate restTemplate;

    @Autowired
    PersNotifyClient persNotifyClient;

    private Long startModerationModerateAndGetModerationId(long articleId, int revision, long fakeUser) {
        articleModerationService.startArticleModeration(articleId, revision, fakeUser);
        Long id = pgJdbcTemplate.queryForObject("select id from article_moderation where article_id = ? and revision = " +
                "?", Long.class, articleId, revision);
        articleModerationService.moderate(id, articleId, revision, ArticleModState.APPROVED, 0, 1);
        return id;
    }

    void mockPersNotifyClient() throws PersNotifyClientException {
        Email activeEmail = new Email("active@email.ru", true);
        Mockito.when(persNotifyClient.getEmails(anyLong())).thenReturn(Collections.singleton(activeEmail),
                Collections.emptySet());
    }

    @Test
    public void testPublish() throws Exception {
        mockPersNotifyClient();
        long articleSuccessfulPublication = 1;
        long articleUnsuccessfulPublication = 2;
        long articleSuccessfulPublicationWithoutEmail = 3;
        int revision = 1;
        Mockito.when(restTemplate.getForEntity(anyString(), any(), anyMap())).thenReturn(ResponseEntity.badRequest().build());
        Long successArticleModerationId = startModerationModerateAndGetModerationId(articleSuccessfulPublication,
                revision, FAKE_USER_ID1);
        Long unsuccessArticleModerationId = startModerationModerateAndGetModerationId(articleUnsuccessfulPublication,
                revision, FAKE_USER_ID1);
        Long successArticleModerationIdWithoutEmail =
                startModerationModerateAndGetModerationId(articleSuccessfulPublicationWithoutEmail, revision,
                        FAKE_USER_ID2);

        mockMboCmsOnPublishMethod(Sets.newHashSet(articleSuccessfulPublication,
                articleSuccessfulPublicationWithoutEmail),
                Collections.singleton(articleUnsuccessfulPublication));
        mockMboCmsOnGetInfoMethod();
        startrekServiceSuccessAnswer(articleSuccessfulPublication, ISSUE1);
        startrekServiceSuccessAnswer(articleSuccessfulPublicationWithoutEmail, ISSUE2);
        startrekServiceUnsuccess(articleUnsuccessfulPublication);
        long unpublishedCount = pgJdbcTemplate.queryForObject(
                "select count(*) from article_published where published is null",
                Long.class);
        Assert.assertEquals("Must be 3 unpublished articles", 3L, unpublishedCount);
        try {
            articlePublishExecutor.runTmsJob();
            Assert.fail("exception wasn't thrown");
        } catch (RuntimeException e) {
            Assert.assertEquals("1 exception(s) found. First exception: Unable to create ticket", e.getMessage());
        }
        checkSuccessfulPublication(successArticleModerationId);
        checkSuccessfulPublication(successArticleModerationIdWithoutEmail);
        checkUnsuccessfulPublication(unsuccessArticleModerationId);

        checkSuccessfulTicketCreation(successArticleModerationId, TICKET_KEY1);
        checkSuccessfulTicketCreation(successArticleModerationIdWithoutEmail, TICKET_KEY2);
        checkUnsuccessfulTicketCreation(unsuccessArticleModerationId);
    }

    private void startrekServiceSuccessAnswer(long articleSuccessfulPublication, Issue issue) {
        Mockito.when(startrekService.createTicket(argThat(new ArgumentMatcher<IssueCreate>() {
            @Override
            public boolean matches(IssueCreate o) {
                return o != null && ((String) ((IssueCreate) o).getValues().getOrElse("description", "")).contains(
                        "articleId=" + articleSuccessfulPublication);
            }
        }))).thenReturn(issue);
    }

    private void startrekServiceUnsuccess(long articleUnsuccessfulPublication) {
        Mockito.when(startrekService.createTicket(argThat(new ArgumentMatcher<IssueCreate>() {
            @Override
            public boolean matches(IssueCreate o) {
                return o != null && ((String) ((IssueCreate) o).getValues().getOrElse("description", "")).contains(
                        "articleId=" + articleUnsuccessfulPublication);
            }
        }))).thenThrow(new RuntimeException("Unable to create ticket"));
    }

    private void checkSuccessfulTicketCreation(Long successArticleModerationId, String expectedTicketKey) {
        long created = pgJdbcTemplate.queryForObject(
                "select created from article_ticket where article_moderation_id = ?",
                Long.class, successArticleModerationId);
        Assert.assertEquals("Ticket for this article must be created", 1, created);

        String ticketKey = pgJdbcTemplate.queryForObject(
                "select ticket_key from article_ticket where article_moderation_id = ?",
                String.class, successArticleModerationId);
        Assert.assertEquals(expectedTicketKey, ticketKey);
    }

    private void checkUnsuccessfulTicketCreation(Long unsuccessArticleModerationId) {
        try {
            pgJdbcTemplate.queryForObject(
                    "select created from article_ticket where article_moderation_id = ?",
                    Long.class, unsuccessArticleModerationId);
            Assert.fail("Exception expected");
        } catch (EmptyResultDataAccessException ignored) {
        }
    }

    private void checkSuccessfulPublication(Long successArticleModerationId) {
        long published = pgJdbcTemplate.queryForObject(
                "select published from article_published where article_moderation_id = ?",
                Long.class, successArticleModerationId);
        Assert.assertEquals("This article must be published", 1, published);
    }

    private void checkUnsuccessfulPublication(Long unsuccessArticleModerationId) {
        Long unpublished = pgJdbcTemplate.queryForObject(
                "select published from article_published where article_moderation_id = ?",
                Long.class, unsuccessArticleModerationId);
        Assert.assertNull("This article must not be published", unpublished);
    }

    private void mockMboCmsOnGetInfoMethod() {
        Mockito.when(restTemplate.getForEntity(
                argThat((ArgumentMatcher<String>) o ->
                        Optional.ofNullable(o).map(s -> s.contains("export")).orElse(false)), any()))
                .thenAnswer(invocation -> ResponseEntity.ok(createArticleInfoWrapper()));
    }

    private ArticleInfoWrapper createArticleInfoWrapper() {
        ArticleAuthor articleAuthor = new ArticleAuthor(12345L);
        Entrypoint entrypoint = new Entrypoint("hub", articleAuthor);
        ArticleInfo articleInfo = new ArticleInfo(123, "UGC статья", "type", "semanticId",
                Collections.singletonList(entrypoint));
        return new ArticleInfoWrapper(Collections.singletonList(articleInfo));
    }

    private void mockMboCmsOnPublishMethod(Set<Long> successfulPublicationArticles,
                                           Set<Long> unsuccessfulPublicationArticles) {
        Mockito.when(restTemplate.exchange(
                argThat((ArgumentMatcher<String>) o ->
                        Optional.ofNullable(o).map(s -> s.contains("editor")).orElse(false)),
                eq(HttpMethod.GET), ArgumentMatchers.isNull(),
                any(ParameterizedTypeReference.class))).thenAnswer(invocation -> {

            List<NameValuePair> queryParams = new URIBuilder((String) invocation.getArgument(0)).getQueryParams();
            long article = queryParams.stream()
                    .filter(nameValuePair -> "page-id".equals(nameValuePair.getName()))
                    .map(NameValuePair::getValue)
                    .map(Long::valueOf)
                    .findAny()
                    .orElseThrow(IllegalStateException::new);
            if (successfulPublicationArticles.contains(article)) {
                return ResponseEntity.ok().build();
            } else if (unsuccessfulPublicationArticles.contains(article)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        });
    }

}
