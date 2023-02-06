package ru.yandex.market.mboc.integration.test.tracker;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileUtils;
import ru.yandex.market.mbo.tracker.TrackerServiceImpl;
import ru.yandex.market.mbo.tracker.models.ImportAttachment;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author s-ermakov
 */
public class IssueUtilsTest extends BaseIssueUtilsTestClass {

    @Autowired
    private TrackerServiceImpl trackerService;

    @Override
    protected Issue createIssue() {
        ImportAttachment attachment = new ImportAttachment("test" + ExcelFileUtils.XLSX_EXTENSION,
            new ExcelFile.Builder().build());
        return trackerService.createTicket("Integration test ticket",
            "Тикет создан автоматически при тестировании " + getClass().getSimpleName(),
            null, Collections.emptyList(), TicketType.CLASSIFICATION, attachment);
    }

    @Override
    protected Issue getIssue(Issue oldIssue) {
        return trackerService.getTicket(oldIssue.getKey());
    }
}
