package ru.yandex.market.mboc.common.services.tracker;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.excel.ExcelFileUtils;
import ru.yandex.market.mbo.tracker.TrackerService;
import ru.yandex.market.mbo.tracker.TrackerServiceImpl;
import ru.yandex.market.mbo.tracker.models.ExportAttachment;
import ru.yandex.market.mbo.tracker.models.ImportAttachment;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.services.offers.tracker.OffersTrackerService.TOTAL_OFFERS;

/**
 * Интеграционные тесты для тестирования ${@link TrackerServiceImpl}.
 *
 * @author s-ermakov
 */
@RunWith(SpringRunner.class)
@Ignore("Тесты нужно переделать, чтобы не так сильно нагружать трекер кучей ненужных тикетов")
public class TrackerServiceTest extends BaseIntegrationTestClass {
    private static final Logger log = LoggerFactory.getLogger(TrackerServiceTest.class);

    private static final String SUPPLIER = "integration-test-supplier";
    private static final String TITLE = "Test ticket";
    private static final String DESCRIPTION = "Test ticket";

    private static final long SLEEP_DELAY = TimeUnit.SECONDS.toMillis(5);

    @Autowired
    private TrackerServiceImpl trackerService;

    @Autowired
    private Session trackerClient;

    @Value("${mboc.tracker.queue}")
    private String queue;

    private Issue operatingIssue;

    @After
    public void tearDown() throws Exception {
        if (operatingIssue != null) {
            IssueUtils.closeIssue(operatingIssue);
            log.info("Issue {} closed", operatingIssue.getKey());
        }
    }

    @Test
    public void testGetTicket() {
        String ticketKey = queue + "-1";
        log.debug("Requesting ticket: {}", ticketKey);

        Issue ticket = trackerService.getTicket(ticketKey);
        Assertions.assertThat(ticket).isNotNull();
        Assertions.assertThat(ticket.getKey()).isEqualTo(ticketKey);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testCreateAndCloseTicket() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, ImmutableMap.of(TOTAL_OFFERS, 70192L), Collections.emptyList(),
            createSimpleAttachment());

        //не отходя от кассы, проверим кастомное поле
        Option<Long> maybeField = operatingIssue.get(TOTAL_OFFERS);
        assertTrue(maybeField.isPresent());
        assertEquals(70192L, (long) maybeField.get());

        // закрываем тикет
        trackerService.closeAndCommentTicket(operatingIssue, TrackerService.CLOSE_COMMENT);

        // проверяем, что тикет действительно закрыт
        operatingIssue = trackerService.getTicket(operatingIssue.getKey());
        Assertions.assertThat(operatingIssue.getStatus().getKey())
            .isEqualTo(IssueStatus.CLOSE.getIssueKey());
    }

    @Test
    public void testNewTicketWillBeInOpenList() throws InterruptedException {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // делаем небольшую задержку, так как трекер не моментально индексирует новые изменения
        Thread.sleep(SLEEP_DELAY);

        // проверяем, что тикет оказался в списке новых
        List<Issue> openIssues = trackerService.fetchOpenTickets(TicketType.MATCHING);
        Assertions.assertThat(openIssues.stream().map(IssueRef::getKey))
            .contains(operatingIssue.getKey());

        // и не оказался в списке решенных
        List<Issue> resolvedIssues = trackerService.fetchResolvedTickets(TicketType.MATCHING);
        Assertions.assertThat(resolvedIssues.stream().map(IssueRef::getKey))
            .doesNotContain(operatingIssue.getKey());
    }

    @Test
    public void testResolvedTicketWillBeInResolvedList() throws InterruptedException {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // переводит тикет в статус решен
        IssueUtils.resolveIssue(operatingIssue);

        // делаем небольшую задержку, так как трекер не моментально индексирует новые изменения
        Thread.sleep(SLEEP_DELAY);

        // проверяем, что тикет НЕ оказался в списке новых
        List<Issue> openIssues = trackerService.fetchOpenTickets(TicketType.MATCHING);
        Assertions.assertThat(openIssues.stream().map(IssueRef::getKey))
            .doesNotContain(operatingIssue.getKey());

        // и оказался в списке решенных
        List<Issue> resolvedIssues = trackerService.fetchResolvedTickets(TicketType.MATCHING);
        Assertions.assertThat(resolvedIssues.stream().map(IssueRef::getKey))
            .contains(operatingIssue.getKey());
    }

    @Test
    public void testClosedTicketWontBeInAnyList() throws InterruptedException {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // переводим тикет в статус закрыт
        IssueUtils.closeIssue(operatingIssue);

        // делаем небольшую задержку, так как трекер не моментально индексирует новые изменения
        Thread.sleep(SLEEP_DELAY);

        // проверяем, что тикет НЕ оказался в списке новых
        List<Issue> openIssues = trackerService.fetchOpenTickets(TicketType.MATCHING);
        Assertions.assertThat(openIssues.stream().map(IssueRef::getKey))
            .doesNotContain(operatingIssue.getKey());

        // и НЕ оказался в списке решенных
        List<Issue> resolvedIssues = trackerService.fetchResolvedTickets(TicketType.MATCHING);
        Assertions.assertThat(resolvedIssues.stream().map(IssueRef::getKey))
            .doesNotContain(operatingIssue.getKey());
    }

    @Test
    public void testNoNewAttachmentsInNewTicket() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // он не будет содержать новых файлов
        List<ExportAttachment> attachments = trackerService.fetchNewAttachments(operatingIssue);
        Assertions.assertThat(attachments).isEmpty();
    }

    @Test
    public void testNewAttachmentsAddedByComments() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // комментируем с файлом тикет
        ExcelFile secondExcel = createSecondExcelBuilder().build();
        createComment(operatingIssue, "first comment", secondExcel);

        // проверяем, что новые файл появился
        List<ExportAttachment> attachments = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> newFiles = convert(attachments);
        Assertions.assertThat(newFiles).containsExactly(secondExcel);
    }

    @Test
    public void testNewAttachmentsAddedBySeveralComments() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // комментируем с файлом тикет
        ExcelFile secondExcel = createSecondExcelBuilder().build();
        createComment(operatingIssue, "first comment", secondExcel);

        // проверяем, что новые файл появился
        List<ExportAttachment> attachments = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> newFiles = convert(attachments);
        Assertions.assertThat(newFiles).containsExactly(secondExcel);

        // добавляем еще 3 разных файла
        ExcelFile.Builder excel1 = createSimpleExcelBuilder();
        ExcelFile.Builder excel2 = createSimpleExcelBuilder();
        ExcelFile.Builder excel3 = createSimpleExcelBuilder();
        excel1.setHeader(0, "Заголовок 1");
        excel2.setHeader(0, "Заголовок 2");
        excel3.setHeader(0, "Заголовок 3");
        createComment(operatingIssue, "comment1", excel1.build());
        createComment(operatingIssue, "сomment2", excel2.build());
        createComment(operatingIssue, "сomment2", excel3.build());

        // проверяем, что 3 новых файла появились и что их порядок совпадает с порядком добавления
        List<ExportAttachment> newAttachments2 = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> newFiles2 = convert(newAttachments2);
        Assertions.assertThat(newFiles2).containsExactly(secondExcel, excel1.build(), excel2.build(), excel3.build());
    }

    @Test
    public void testNewAttachmentsAfterRobotComment() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // комментируем с файлом тикет
        ExcelFile secondExcel = createSecondExcelBuilder().build();
        createComment(operatingIssue, "first comment", secondExcel);

        // помечаем, что мы успешно получили первый файл
        List<ExportAttachment> attachments = trackerService.fetchNewAttachments(operatingIssue);
        trackerService.commentSuccessfullyRetrieval(operatingIssue, attachments);

        // добавляем еще 2 разных файла
        ExcelFile.Builder excel1 = createSimpleExcelBuilder();
        ExcelFile.Builder excel2 = createSimpleExcelBuilder();
        excel1.setHeader(0, "Заголовок 1");
        excel2.setHeader(0, "Заголовок 2");
        createComment(operatingIssue, "comment1", excel1.build());
        createComment(operatingIssue, "сomment2", excel2.build());

        // проверяем, что 2 новых файла появились и что их порядок совпадает с порядком добавления
        List<ExportAttachment> newAttachments2 = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> newFiles2 = convert(newAttachments2);
        Assertions.assertThat(newFiles2).containsExactly(excel1.build(), excel2.build());
    }

    @Test
    public void testNewAttachmentsAfterRobotUnsuccessfullComment() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // комментируем с файлом тикет
        ExcelFile secondExcel = createSecondExcelBuilder().build();
        createComment(operatingIssue, "first comment", secondExcel);

        // помечаем, что мы НЕУСПЕШНО получили первый файл
        List<ExportAttachment> attachments = trackerService.fetchNewAttachments(operatingIssue);
        Multimap<ExportAttachment, Throwable> failedAttachments = ImmutableListMultimap.of(
            attachments.get(0), new Exception("Dummy exception"));
        trackerService.commentUnsuccessfullyRetrieval(operatingIssue, failedAttachments);

        // добавляем еще 2 разных файла
        ExcelFile.Builder excel1 = createSimpleExcelBuilder();
        ExcelFile.Builder excel2 = createSimpleExcelBuilder();
        excel1.setHeader(0, "Заголовок 1");
        excel2.setHeader(0, "Заголовок 2");
        createComment(operatingIssue, "comment1", excel1.build());
        createComment(operatingIssue, "сomment2", excel2.build());

        // проверяем, что 2 новых файла появились и что их порядок совпадает с порядком добавления
        List<ExportAttachment> newAttachments2 = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> newFiles2 = convert(newAttachments2);
        Assertions.assertThat(newFiles2).containsExactly(excel1.build(), excel2.build());
    }

    @Test
    public void testFetchOnlyXlsAndXlsxFiles() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // комментируем с файлом тикет
        ExcelFile simpleExcel = createSimpleExcelBuilder().build();
        ExcelFile secondExcel = createSecondExcelBuilder().build();
        createComment(operatingIssue, "first comment", simpleExcel);
        createComment(operatingIssue, "second comment", secondExcel, false);
        createComment(operatingIssue, "third comment", "Just random value: 56");

        List<ExportAttachment> attachments = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> files = convert(attachments);
        Assertions.assertThat(files).containsExactly(simpleExcel, secondExcel);
    }

    @Test
    public void testNewMatchingTicketsHaveRightProperties() throws InterruptedException {
        // создаем тикет на матчинг
        String title = String.format("Заведение msku '%s'", SUPPLIER);
        String description = "Заполните поля из файла.";
        operatingIssue = trackerService.createTicket(title, description, null, Collections.emptyList(),
            TicketType.MATCHING, createSimpleAttachment());

        // делаем небольшую задержку, так как трекер не моментально индексирует новые изменения
        Thread.sleep(SLEEP_DELAY);

        operatingIssue = trackerService.getTicket(operatingIssue.getKey());

        Assertions.assertThat(operatingIssue.getSummary())
            .isEqualTo(title);
        Assertions.assertThat(operatingIssue.getDescription().get())
            .isEqualTo(description);
        Assertions.assertThat(operatingIssue.getTags())
            .contains("matching")
            .doesNotContain("classification");

        List<Issue> classificationTickets = trackerService.fetchOpenTickets(TicketType.CLASSIFICATION);
        List<Issue> matchingTickets = trackerService.fetchOpenTickets(TicketType.MATCHING);
        Assertions.assertThat(matchingTickets).contains(operatingIssue);
        Assertions.assertThat(classificationTickets).doesNotContain(operatingIssue);
    }

    @Test
    public void testNewClassifyingTicketsHaveRightProperties() throws InterruptedException {
        // создаем тикет на класификацию
        String title = String.format("Классификация офферов для msku '%s'", SUPPLIER);
        String description = "Проверьте категорию офферов и исправьте, если необходимо.";
        operatingIssue = trackerService.createTicket(title, description, null, Collections.emptyList(),
            TicketType.CLASSIFICATION, createSimpleAttachment());

        // делаем небольшую задержку, так как трекер не моментально индексирует новые изменения
        Thread.sleep(SLEEP_DELAY);

        operatingIssue = trackerService.getTicket(operatingIssue.getKey());

        Assertions.assertThat(operatingIssue.getSummary())
            .isEqualTo(title);
        Assertions.assertThat(operatingIssue.getDescription().get())
            .isEqualTo(description);
        Assertions.assertThat(operatingIssue.getTags())
            .contains("classification")
            .doesNotContain("matching");

        List<Issue> classificationTickets = trackerService.fetchOpenTickets(TicketType.CLASSIFICATION);
        List<Issue> matchingTickets = trackerService.fetchOpenTickets(TicketType.MATCHING);
        Assertions.assertThat(matchingTickets).doesNotContain(operatingIssue);
        Assertions.assertThat(classificationTickets).contains(operatingIssue);
    }

    @Test
    public void testNewAttachmentsAfterRobotIncompleteComment() {
        // создаем тикет
        operatingIssue = trackerService.createTicket(TITLE, DESCRIPTION, null, Collections.emptyList(),
            TicketType.CLASSIFICATION, createSimpleAttachment());

        // комментируем с файлом тикет
        ExcelFile secondExcel = createSecondExcelBuilder().build();
        createComment(operatingIssue, "first comment", secondExcel);

        // помечаем, что обработаны не все офферы
        List<String> offers = Lists.newArrayList(new Offer().setId(1L), new Offer().setId(2L)).stream()
            .map(Offer::getId)
            .map(String::valueOf)
            .collect(Collectors.toList());
        trackerService.commentIncompleteProcessing(operatingIssue,
            String.format("<{список, %d шт.\n%s}>", offers.size(), offers));

        // добавляем еще 2 разных файла
        ExcelFile excel1 = createSimpleExcelBuilder()
            .setHeader(0, "Заголовок 1")
            .build();
        ExcelFile excel2 = createSimpleExcelBuilder()
            .setHeader(0, "Заголовок 2")
            .build();
        createComment(operatingIssue, "comment1", excel1);
        createComment(operatingIssue, "сomment2", excel2);

        // проверяем, что 2 новых файла появились и что их порядок совпадает с порядком добавления
        List<ExportAttachment> newAttachments2 = trackerService.fetchNewAttachments(operatingIssue);
        List<ExcelFile> newFiles2 = convert(newAttachments2);
        Assertions.assertThat(newFiles2).containsExactly(excel1, excel2);
    }

    //stateful test
    @Test
    public void testSearchByDescription() {
        List<Issue> issues = trackerService.fetchTickets(TicketType.NO_SIZE_MEASURE_VALUE,
            "MCPTEST", Arrays.asList("category_1", "vendor_1", "supplier_1"),
            Arrays.asList(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));

        Assertions.assertThat(issues.size()).isGreaterThan(0);
    }

    //stateful test
    @Test
    public void testUpdateTicketDescription() {
        List<Issue> issues = trackerService.fetchTickets(TicketType.NO_SIZE_MEASURE_VALUE,
            "MCPTEST", Arrays.asList("category_1", "vendor_1", "supplier_1"),
            Arrays.asList(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));

        Issue updatedTicket = issues.get(0);

        Assertions.assertThat(updatedTicket).isNotNull();

        Issue ticket = trackerService.updateTicketsDescription(updatedTicket,
            updatedTicket.getDescription().orElse("empty description") + "\n updated description");
        Assertions.assertThat(ticket.getDescription()).contains("updated description");
    }

    //не работает с дефолтным токеном
    @SuppressWarnings("checkstyle:magicNumber")
    @Test
    public void testFetchingTicketsByKey() {
        List<Issue> issue = trackerService.fetchTickets(Arrays.asList("MCP-68489", "MCP-68501", "MCP-77312",
            "MCP-77307"), "MCP");

        Assertions.assertThat(issue.size()).isEqualTo(4);
    }

    @Test
    public void testCreationTicketWithComponent() {
        Component component = trackerService.findOrCreateComponent("MCPTEST", "Размерные сетки");
        Issue issue = trackerService.createTicket("ticket", "description", null,
            Collections.emptyList(), TicketType.NEED_SIZE_MEASURE, Collections.emptyMap(), Collections.emptyList(),
            Arrays.asList("need_size_measure"), el -> el.components(component.getId()));

        Assertions.assertThat(issue).isNotNull();
    }

    private ImportAttachment createSimpleAttachment() {
        return new ImportAttachment("test" + ExcelFileUtils.XLSX_EXTENSION,
            createSimpleExcelBuilder().build());
    }

    private ExcelFile.Builder createSimpleExcelBuilder() {
        ExcelFile.Builder excelFile = new ExcelFile.Builder();
        excelFile.setHeaders("Header1", "Header2");
        excelFile.addLine("Value11", "Value12");
        excelFile.addLine("Value21", "Value22");
        return excelFile;
    }

    private ExcelFile.Builder createSecondExcelBuilder() {
        ExcelFile.Builder excelFile = new ExcelFile.Builder();
        excelFile.setHeaders("Производитель", "Товар");
        excelFile.addLine("Apple", "iPhone");
        excelFile.addLine("Samsung", "S9");
        return excelFile;
    }

    private void createComment(Issue issue, String comment, ExcelFile excelFile) {
        createComment(issue, comment, excelFile, true);
    }

    private void createComment(Issue issue, String comment, ExcelFile excelFile, boolean newFormat) {
        String extension = newFormat ? ExcelFileUtils.XLSX_EXTENSION : ExcelFileUtils.XLS_EXTENSION;
        String fileName = String.format("integration-test-%d%s", excelFile.hashCode(), extension);
        InputStream inputStream = newFormat
            ? ExcelFileConverter.convert(excelFile)
            : ExcelFileConverter.convertToOldFormat(excelFile);

        Attachment attachment = trackerClient.attachments()
            .upload(fileName, inputStream, ExcelFileUtils.parseContentType(fileName));

        CommentCreate commentCreate = CommentCreate.builder()
            .comment(comment)
            .attachments(attachment)
            .build();
        trackerClient.comments().create(issue, commentCreate);
    }

    private void createComment(Issue issue, String comment, String fileContent) {
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes(Charsets.UTF_8));
        Attachment attachment = trackerClient.attachments()
            .upload(String.format("integration-test-%d.txt", fileContent.hashCode()),
                inputStream,
                ContentType.TEXT_PLAIN);
        CommentCreate commentCreate = CommentCreate.builder()
            .comment(comment)
            .attachments(attachment)
            .build();
        trackerClient.comments().create(issue, commentCreate);
    }

    private List<ExcelFile> convert(List<ExportAttachment> attachments) {
        List<ExcelFile> excelFiles = attachments.stream()
            .map(ExportAttachment::getExcelFile)
            .collect(Collectors.toList());
        return excelFiles;
    }
}
