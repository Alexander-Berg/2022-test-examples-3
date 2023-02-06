package ru.yandex.tma;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsmpp.SMPPConstant;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.Session;
import org.jsmpp.session.SubmitSmResult;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.jsmpp.util.TimeFormatter;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class TmaTest extends TestBase {
    private static final long SLEEP_INTERVAL = 6000L;
    private static final TimeFormatter TIME_FORMATTER =
        new AbsoluteTimeFormatter();

    public TmaTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (TmaCluster cluster = new TmaCluster(this);
            SMPPSession session = new SMPPSession();
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.clearDatabase();
            cluster.applyMigration1();
            cluster.applyMigration2();
            cluster.applyMigration4();
            String v1Send = "/v1/send";
            cluster.messengerGateway().add(
                v1Send,
                new ExpectingHttpItem(
                    new JsonChecker(loadResourceAsString("send1.json"))),
                new ExpectingHttpItem(
                    new JsonChecker(loadResourceAsString("send2.json"))),
                new ExpectingHttpItem(
                    new JsonChecker(loadResourceAsString("send3.json"))));
            cluster.start();

            int smppPort = cluster.tmaServer().smppServer().port();
            logger.info("Connecting to SMPP port " + smppPort);
            String systemId =
                session.connectAndBind(
                    "localhost",
                    smppPort,
                    new BindParameter(
                        BindType.BIND_TRX,
                        "yasms",
                        "Secret",
                        "cp",
                        TypeOfNumber.UNKNOWN,
                        NumberingPlanIndicator.UNKNOWN,
                        null));

            logger.info("Connected with systemId " + systemId);
            DeliveryReceiptsListener deliveryListener =
                new DeliveryReceiptsListener(
                    logger.addPrefix("DeliveryListener"));
            session.setMessageReceiverListener(deliveryListener);
            SubmitSmResult submitSmResult =
                session.submitShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.UNKNOWN,
                    "74957397000",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.UNKNOWN,
                    "79267227664",
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    TIME_FORMATTER.format(new Date()),
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(
                        Alphabet.ALPHA_UCS2,
                        MessageClass.CLASS1,
                        false),
                    (byte) 0,
                    ("O seu código de verificação é: 480-356. "
                        + "Introduza-o no campo de texto.")
                        .getBytes(StandardCharsets.UTF_16BE));
            logger.info(
                "Message submitted. Message id "
                + submitSmResult.getMessageId());

            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(
                1,
                cluster.messengerGateway().accessCount(v1Send));
            cluster.applyMigration3();

            submitSmResult =
                session.submitShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.UNKNOWN,
                    "74957397000",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.UNKNOWN,
                    "79267227665",
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    TIME_FORMATTER.format(new Date()),
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(
                        Alphabet.ALPHA_DEFAULT,
                        MessageClass.CLASS1,
                        false),
                    (byte) 0,
                    "Hi Dmitriy! Your account Teßt has been updated."
                        .getBytes(Charset.forName("GSM7")));
            logger.info(
                "Message submitted. Message id "
                + submitSmResult.getMessageId());
            cluster.applyMigration5();

            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(
                2,
                cluster.messengerGateway().accessCount(v1Send));

            submitSmResult =
                session.submitShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.UNKNOWN,
                    "74957397000",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.UNKNOWN,
                    "79267227666",
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    TIME_FORMATTER.format(new Date()),
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(
                        Alphabet.ALPHA_UCS2,
                        MessageClass.CLASS1,
                        false),
                    (byte) 0,
                    "<#>O seu código de verificação é: 555999.\nMOoS+UjeNYR"
                        .getBytes(StandardCharsets.UTF_16BE));
            logger.info(
                "Message submitted. Message id "
                + submitSmResult.getMessageId());

            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(
                3,
                cluster.messengerGateway().accessCount(v1Send));

            Assert.assertNull(deliveryListener.states.get("1"));
            Assert.assertNull(deliveryListener.states.get("2"));
            Assert.assertNull(deliveryListener.states.get("3"));

            HttpPost post =
                new HttpPost(cluster.tmaServer().host() + "/delivery-reports");
            post.setEntity(
                new StringEntity(
                    loadResourceAsString("delivery-reports.json")
                        .replace(
                            "__worker_id__",
                            cluster.tmaServer().workerId()),
                    ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(
                DeliveryReceiptState.DELIVRD,
                deliveryListener.states.get("1"));
            Assert.assertEquals(
                DeliveryReceiptState.UNDELIV,
                deliveryListener.states.get("2"));
            Assert.assertNull(deliveryListener.states.get("3"));
        }
    }

    private static class DeliveryReceiptsListener
        implements MessageReceiverListener
    {
        private final Map<String, DeliveryReceiptState> states =
            new ConcurrentHashMap<>();
        private final PrefixedLogger logger;

        DeliveryReceiptsListener(final PrefixedLogger logger) {
            this.logger = logger;
        }

        @Override
        public void onAcceptDeliverSm(final DeliverSm deliverSm)
            throws ProcessRequestException
        {
            byte esmClass = deliverSm.getEsmClass();
            logger.info(
                "Received deliver_sm with class " + (esmClass & 0xff)
                + " and id " + deliverSm.getId());
            if (MessageType.SMSC_DEL_RECEIPT.containedIn(esmClass)) {
                try {
                    DeliveryReceipt receipt =
                        deliverSm.getShortMessageAsDeliveryReceipt();
                    String id = receipt.getId();
                    DeliveryReceiptState state = receipt.getFinalStatus();
                    logger.info(
                        "It is delivery receipt " + id
                        + " with state " + state);
                    states.put(id, state);
                } catch (InvalidDeliveryReceiptException e) {
                    logger.log(
                        Level.WARNING,
                        "Failed to parse delivery receipt",
                        e);
                    throw new ProcessRequestException(
                        "Failed to parse delivery receipt",
                        SMPPConstant.STAT_ESME_RSYSERR,
                        e);
                }
            }
        }

        @Override
        public void onAcceptAlertNotification(
            final AlertNotification alertNotification)
        {
            logger.info("Received alert notification");
        }

        @Override
        public DataSmResult onAcceptDataSm(
            final DataSm dataSm,
            final Session source)
            throws ProcessRequestException
        {
            logger.warning("Rejecting data_sm request");
            throw new ProcessRequestException(
                "data_sm not implemented",
                SMPPConstant.STAT_ESME_RSYSERR);
        }
    }
}

