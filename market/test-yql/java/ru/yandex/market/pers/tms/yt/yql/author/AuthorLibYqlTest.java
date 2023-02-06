package ru.yandex.market.pers.tms.yt.yql.author;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

/**
 * @author grigor-vlad
 * 20.09.2021
 */
public class AuthorLibYqlTest extends AbstractPersYqlTest {

    @Test
    public void testLibPay() {
        runTest(
            loadScript("/yql/author/lib_pay.sql").requestProperty("current_paid_models"),
            "/author/lib/lib_pay_expected.json",
            "/author/lib/lib_pay.mock"
        );
    }

    @Test
    public void testLibPayOpinion() {
        runTest(
            loadScript("/yql/author/lib_pay.sql").requestProperty("current_paid_opinion_models"),
            "/author/lib/lib_pay_opinion_expected.json",
            "/author/lib/lib_pay_opinion.mock"
        );
    }

    @Test
    public void testLibMailinfo() {
        runTest(
            loadScript("/yql/author/lib_mailinfo.sql").requestProperty("model_info_for_mail"),
            "/author/lib/lib_mailinfo_expected.json",
            "/author/lib/lib_mailinfo.mock"
        );
    }

    @Test
    public void testPersModelRankLib() {
        runTest(
            loadScript("/yql/author/lib_pers_model_rank.sql").requestProperty("pers_model_rank"),
            "/author/lib/lib_pers_model_rank_expected.json",
            "/author/lib/lib_pers_model_rank.mock"
        );
    }
}
