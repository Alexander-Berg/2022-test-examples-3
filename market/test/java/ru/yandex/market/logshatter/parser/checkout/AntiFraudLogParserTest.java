package ru.yandex.market.logshatter.parser.checkout;

import java.io.File;
import java.net.URL;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.reader.file.BufferedFileLogReader;

public class AntiFraudLogParserTest {

    @Test
    public void parse() throws Exception {
        URL resource = getClass().getClassLoader().getResource("market-checkouter-anti-fraud.log");
        BufferedFileLogReader reader = new BufferedFileLogReader(new File(resource.toURI()).toPath());

        AntiFraudLogParser parser = new AntiFraudLogParser();
        LogParserChecker logParserChecker = new LogParserChecker(parser);

        String line = reader.readLine();
        logParserChecker.check(line,
                1511205376,
                logParserChecker.getHost(),
                "fraudDetectedOlolol!",
                555L,
                "Buyer{id='null', uid=6666, uuid='null', yandexUid='YandexUidYandexUid', muid=null, ip='null', " +
                    "regionId=null, lastName='null', firstName='null', middleName='null', phone='null', " +
                    "email='null', dontCall='null', assessor='null', bindKey=null, beenCalled='false', " +
                    "unreadImportantEvents=null'}",
                "Payment{id=123123, orderId=null, basketId='BasketIdBasketId', status=null, substatus=null, " +
                    "uid=null, currency=null, totalAmount=null, creationDate=null, updateDate=null, " +
                    "statusUpdateDate=null, statusExpiryDate=null, fake=null, failReason='null', type=USER, " +
                    "prepayType=YANDEX_MONEY, paymentForm=null, balancePayMethodType=null}"
        );

        line = reader.readLine();
        logParserChecker.check(line,
                1511205434,
                logParserChecker.getHost(),
                "fraudDetectedOlolol!",
                555L,
                "Buyer{id='null', uid=6666, uuid='null', yandexUid='YandexUidYandexUid', muid=null, ip='null', " +
                    "regionId=null, lastName='null', firstName='null', middleName='null', phone='null', " +
                    "email='null', dontCall='null', assessor='null', bindKey=null, beenCalled='false', " +
                    "unreadImportantEvents=null'}",
                "Payment{id=123123, orderId=null, basketId='BasketIdBasketId', status=null, substatus=null, " +
                    "uid=null, currency=null, totalAmount=null, creationDate=null, updateDate=null, " +
                    "statusUpdateDate=null, statusExpiryDate=null, fake=null, failReason='null', type=USER, " +
                    "prepayType=YANDEX_MONEY, paymentForm=null, balancePayMethodType=null}"
        );

        line = reader.readLine();
        logParserChecker.check(line,
                1511205456,
                logParserChecker.getHost(),
                "someDetecter",
                444L,
                "",
                ""
        );

        line = reader.readLine();
        logParserChecker.check(line,
                1511293266,
                logParserChecker.getHost(),
                "FFPromoUserFraudDetector",
                0L,
                "Buyer{id='null', uid=105913357, uuid='null', yandexUid='null', muid=null, ip='5.172.24.48', " +
                    "regionId=11171, lastName='Черепахин', firstName='Антон', middleName='null', " +
                    "phone='+79090134810', email='anton.chao@yandex.ru', dontCall='false', assessor='false', " +
                    "bindKey=null, beenCalled='false', unreadImportantEvents=null'}",
                ""
        );
    }

}
