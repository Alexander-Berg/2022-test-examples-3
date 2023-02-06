package ru.yandex.market.hrms.core.service.outstaff.stubs;

import java.util.Optional;

import ru.yandex.market.hrms.core.domain.outstaff.OutstaffEntity;
import ru.yandex.market.hrms.core.service.outstaff.client.OutstaffStartrekClient;
import ru.yandex.market.tpl.common.startrek.ticket.TicketResolutionEnum;
import ru.yandex.market.tpl.common.startrek.ticket.TicketStatusEnum;
import ru.yandex.startrek.client.model.Issue;

public class OutstaffStartrekClientStub extends OutstaffStartrekClient {

    private Issue issue;
    private boolean opened;

    @Override
    public void init() {
    }

    public void withIssue(Issue issue) {
        this.issue = issue;
    }

    public void withIssueOpened(boolean opened) {
        this.opened = opened;
    }

    @Override
    public Optional<Issue> getIssue(OutstaffEntity entity) {
        return Optional.of(issue);
    }

    @Override
    public Issue createIssue(String title, String description, String... tags) {
        return issue;
    }

    @Override
    public Issue createIssueWithUniqueKey(String uniqueKey, String title, String description, String... tags) {
        return issue;
    }

    @Override
    public Issue updateIssue(OutstaffEntity entity, String commentToSummoned, String commentCommon) {
        return issue;
    }

    @Override
    public boolean isIssueOpened(Issue issue) {
        return opened;
    }

    @Override
    public void setIssueStatus(Issue issue, TicketStatusEnum newState, TicketResolutionEnum resolution) {
    }
}
