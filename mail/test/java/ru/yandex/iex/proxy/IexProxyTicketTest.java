package ru.yandex.iex.proxy;

import java.io.File;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.TestBase;

public class IexProxyTicketTest extends TestBase {
    private static final String PATH = "ticket/";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String POBEDA_AERO = "pobeda.aero";
    private static final String SINDBAD = "sindbad.ru";
    private static final String AEROFLOT = "aeroflot.ru";
    private static final String BILETDV_RU = "biletdv.ru";
    private static final String RECEIVED_DATE_SAMPLE_215 = "1532457215";
    private static final String RECEIVED_DATE_SAMPLE_216 = "1532457216";
    private static boolean rewriteExpectedJson = false; // Make it by para

    @Test
    public void testTicket1() throws Exception {
        genericTicketTest(
            "ticket_raw_coke_1.json",
            "ticket_returnedby_iexproxy_1.json",
            "1451595600");
    }

    @Test
    public void testTicket2() throws Exception {
        genericTicketTest(
            "ticket_raw_coke_2.json",
            "ticket_returnedby_iexproxy_2.json",
            "1451595601");
    }

    @Test
    public void testTicket3() throws Exception {
        genericTicketTest(
            "ticket_raw_coke_3.json",
            "ticket_returnedby_iexproxy_3.json",
            "1451595602");
    }

    @Test
    public void testTicket4() throws Exception {
        genericTicketTest(
            "ticket_raw_coke_4.json",
            "ticket_returnedby_iexproxy_4.json",
            "1451595603");
    }

    @Test
    public void testTicket6() throws Exception {
        genericTicketTest(
            "ticket_raw_coke_6.json",
            "ticket_returnedby_iexproxy_6.json",
            "1420059600");
    }

    @Test
    public void testTicket8() throws Exception {
        genericTicketTest(
            "ticket_raw_coke_8.json",
            "ticket_returnedby_iexproxy_8.json",
            "1514754003");
    }

    @Test
    public void testTicket9() throws Exception {
        genericTicketTestWithDomain(
            "ticket_raw_coke_9.json",
            "ticket_returnedby_iexproxy_9.json",
            "blablacar.ru",
            "1483218000");
    }

    @Test
    public void testTicket10() throws Exception {
        genericTicketTestWithDomain(
            "ticket_raw_coke_10.json",
            "ticket_returnedby_iexproxy_10.json",
            "go2see.ru",
            "1514754000");
    }

    @Test
    public void testTicket11() throws Exception {
        genericTicketTestWithDomain(
            "ticket_raw_coke_11.json",
            "ticket_returnedby_iexproxy_11.json",
            SINDBAD,
            "1483218001");
    }

    @Test
    public void testTicket12() throws Exception {
        genericTicketTestWithDomain(
            "ticket_raw_coke_12.json",
            "ticket_returnedby_iexproxy_12.json",
            "superkassa.ru",
            "1483218002");
    }

    @Test
    public void testUtairTicket() throws Exception {
        genericTicketTestWithDomain(
            "utair_ticket_raw_coke.json",
            "utair_ticket_returnedby_iexproxy.json",
            "utair.ru",
            "1514754001");
    }

    @Test
    public void testPobedaTicket() throws Exception {
        genericTicketTestWithDomain(
            "pobeda_ticket_raw_coke.json",
            "pobeda_ticket_returnedby_iexproxy.json",
            POBEDA_AERO,
            "1514754002");
    }

    @Test
    public void testPobedaTicketUrlOnly() throws Exception {
        genericTicketTestWithDomain(
            "pobeda_ticket_url_only_raw_coke.json",
            "pobeda_ticket_url_only_returnedby_iexproxy.json",
            POBEDA_AERO,
            "1514754004");
    }

    @Test
    public void testSindbadTicketPaymentUrlOnly() throws Exception {
        genericTicketTestWithDomain(
            "sindbad_ticket_payment_url_only_raw_coke.json",
            "sindbad_ticket_payment_url_only_returnedby_iexproxy.json",
            SINDBAD,
            "1483218234");
    }

    @Test
    public void testAeroflotTicketPaymentUrl() throws Exception {
        genericTicketTestWithDomain(
            "aeroflot_ticket_payment_url_raw_coke.json",
            "aeroflot_ticket_payment_url_returnedby_iexproxy.json",
            AEROFLOT,
            "1532457211");
    }

    @Test
    public void testAeroflotTicketEmptyPaymentUrl() throws Exception {
        genericTicketTestWithDomain(
            "aeroflot_ticket_empty_payment_url_raw_coke.json",
            "aeroflot_ticket_empty_payment_url_returnedby_iexproxy.json",
            AEROFLOT,
            "1532457212");
    }

    @Test
    public void testAeroflotTicketNumberMicroData() throws Exception {
        genericTicketTestWithDomain(
            "aeroflot_ticketNumber_trip_ru_microdata.json",
            "aeroflot_ticketNumber_trip_ru_microdata_returned_by_iexproxy.json",
            AEROFLOT,
            RECEIVED_DATE_SAMPLE_215);
    }

    @Test
    public void testAeroflotOneTicketNumberMicroData() throws Exception {
        genericTicketTestWithDomain(
            "aeroflot_microdata_one_ticket_number.json",
            "aeroflot_microdata_one_ticket_number_returned_by_iexproxy.json",
            AEROFLOT,
            RECEIVED_DATE_SAMPLE_215);
    }

    @Test
    public void testTicketNumber13DigitsOneEntry() throws Exception {
        genericTicketTestWithDomain(
            "ticket_number_13_digits_one_entry.json",
            "ticket_number_13_digits_one_entry_returned_by_iexproxy.json",
            BILETDV_RU,
            RECEIVED_DATE_SAMPLE_216);
    }

    @Test
    public void testTicketNumber13DigitsFewEntries() throws Exception {
        genericTicketTestWithDomain(
            "ticket_number_13_digits.json",
            "ticket_number_13_digits_returned_by_iexproxy.json",
            BILETDV_RU,
            RECEIVED_DATE_SAMPLE_216);
    }

    @Test
    public void testBiletdvTicketPaymentUrl() throws Exception {
        genericTicketTestWithDomain(
            "biletdv_ticket_payment_url_raw_coke.json",
            "biletdv_ticket_payment_url_returnedby_iexproxy.json",
            BILETDV_RU,
            "1535467212");
    }

    @Test
    public void testS7Unpaid() throws Exception {
        genericTicketTestWithDomain(
            "s7_unpaid_raw_coke.json",
            "s7_unpaid_returnedby_iexproxy.json",
            "s7.ru",
            "1535790134");
    }

    @Test
    public void testNabortuUnpaid() throws Exception {
        genericTicketTestWithDomain(
            "nabortu_unpaid_raw_coke.json",
            "nabortu_unpaid_returnedby_iexproxy.json",
            "nabortu.ru",
            "1535223263");
    }

    @Test
    public void testAnywayanydayUnpaid() throws Exception {
        genericTicketTestWithDomain(
            "anywayanyday_unpaid_raw_coke.json",
            "anywayanyday_unpaid_returnedby_iexproxy.json",
            "anywayanyday.ru",
            "1534839422");
    }

    @Test
    public void testUralairlinesTicket() throws Exception {
        genericTicketTestWithDomain(
            "uralairlines_ticket_empty_raw_coke.json",
            "uralairlines_ticket_empty_returnedby_iexproxy.json",
            "uralairlines.ru",
            "1540121696");
    }

    private void genericTicketTest(
        final String inputCokeSolution,
        final String outputExpectedSolution,
        final String receivedDate)
        throws Exception
    {
        genericTicketTestWithDomain(
            inputCokeSolution,
            outputExpectedSolution,
            "thy.com",
            receivedDate);
    }

    // CSOFF: ParameterNumber
    private void genericTicketTestWithDomain(
        final String inputCokeSolution,
        final String outputExpectedSolution,
        final String domain,
        final String receivedDate)
        throws Exception
    {
        try (IexProxyCluster cluster = new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            HttpPost post = createHttpPost(
                cluster,
                PATH + inputCokeSolution,
                domain,
                receivedDate);
            cluster.iexproxy().start();
            try (CloseableHttpResponse response = client.execute(post)) {
                String returned = CharsetUtils.toString(response.getEntity());
                cluster.compareJson(
                    PATH + outputExpectedSolution,
                    returned,
                    rewriteExpectedJson);
            }
        }
    }

    private HttpPost createHttpPost(
        final IexProxyCluster cluster,
        final String file,
        final String domain,
        final String receivedDate)
        throws Exception
    {
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.iexproxy().port() + "/ticket?"
            + "subject=subjecthere&uid=4002720415&mid=158470411888099553"
            + "&email=ozon@" + domain + "&user_email=ren.prs@yandex.ru"
            + "&received_date=" + receivedDate + "&domain=" + domain
            + "&types=5,16,55,64,9999,100500");
        post.setEntity(
            new FileEntity(
                new File(getClass()
                    .getResource(file).toURI()),
                ContentType.APPLICATION_JSON));
        return post;
    }
    // CSON: ParameterNumber
}
