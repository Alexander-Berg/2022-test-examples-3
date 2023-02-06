package ru.yandex.market.abo.cpa.cancelled.reason.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.cancelled.reason.report.UserCancelReasonReportService.DateReasonStat;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketStatus;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;
import ru.yandex.market.abo.util.mvc.PgPeriod;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 09.10.18.
 */
public class UserCancelReasonReportServiceTest extends EmptyTest {

    private static final Random RND = new Random();
    private static final int REASON_COUNT = 5;
    private static final int TICKET_COUNT = 1000;
    private static final int MAX_DAYS_BEFORE = 40;
    private static final int REQUEST_COUNT = 100;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    private UserCancelReasonReportService reportService;

    private List<TicketInfo> infos;

    @BeforeEach
    void setUp() {
        IntStream.range(0, REASON_COUNT).forEach(this::createReason);
        infos = IntStream.range(0, TICKET_COUNT).mapToObj(this::createTicket).collect(Collectors.toList());
    }

    @Test
    public void testLoad() {
        IntStream.range(0, REQUEST_COUNT).forEach(i -> {
            UserCancelReasonReportRequest request = randomRequest();
            List<DateReasonStat> dateReasonStats = reportService.loadStat(request);
            dateReasonStats.forEach(dateReasonStat -> checkResult(dateReasonStat, request));
        });
    }

    private void checkResult(DateReasonStat stat,
                             UserCancelReasonReportRequest request) {
        long expectedCount = infos.stream().filter(info -> info.reasonId == stat.reasonId &&
                !(info.getDate().isBefore(request.getFromDate()) || info.getDate().isAfter(request.getToDate())) &&
                compareDate(info.getDate(), stat.date, request) &&
                (request.getTicketType() == null || info.type == request.getTicketType())
        ).count();

        assertEquals(expectedCount, stat.count);
    }

    private boolean compareDate(LocalDate expected, LocalDate loaded, UserCancelReasonReportRequest request) {
        int daysSinceStartOfPeriod;
        switch (request.getPeriod()) {
            case DAY:
                daysSinceStartOfPeriod = 0;
                break;
            case WEEK:
                daysSinceStartOfPeriod = expected.getDayOfWeek().getValue() - 1;
                break;
            case MONTH:
                daysSinceStartOfPeriod = expected.getDayOfMonth() - 1;
                break;
            case YEAR:
                daysSinceStartOfPeriod = expected.getDayOfYear() - 1;
                break;
            default:
                throw new RuntimeException("Unknown enum value");
        }
        return expected.minusDays(daysSinceStartOfPeriod).equals(loaded);
    }

    private void createReason(int id) {
        pgJdbcTemplate.update("INSERT INTO user_cancel_reason (id, name, created_user_id) VALUES (?, ?, 0)",
                id, "reason#" + id);
    }

    private TicketInfo createTicket(int id) {
        TicketInfo ticketInfo = new TicketInfo(
                RND.nextInt(MAX_DAYS_BEFORE + 1),
                RND.nextBoolean() ?
                        RecheckTicketType.USER_SURVEY_BLUE_CANCELLED :
                        RecheckTicketType.USER_SURVEY_PHARMA_CHANGED,
                RND.nextInt(REASON_COUNT)
        );

        pgJdbcTemplate.update("INSERT INTO recheck_ticket " +
                        "(id, shop_id, check_item_id, status_id, type_id, creation_time) " +
                        "VALUES (?, 0, 0, ?, ?, current_date - ?)",
                id,
                RecheckTicketStatus.PASS.getId(),
                ticketInfo.type.getId(),
                ticketInfo.nowMinusDays
        );
        pgJdbcTemplate.update("INSERT INTO core_order (hyp_id, cancel_reason_id) VALUES (?, ?)",
                id, ticketInfo.reasonId);
        return ticketInfo;
    }

    private static UserCancelReasonReportRequest randomRequest() {
        UserCancelReasonReportRequest request = new UserCancelReasonReportRequest();
        request.setFromDate(LocalDate.now().minusDays(RND.nextInt(MAX_DAYS_BEFORE + 1)));
        request.setToDate(LocalDate.now());
        request.setPeriod(PgPeriod.values()[RND.nextInt(PgPeriod.values().length)]);


        RecheckTicketType ticketType = null;
        if (RND.nextBoolean()) {
            ticketType = RND.nextBoolean() ?
                    RecheckTicketType.USER_SURVEY_BLUE_CANCELLED :
                    RecheckTicketType.USER_SURVEY_PHARMA_CHANGED;
        }

        request.setTicketType(ticketType);
        return request;
    }

    private static class TicketInfo {
        private int nowMinusDays;
        private RecheckTicketType type;
        private int reasonId;

        public TicketInfo(int nowMinusDays, RecheckTicketType type, int reasonId) {
            this.nowMinusDays = nowMinusDays;
            this.type = type;
            this.reasonId = reasonId;
        }

        private LocalDate getDate() {
            return LocalDate.now().minusDays(nowMinusDays);
        }
    }
}
