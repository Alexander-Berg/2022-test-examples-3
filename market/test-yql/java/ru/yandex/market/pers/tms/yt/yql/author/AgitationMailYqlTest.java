package ru.yandex.market.pers.tms.yt.yql.author;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author grigor-vlad
 * 14.09.2021
 */
public class AgitationMailYqlTest extends AbstractPersYqlTest {

    @Test
    public void testGradeAgitationPush() {
        runTest(
            loadScript("/yql/tables/grade-agitation-push.sql").requestProperty("user_agitation_info"),
            "/author/mail/grade_agitation_push_expected.json",
            "/author/mail/grade_agitation_push_and_mail.mock"
            );
    }

    @Test
    public void testGradeAgitationMail() {
        runTest(
            loadScript("/yql/tables/grade_agitation_mail.sql").requestProperty("user_agitation_info"),
            "/author/mail/grade_agitation_mail_expected.json",
            "/author/mail/grade_agitation_push_and_mail.mock"
            );
    }

    @Test
    public void testPaidMails() {
        runTest(
            loadScript("/yql/author/paid_agitation_mail.sql").requestProperty("paid_agitation_mail"),
            "/author/mail/paid_agitation_mail_expected.json",
            "/author/mail/paid_agitation_mail.mock"
        );
    }

    @Test
    public void testPaidMailsLimited() {
        runTest(
            loadScript("/yql/author/paid_agitation_mail.sql").requestProperty("paid_agitation_mail"),
            "/author/mail/paid_agitation_mail_count_limited_expected.json",
            "/author/mail/paid_agitation_mail_count_limit.mock"
        );
    }

    @Test
    public void testPaidMailsFlatten() {
        runTest(
            loadScript("/yql/author/paid_agitation_mail.sql").requestProperty("yql"),
            "/author/mail/paid_agitation_mail_flat_expected.json",
            "/author/mail/paid_agitation_mail_flat.mock"
        );
    }
}
