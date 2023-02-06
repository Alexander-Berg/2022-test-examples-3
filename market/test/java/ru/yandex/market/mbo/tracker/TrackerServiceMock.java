package ru.yandex.market.mbo.tracker;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.tracker.models.ExportAttachment;
import ru.yandex.market.mbo.tracker.models.ImportAttachment;
import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mbo.tracker.utils.TrackerIssueValueUtils;
import ru.yandex.market.mbo.tracker.utils.TrackerServiceHelper;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.UserRef;

/**
 * @author s-ermakov
 */
public class TrackerServiceMock implements TrackerService {

    public static final String TEST_QUEUE = "MCP";

    private int idGenerator = 1;
    private int attachmentsNumberGenerator = 1;
    private int attachmentsSecondsGenerator = 1000;

    private Map<String, IssueMock> issuesByKey = new HashMap<>();
    private ArrayListMultimap<Issue, ImportAttachment> headerExcelsWithNames = ArrayListMultimap.create();
    private Multimap<Issue, ExportAttachment> commentFiles = HashMultimap.create();
    private LinkedListMultimap<Issue, String> rawComments = LinkedListMultimap.create();
    private LinkedListMultimap<Issue, String> summonees = LinkedListMultimap.create();
    private Map<String, Set<String>> links = new HashMap<>();
    private TrackerServiceHelper trackerServiceHelper =
        new TrackerServiceHelper(null, null,
            null, null, null,
            null);

    @Override
    public Issue getTicket(String ticketKey) {
        return issuesByKey.get(ticketKey);
    }

    @Override
    public List<Issue> getTickets(List<String> keys) {
        List<Issue> result = new ArrayList<>();
        for (String key : keys) {
            result.add(issuesByKey.get(key));
        }
        return result;
    }

    public Collection<? extends Issue> getAllTickets() {
        return issuesByKey.values();
    }

    public Issue simpleIssue(@Nullable TicketType ticketType) {
        return createTicket("title", "description", "author", Collections.emptyList(),
            ticketType, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(), builder -> {
            });
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    public Issue createTicket(String title, String description, String author,
                              Collection<String> ticketFollowers,
                              @Nullable TicketType ticketType, Map<String, Object> customFields,
                              List<ImportAttachment> attachments, Collection<String> additionalTags,
                              Consumer<IssueCreate.Builder> issueCustomizer) {
        IssueCreate.Builder builder = IssueCreate.builder()
            .summary(title)
            .description(description)
            .author(author)
            .type(TrackerServiceImpl.TASK_TYPE);

        ArrayListF<String> tags = new ArrayListF<>();
        tags.add("mock-service-tag");
        if (!additionalTags.isEmpty()) {
            tags.addAll(additionalTags);
        }
        if (ticketType != null) {
            tags.add(trackerServiceHelper.getTicketTag(ticketType));
        }
        builder = builder.tags(tags);

        customFields.forEach(builder::set);

        if (issueCustomizer != null) {
            issueCustomizer.accept(builder);
        }
        IssueCreate issueCreate = builder.build();
        MapF<String, Object> values = issueCreate.getValues();

        Set<String> concatFollowers = new HashSet<>(Arrays.asList(
            TrackerIssueValueUtils.extractStrings(values.getOrElse(KnownKeys.followers.name(), null))));
        concatFollowers.addAll(ticketFollowers);

        IssueMock issue = new IssueMock()
            .setSummary((String) values.getOrThrow(KnownKeys.summary.name()))
            .setDescription((String) values.getOrThrow(KnownKeys.description.name()))
            .setAuthor((String) values.getOrElse(KnownKeys.author.name(), null))
            .setAssignee((String) values.getOrElse(KnownKeys.assignee.name(), "test-user"))
            .setFollowers(concatFollowers.toArray(new String[0]))
            .setKey(nextKey())
            .setCreatedAt(org.joda.time.Instant.now())
            .setTags(TrackerIssueValueUtils.extractStrings(values.getOrThrow(KnownKeys.tags.name())));

        Set<String> knownKeys = Stream.of(KnownKeys.values()).map(Enum::name).collect(Collectors.toSet());
        values.forEach((key, value) -> {
            if (knownKeys.contains(key)) {
                return;
            }
            issue.setCustomField(key, value);
        });
        issuesByKey.put(issue.getKey(), issue);

        for (ImportAttachment attachment : attachments) {
            headerExcelsWithNames.put(issue, attachment);
        }
        return issue;
    }

    /**
     * Возвращает файл, который был создан у тикета.
     */
    public ExcelFile getHeaderExcelFile(Issue ticket) {
        return headerExcelsWithNames.get(ticket).get(0).getExcelFile();
    }

    public List<ExcelFile> getHeaderExcelFiles(Issue ticket) {
        return headerExcelsWithNames.get(ticket).stream()
            .map(ImportAttachment::getExcelFile)
            .collect(Collectors.toList());
    }

    /**
     * Возвращает файл с именем, который был создан у тикета.
     */
    public ImportAttachment getTicketImportAttachment(Issue ticket) {
        return headerExcelsWithNames.get(ticket).get(0);
    }

    public List<ImportAttachment> getTicketImportAttachments(Issue ticket) {
        return headerExcelsWithNames.get(ticket);
    }

    /**
     * Добавляет комментарий к тикету в качестве файла.
     */
    public void commentWithAttachment(Issue ticket, ExcelFile excelFile) {
        MockExportAttachment attachment = new MockExportAttachment(
            "Mock attachment " + attachmentsNumberGenerator++,
            Instant.ofEpochSecond(attachmentsSecondsGenerator),
            "mapping-user",
            excelFile);
        commentFiles.put(ticket, attachment);
        attachmentsSecondsGenerator += 100;
        rawComments.put(ticket, "Add excelFile " + attachment.getFileName());
    }

    public String getTicketTag(TicketType ticketType) {
        return trackerServiceHelper.getTicketTag(ticketType);
    }

    @Override
    public List<Issue> fetchOpenTickets(TicketType ticketType) {
        return fetchOpenTickets(ticketType, null);
    }

    @Override
    public List<Issue> fetchResolvedTickets(TicketType ticketType) {
        return fetchResolvedTickets(ticketType, null);
    }

    @Override
    public List<Issue> fetchOpenTickets(TicketType ticketType, @Nullable String queue) {
        return issuesByKey.values().stream()
            .filter(issueMock -> issueMock.getIssueStatus() != IssueStatus.CLOSE
                && issueMock.getIssueStatus() != IssueStatus.RESOLVED)
            .filter(issue -> issue.getTags().containsTs(trackerServiceHelper.getTicketTag(ticketType)))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<Issue> fetchResolvedTickets(TicketType ticketType, @Nullable String queue) {
        return issuesByKey.values().stream()
            .filter(issueMock -> issueMock.getIssueStatus() == IssueStatus.RESOLVED)
            .filter(issue -> issue.getTags().containsTs(trackerServiceHelper.getTicketTag(ticketType)))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<ExportAttachment> fetchNewAttachments(Issue issue) {
        List<ExportAttachment> result = new ArrayList<>(commentFiles.get(issue));
        commentFiles.removeAll(issue);
        return result;
    }

    @Override
    public void commentSuccessfullyRetrieval(Issue issue, Collection<ExportAttachment> attachments) {
        if (!attachments.isEmpty()) {
            rawComments.put(issue, "Successfully retrieval");
        }
    }

    @Override
    public void commentUnsuccessfullyRetrieval(Issue issue, Multimap<ExportAttachment, Throwable> failedAttachments) {
        if (!failedAttachments.isEmpty()) {
            String errorsStr = failedAttachments.values().stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining("\n"));
            rawComments.put(issue, "Unsuccessfully retrieval\n" + errorsStr);
        }
    }

    @Override
    public void commentIncompleteProcessing(Issue issue, String offersToString) {
        if (!offersToString.isEmpty()) {
            rawComments.put(issue, "Incomplete processing\n" + offersToString);
        }
    }

    @Override
    public void closeAndCommentTicket(Issue issue, String comment) {
        IssueUtils.closeIssue(issue);
        rawComments.put(issue, comment);
    }

    @Override
    public void reopenAndCommentTicket(Issue issue, String comment) {
        IssueUtils.reopenIssue(issue);
        rawComments.put(issue, comment);
    }

    @Override
    public void commentTicket(Issue issue, String comment) {
        rawComments.put(issue, comment);
    }

    @Override
    public void summonMaillistInTicket(Issue issue, String maillist) {
        summonees.put(issue, maillist);
    }

    @Override
    public void summonMaillistInTicket(Issue issue, String maillist, String commentText) {
        summonees.put(issue, maillist);
    }

    @Override
    public void linkTickets(Issue issue1, Issue issue2) {
        links.computeIfAbsent(issue1.getKey(), s -> new HashSet<>());
        links.get(issue1.getKey()).add(issue2.getKey());
        links.computeIfAbsent(issue2.getKey(), s -> new HashSet<>());
        links.get(issue2.getKey()).add(issue1.getKey());
    }

    @Override
    public boolean isLinked(String key1, String key2) {
        return links.containsKey(key1) && links.get(key1).contains(key2);
    }

    @Override
    public Map<String, String> getUsersByLogin(Collection<String> staffLogins) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setCustomField(Issue issue, String key, Object value) {
        if (issue instanceof IssueMock) {
            ((IssueMock) issue).setCustomField(key, value);
        }
    }

    @Override
    public Issue addTag(Issue issue, String tag) {
        ((IssueMock) issue).setTags(issue.getTags().plus1(tag).toArray(new String[0]));
        return issue;
    }

    @Override
    public Issue addFollowers(Issue issue, String stuffLogin) {
        IssueMock issueMock = issuesByKey.get(issue.getKey());
        List<String> followers = issueMock.getFollowers().stream()
            .map(UserRef::getLogin)
            .collect(Collectors.toList());
        followers.add(stuffLogin);
        return issueMock.setFollowers(followers.toArray(new String[0]));
    }

    @Override
    public List<Issue> getActiveChangedMappingsTickets(String trackerQueue, String tag) {
        return issuesByKey.keySet().stream()
            .filter(s -> s.startsWith(trackerQueue))
            .map(s -> issuesByKey.get(s))
            .filter(issueMock -> issueMock.getTags().containsTs(tag))
            .filter(issueMock -> !issueMock.getResolution().isPresent())
            .filter(issueMock -> IssueUtils.hasStatus(issueMock, IssueStatus.OPEN) ||
                IssueUtils.hasStatus(issueMock, IssueStatus.IN_PROGRESS))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<Issue> fetchTickets(TicketType ticketType, @Nullable String queue, List<String> tags,
                                    List<IssueStatus> issueStatuses) {
        return issuesByKey.values().stream()
            .filter(issueMock -> issueMock.getIssueStatus() == IssueStatus.OPEN ||
                issueMock.getIssueStatus() == IssueStatus.IN_PROGRESS)
            .filter(issue -> issue.getTags().containsAllTs(tags))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<Issue> fetchTickets(List<String> ticketKeys, String queue) {
        return ticketKeys.stream()
            .map(key -> issuesByKey.get(key))
            .collect(Collectors.toList());
    }

    @Override
    public Issue updateTicketsDescription(Issue updatedTicket, String description) {
        IssueMock issueMock = issuesByKey.get(updatedTicket.getKey());
        String issueDescription = issueMock.getDescription().get();
        return issueMock.setDescription(issueDescription + description);
    }

    public Issue updateTicketStatus(Issue updatedTicket, IssueStatus status) {
        IssueMock issueMock = issuesByKey.get(updatedTicket.getKey());
        return issueMock.setIssueStatus(status);
    }

    @Override
    public Component findOrCreateComponent(String queue, String componentName) {
        return new ComponentMock();
    }

    @Override
    public void setDeadline(String issue, LocalDate deadline) {
        IssueMock issueMock = issuesByKey.get(issue);
        issueMock.setDeadline(Option.of(new org.joda.time.LocalDate()
            .withYear(deadline.getYear())
            .withDayOfYear(deadline.getDayOfYear())));
    }

    private String nextKey() {
        return TEST_QUEUE + "-" + (idGenerator++);
    }

    public List<String> getRawComments(Issue issue) {
        return rawComments.get(issue);
    }

    public List<String> getSummonees(Issue issue) {
        return summonees.get(issue);
    }

    private enum KnownKeys {
        summary, description, author, assignee, tags, followers
    }
}
