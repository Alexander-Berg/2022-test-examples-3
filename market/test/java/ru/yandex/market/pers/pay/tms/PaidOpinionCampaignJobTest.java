package ru.yandex.market.pers.pay.tms;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.model.ContentPrice;
import ru.yandex.market.pers.pay.model.PayerBalance;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.model.PersPayer;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.dto.PaymentOfferDto;
import ru.yandex.market.pers.pay.mvc.GradePaymentMvcMocks;
import ru.yandex.market.pers.pay.service.PaymentProcessor;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.pers.pay.tms.state.AbstractPaymentStatesExecutorTest;
import ru.yandex.market.pers.pay.tms.util.PersPayUtils;
import ru.yandex.market.util.ListUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_GRADE;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_GRADE_PHOTO;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_VIDEO;
import static ru.yandex.market.pers.pay.model.PersPayState.PAYED;
import static ru.yandex.market.pers.pay.tms.PaidOpinionCampaignJob.CAMPAIGN_PRICES_LOAD_FLAG;
import static ru.yandex.market.pers.pay.tms.PaidOpinionCampaignJob.CLEANUP_INTERVAL_DAYS;
import static ru.yandex.market.pers.pay.tms.PaidOpinionCampaignJob.REGULAR_CAMPAIGN_PAYER_DEFAULT_VALUE;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 31.05.2021
 */
public class PaidOpinionCampaignJobTest extends AbstractPaymentStatesExecutorTest {
    public static final int LIMIT = 1000;

    @Autowired
    private YtHelper ytHelper;
    @Autowired
    private PaidOpinionCampaignJob job;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AutoCreatePaymentJob autoCreatePaymentJob;
    @Autowired
    private TmsPaymentService paymentService;
    @Autowired
    private GradePaymentMvcMocks paymentMvc;
    @Autowired
    private UnbalancedPaymentsJob unbalancedPaymentsJob;

    @Test
    public void testPriceUpload() throws IOException {
        ArgumentCaptor<Function> mappingCaptor = ArgumentCaptor.forClass(Function.class);
        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<YPath> ytPathCaptor = ArgumentCaptor.forClass(YPath.class);

        String campaignName = "campaign_marketpers_7340";
        addNewPayerBalance(campaignName);
        PayerBalance payer = getPayer(campaignName);

        //uploaded campaign
        addNewPayerBalance("campaign_marketpers_7323");
        configurationService.mergeValue(String.format(CAMPAIGN_PRICES_LOAD_FLAG, "campaign_marketpers_7323"), "true");

        job.loadPaidOpinionCampaignPrices();

        verify(ytHelper.getHahnClient(), times(1)).consumeTableBatched(
            ytPathCaptor.capture(), anyInt(),
            mappingCaptor.capture(),
            consumerCaptor.capture());

        assertEquals(campaignName, ytPathCaptor.getValue().name());

        Function<JsonNode, List<ContentPrice>> mapper = mappingCaptor.getValue();
        Consumer<List<List<ContentPrice>>> consumer = consumerCaptor.getValue();

        String json = fileToString("/data/prices.json");

        JsonNode arrayNode = new ObjectMapper().readTree(json);
        List<List<ContentPrice>> parsedPrice = IntStream.range(0, arrayNode.size()).mapToObj(arrayNode::get)
            .map(mapper)
            .collect(Collectors.toList());

        List<ContentPrice> parsedPriceFlat = parsedPrice.stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(x -> x.getEntity().getIdLong()))
            .collect(Collectors.toList());

        assertEquals(0, getCurrentPrices().size());

        assertPrice2(parsedPriceFlat.get(0), MODEL_VIDEO, payer, 32465, "42", "42", 3);
        assertPrice2(parsedPriceFlat.get(1), MODEL_GRADE_PHOTO, payer, 562523, "256", "256", 2);
        assertPrice2(parsedPriceFlat.get(2), MODEL_GRADE_PHOTO, payer, 1341343, "1313", "1313", null);
        assertPrice2(parsedPriceFlat.get(3), MODEL_GRADE, payer, 11155028, "50", "50", null);
        assertPrice2(parsedPriceFlat.get(4), MODEL_GRADE, payer, 216519177, "90", "90", 2);
        assertPrice2(parsedPriceFlat.get(5), MODEL_GRADE, payer, 216521530, "1000", "1000", 9);

        // save to db
        consumer.accept(parsedPrice);
        List<ContentPrice> pricesInDb = getCurrentPrices();
        assertEquals(6, pricesInDb.size());

        pricesInDb.sort(Comparator.comparing(x -> x.getEntity().getIdLong()));
        assertPrice2(pricesInDb.get(0), MODEL_VIDEO, payer, 32465, "42", "42", 3);
        assertPrice2(pricesInDb.get(1), MODEL_GRADE_PHOTO, payer, 562523, "256", "256", 2);
        assertPrice2(pricesInDb.get(2), MODEL_GRADE_PHOTO, payer, 1341343, "1313", "1313", null);
        assertPrice2(pricesInDb.get(3), MODEL_GRADE, payer, 11155028, "50", "50", null);
        assertPrice2(pricesInDb.get(4), MODEL_GRADE, payer, 216519177, "90", "90", 2);
        assertPrice2(pricesInDb.get(5), MODEL_GRADE, payer, 216521530, "1000", "1000", 9);

        assertTrue(configurationService.getValue(String.format(CAMPAIGN_PRICES_LOAD_FLAG, campaignName),
            Boolean.class));

        // try to load another set of data - didn't work out
        job.loadPaidOpinionCampaignPrices();
        assertEquals(6, getCurrentPrices().size());

        // try with enable flag
        configurationService.mergeValue(String.format(CAMPAIGN_PRICES_LOAD_FLAG, "campaign_marketpers_7340"), "false");
        job.loadPaidOpinionCampaignPrices();

        uploadNewCampaignFromYt(fileToString("/data/prices_2.json"), 2);

        pricesInDb = getCurrentPrices();
        assertEquals(1, pricesInDb.size());

        assertPrice2(pricesInDb.get(0), MODEL_GRADE, payer, 12345, "200", "200", 4);
    }

    @Test
    public void testClearInfo() throws IOException {
        String jsonCampaign7320 = "[{\"model_id\":20000001,\"price\":1000,\"charge\":\"36.66\",\"target_count\":9},\n" +
            "{\"model_id\":20000002,\"price\":1000,\"charge\":\"36.66\",\"target_count\":9}]";
        uploadNewCampaignFromYt("campaign_marketpers_7320", jsonCampaign7320, 1);

        String jsonCampaign7330 = "[{\"model_id\":10000001,\"price\":50,\"charge\":\"1.82\",\"target_count\":0}]";
        uploadNewCampaignFromYt("campaign_marketpers_7330", jsonCampaign7330, 2);

        String jsonCampaign7340 = "[{\"model_id\":30000001,\"price\":90,\"charge\":\"3.3\",\"target_count\":2}]";
        uploadNewCampaignFromYt("campaign_marketpers_7340", jsonCampaign7340, 3);

        shiftEndTimeIntoPastByDays("campaign_marketpers_7320", CLEANUP_INTERVAL_DAYS + 1); // need clear
        shiftEndTimeIntoPastByDays("campaign_marketpers_7330", CLEANUP_INTERVAL_DAYS - 1);

        when(ytHelper.getCurrentClient().exists(any(YPath.class))).thenReturn(true);

        job.clearPaidOpinionCampaignInfo();

        verify(ytHelper.getCurrentClient(), times(1)).exists(any(YPath.class));
        verify(ytHelper.getCurrentClient(), times(1)).move(isNull(), any(YPath.class), any(YPath.class));
    }

    @Test
    public void testPayerBalanceDecrease() throws IOException {
        int maxPriceCharge = 900;
        String jsonCampaign7320 = "[{\"model_id\":20000001,\"price\":900,\"charge\":\"30.00\",\"target_count\":9},\n" +
            "{\"model_id\":20000002,\"price\":900,\"charge\":\"30.00\",\"target_count\":9}]";
        uploadNewCampaignFromYt("campaign_marketpers_7320", jsonCampaign7320, 1);

        String jsonCampaign7330 = "[{\"model_id\":10000001,\"price\":60,\"charge\":\"2.00\",\"target_count\":0}," +
            "{\"model_id\":20000001,\"price\":60,\"charge\":\"2.00\",\"target_count\":0}]";
        uploadNewCampaignFromYt("campaign_marketpers_7330", jsonCampaign7330, 2);

        String jsonCampaign7340 = "[{\"model_id\":30000001,\"price\":180,\"charge\":\"6.00\",\"target_count\":2}]";
        uploadNewCampaignFromYt("campaign_marketpers_7340", jsonCampaign7340, 3);

        Map<String, PayerBalance> payers = Map.of("campaign_marketpers_7320", getPayer("campaign_marketpers_7320"));

        mockPersAuthorAgitations(USER_ID, List.of(10000001L, 20000001L, 20000002L, 30000001L));

        // get offers
        List<PaymentOfferDto> foundOffers = paymentMvc
            .showPaymentOffers(USER_ID, List.of(10000001L, 20000001L, 20000002L, 30000001L));
        assertEquals(4, foundOffers.size());

        // create content
        createModelGrade(20000001, USER_ID, PersPayEntityState.APPROVED);

        // create payment
        autoCreatePaymentJob.autoCreatePayments();

        // check payments
        List<PersPayment> payments = paymentService.getPaymentsToProcess(PersPayState.NEW, LIMIT);
        paymentService.changeState(ListUtils.toList(payments, PersPayment::getId), PAYED, Map.of());
        assertEquals(1, payments.size());
        assertEquals(MockUtils.testPayKey(20000001, USER_ID), payments.get(0).getPayKey());
        assertEquals(payers.get("campaign_marketpers_7320").getPayer(), payments.get(0).getPayer());

        unbalancedPaymentsJob.processUnbalancedMarketPays();

        // decrease payer balance
        assertEquals(
            payers.get("campaign_marketpers_7320").getBalance().intValue() - maxPriceCharge,
            getPayer("campaign_marketpers_7320").getBalance().intValue()
        );
    }

    @Test
    public void testRegularCampaignPriceUpload() throws IOException {
        addNewPayerBalance(REGULAR_CAMPAIGN_PAYER_DEFAULT_VALUE);
        String jsonCampaign = "[{\"model_id\":100000,\"price\":50,\"charge\":\"1.6666\",\"target_count\":5},\n" +
            "{\"model_id\":100001,\"price\":50,\"charge\":\"1.6666\",\"target_count\":5}]";
        uploadNewCampaignFromYt(jsonCampaign, 1);

        List<ContentPrice> pricesInDb = getCurrentPrices();
        assertEquals(2, pricesInDb.size());
        assertEquals("100000", pricesInDb.get(0).getEntity().getId());
        assertEquals("100001", pricesInDb.get(1).getEntity().getId());

        job.updateTopClickoutRegularCampaignPrices();

        configurationService.mergeValue(String.format(
            CAMPAIGN_PRICES_LOAD_FLAG, REGULAR_CAMPAIGN_PAYER_DEFAULT_VALUE), "false");

        jsonCampaign = "[{\"model_id\":200000,\"price\":50,\"charge\":\"1.6666\",\"target_count\":5},\n" +
            "{\"model_id\":200001,\"price\":50,\"charge\":\"1.6666\",\"target_count\":5}]";
        uploadNewCampaignFromYt(jsonCampaign, 2);

        pricesInDb = getCurrentPrices();
        assertEquals(2, pricesInDb.size());
        assertEquals("200000", pricesInDb.get(0).getEntity().getId());
        assertEquals("200001", pricesInDb.get(1).getEntity().getId());
    }

    private void uploadNewCampaignFromYt(String campaignName, String json, int numberOfInvoc) throws IOException {
        addNewPayerBalance(campaignName);
        uploadNewCampaignFromYt(json, numberOfInvoc);
    }

    private void uploadNewCampaignFromYt(String json, int numberOfInvoc) throws IOException {
        ArgumentCaptor<Function> mappingCaptor = ArgumentCaptor.forClass(Function.class);
        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

        job.loadPaidOpinionCampaignPrices();

        verify(ytHelper.getCurrentClient(), times(numberOfInvoc)).consumeTableBatched(
            any(YPath.class), anyInt(),
            mappingCaptor.capture(),
            consumerCaptor.capture()
        );

        Function<JsonNode, ContentPrice> mapper = mappingCaptor.getValue();
        Consumer<List<ContentPrice>> consumer = consumerCaptor.getValue();

        JsonNode arrayNode = new ObjectMapper().readTree(json);
        List<ContentPrice> parsedPrice = IntStream.range(0, arrayNode.size()).mapToObj(arrayNode::get)
            .map(mapper)
            .collect(Collectors.toList());
        consumer.accept(parsedPrice);
    }

    private void assertPrice2(ContentPrice item,
                              PersPayEntityType entityType,
                              PayerBalance payer,
                              long modelId,
                              String amount,
                              String amountCharge,
                              Integer targetCount) {
        assertPrice(item, entityType, payer.getPayer(), modelId, amount, amountCharge, targetCount);
        assertEquals(payer.getEndTime().toEpochMilli(), item.getEndTime().toEpochMilli());
    }

    private void assertPrice(ContentPrice item,
                             PersPayEntityType entityType,
                             PersPayer payer,
                             long modelId,
                             String amount,
                             String amountCharge,
                             Integer targetCount) {
        assertEquals(PersPayerType.MARKET, item.getPayer().getType());
        assertEquals(payer.getId(), item.getPayer().getId());
        assertEquals(entityType, item.getEntity().getType());
        assertEquals(modelId, item.getEntity().getIdLong());
        assertEquals(amount, item.getAmount().toString());
        assertEquals(amountCharge, item.getAmountCharge().toString());

        if (targetCount != null) {
            assertEquals(Map.of(PaymentProcessor.TARGET_COUNT_LIMIT, String.valueOf(targetCount)), item.getLimits());
        } else {
            assertNull(item.getLimits());
        }
    }

    private List<ContentPrice> getCurrentPrices() {
        return jdbcTemplate.query(
            "select *, 0 as balance\n" +
                "from pay.content_price\n" +
                "where end_time > now()" +
                "order by entity_id",
            (rs, idx) -> {
                ContentPrice result = ContentPrice.parse(rs, idx);
                result.setEndTime(rs.getTimestamp("end_time").toInstant());
                return result;
            });
    }

    private void addNewPayerBalance(String campaignName) {
        jdbcTemplate.update(
            "insert into pay.payer_balance(payer_type, payer_id, balance, end_time) " +
                "values (?,?,?,now() + interval '1' day)\n",
            PersPayerType.MARKET.getValue(),
            campaignName,
            10000000
        );
    }

    private PayerBalance getPayer(String campaignName) {
        return jdbcTemplate.query(
            "select payer_type, payer_id, balance, end_time\n" +
                "from pay.payer_balance where payer_type = ? and payer_id = ?",
            (rs, rowNum) -> {
                PayerBalance result = new PayerBalance(
                    PersPayUtils.parsePayer(rs),
                    rs.getBigDecimal("balance")
                );
                result.setEndTime(rs.getTimestamp("end_time").toInstant());
                return result;
            },
            PersPayerType.MARKET.getValue(),
            campaignName
        ).get(0);
    }

    private void shiftEndTimeIntoPastByDays(String campaignName, int shift) {
        jdbcTemplate.update(
            "update pay.payer_balance set end_time = now() - ? * interval '1' day where payer_id = ?",
            shift, campaignName);
    }
}
