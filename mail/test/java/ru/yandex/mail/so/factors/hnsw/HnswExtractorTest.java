package ru.yandex.mail.so.factors.hnsw;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import com.github.jelmerk.knn.SearchResult;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.ConstFunction;
import ru.yandex.function.NullConsumer;
import ru.yandex.jni.fasttext.JniFastText;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorFactoryContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorsRegistry;
import ru.yandex.mail.so.factors.fasttext.FastTextEmbedding;
import ru.yandex.mail.so.factors.fasttext.FastTextEmbeddingExtractor;
import ru.yandex.mail.so.factors.samples.SamplesLoader;
import ru.yandex.mail.so.factors.types.SoFactorTypesRegistry;
import ru.yandex.stater.NullStatersRegistrar;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class HnswExtractorTest extends TestBase {
    private final JniFastText fastText;
    private final HnswExtractor relaxedExtractor;
    private final HnswExtractor greedExtractor;
    private final SamplesLoader samplesLoader;

    public HnswExtractorTest() throws Exception {
        fastText = new JniFastText(
            resource("pures-e15.model.bin"),
            resource(
                "search/wizard/data/wizard/RequestLimits/stopword.lst"));
        SoFactorsExtractorsRegistry registry =
            new SoFactorsExtractorsRegistry(
                NullStatersRegistrar.INSTANCE,
                new SoFactorTypesRegistry());
        SoFactorsExtractorFactoryContext context =
            new SoFactorsExtractorFactoryContext(
                null,
                registry,
                new ConstFunction<>(NullConsumer.instance()),
                new LongAdder(),
                new LongAdder(),
                Thread.currentThread().getThreadGroup(),
                // We don't expect any http clients in this test
                null,
                // As well as external data
                null,
                new HashMap<>(),
                new HashMap<>(),
                logger,
                null,
                0L);
        FastTextEmbeddingExtractor fastTextExtractor =
            new FastTextEmbeddingExtractor(
                "fasttext",
                context.readBodyAsIniConfig(
                    "model = " + resource("pures-e15.model.bin")
                    + "\nstop-word-list = "
                    + resource(
                        "search/wizard/data/wizard/RequestLimits"
                        + "/stopword.lst")));
        registry.registerExtractor("fasttext-test", fastTextExtractor);

        relaxedExtractor =
            new HnswExtractor(
                "relaxed",
                context,
                context.readBodyAsIniConfig(
                    "extractor = fasttext-test\n"
                    + "distance-type = relaxed\n"
                    + "sample-field-name = deobfuscated_pure_body"));

        greedExtractor =
            new HnswExtractor(
                "greed",
                context,
                context.readBodyAsIniConfig(
                    "extractor = fasttext-test\n"
                    + "distance-type = greed\n"
                    + "sample-field-name = deobfuscated_pure_body"));

        samplesLoader = new SamplesLoader();
        samplesLoader.subscribe(relaxedExtractor);
        samplesLoader.subscribe(greedExtractor);
        samplesLoader.loadFromFile(
            resource("spam-samples.json").toFile(),
            logger);
    }

    private FastTextEmbedding embedding(final String text) throws Exception {
        return new FastTextEmbedding(
            fastText.getDimension(),
            fastText.createDoc(text));
    }

    @Test
    public void testSize() throws Exception {
        {
            Assert.assertEquals(7, relaxedExtractor.indexSize());
        }
        {
            Assert.assertEquals(7, greedExtractor.indexSize());
        }
    }

    @Test
    public void testSomeString() throws Exception {
        String someString =
            "результативно начислен на ваш счёт консультируем подтвердить "
            + "сведения и дополнительно завершить получение финансовых средств"
            + " для подтверждения и приобретения пройдите здесь";
        List<SearchResult<BasicItem<FastTextEmbedding>, Float>> neighbours =
            relaxedExtractor.neighbours(embedding(someString), 1);

        YandexAssert.assertSize(1, neighbours);
        logger.info("Relaxed distance: " + neighbours.get(0).distance());
        Assert.assertEquals(
            "spam_samples_991949281_so_compains_171981210770334057",
            neighbours.get(0).item().id());

        neighbours = greedExtractor.neighbours(embedding(someString), 1);

        YandexAssert.assertSize(1, neighbours);
        logger.info("Greed distance: " + neighbours.get(0).distance());
        Assert.assertEquals(
            "spam_samples_991949281_so_compains_171981210770334057",
            neighbours.get(0).item().id());
    }

    @Test
    public void testExistSample() throws Exception {
        final String rawMessage = "{\"url" +
                "\":\"spam_samples_991949281_so_compains_171981210770328957\",\"revision\":\"247668979\"," +
                "\"docs\":[{\"hid\":\"1\",\"body_text\":\"\",\"hdr_to_normalized\":\"gotlober-maksim@yandex.ru\\n\"," +
                "\"hdr_to\":\"gotlober-maksim@yandex.ru\",\"hdr_from_normalized\":\"support@gsetalent.com\\n\"," +
                "\"hdr_from\":\"support@gsetalent.com\",\"hdr_to_email\":\"gotlober-maksim@yandex.ru\\n\"," +
                "\"content_type\":\"text/html\",\"received_date\":\"1585925827.422\",\"deobfuscated_pure_body\":\"hi " +
                "%ShortNumber% как с нуля получать от %ShortNumber% р за 5 минут простой работы %Uri% %ShortNumber% " +
                "thank you for creating your %Uri% account to confirm the registration please click on the link below" +
                " %Uri% please disregard this email if you did not create an %Uri% account %FirstName% %FirstName% " +
                "talent\",\"headers\":\"received: from mxback12j.mail.yandex.net (localhost [127.0.0.1])\\tby " +
                "mxback12j.mail.yandex.net with LMTP id ieHCmKx2MX-Qul6GDaJ\\tfor <gotlober-maksim@yandex.ru>; Fri, " +
                "03 Apr 2020 17:57:14 +0300\\nreceived: from mxfront9g.mail.yandex.net (mxfront9g.mail.yandex.net " +
                "[IPv6:2a02:6b8:0:1472:2741:0:8b7:160])\\tby mxback12j.mail.yandex.net (Yandex) with ESMTP id " +
                "980271165B47\\tfor <gotlober-maksim@yandex.ru>; Fri,  3 Apr 2020 17:57:14 +0300 (MSK)\\nreceived: " +
                "from gproxy1-pub.mail.unifiedlayer.com (gproxy1-pub.mail.unifiedlayer.com [69.89.25.95])\\tby " +
                "mxfront9g.mail.yandex.net (mxfront/Yandex) with ESMTPS id B03KJa0iPp-v7eONMPh;\\tFri, 03 Apr 2020 " +
                "17:57:07 +0300\\t(using TLSv1.2 with cipher ECDHE-RSA-AES128-GCM-SHA256 (128/128 bits))\\t(Client " +
                "certificate not present)\\nx-yandex-front: mxfront9g.mail.yandex.net\\nx-yandex-timemark: 1585925827" +
                ".422\\nauthentication-results: mxfront9g.mail.yandex.net; spf=pass (mxfront9g.mail.yandex.net: " +
                "domain of gsetalent.com designates 69.89.25.95 as permitted sender, rule=[ip4:69.89.16.0/20]) smtp" +
                ".mail=support@gsetalent.com; dkim=pass header.i=@gsetalent.com\\nx-yandex-suid-status: 1 " +
                "872303667\\nx-yandex-spam: 1\\nx-yandex-fwd: " +
                "MTc0NDk3MTY3MTUyODAwNDA5MzAsNzc1MzAwMTIxNzk2MjcwNDcxNQ==\\nreceived: from CMGW (unknown [10.9.0.13])" +
                "\\tby gproxy1.mail.unifiedlayer.com (Postfix) with ESMTP id F01E14743CE6A\\tfor " +
                "<gotlober-maksim@yandex.ru>; Fri,  3 Apr 2020 08:57:05 -0600 (MDT)\\nreceived: from md-80.webhostbox" +
                ".net ([209.99.16.56])\\tby cmsmtp with ESMTP\\tid KNknjh5JatoKZKNknjMOLZ; Fri, 03 Apr 2020 08:57:05 " +
                "-0600\\nx-authority-reason: nr=8\\nx-authority-analysis: v=2.2 cv=fZrd8wYF c=1 sm=1 tr=0 " +
                "a=IxX9mC4/qpByBck0klgYDA==:117 a=IxX9mC4/qpByBck0klgYDA==:17 a=dLZJa+xiwSxG16/P+YVxDGlgEgI=:19 " +
                "a=IkcTkHD0fZMA:10 a=cl8xLZFz6L8A:10 a=eTeykOgfSnUA:10 a=agnHPTu_AAAA:20 a=AxiFEUILAAAA:8 " +
                "a=AU7zb0LZySeshr7-yI0A:9 a=QEXdDO2ut3YA:10 a=rp2mkJuPfX8A:10 a=TWP4G6yYQysA:10 a=E3ORklNfnBgA:10 " +
                "a=8TEoSnXnvhYA:10 a=rw2kDPKs4ZGU99K9jHCx:22\\ndkim-signature: v=1; a=rsa-sha256; q=dns/txt; " +
                "c=relaxed/relaxed;\\td=gsetalent.com; s=default; " +
                "h=Content-Transfer-Encoding:Content-Type:\\tMIME-Version:To:From:Subject:Date:Message-ID:Sender" +
                ":Reply-To:Cc:Content-ID:\\tContent-Description:Resent-Date:Resent-From:Resent-Sender:Resent-To" +
                ":Resent-Cc\\t:Resent-Message-ID:In-Reply-To:References:List-Id:List-Help:List-Unsubscribe:\\tList" +
                "-Subscribe:List-Post:List-Owner:List-Archive;\\tbh=rosIGa8XOurqml4fucxwvXCMyVR6ATXRCjshS8qLKO0=; " +
                "b=YQTtgMAay38PRYJ4v//dZGSOhG\\tTda33CCiTX3IcGykfDbgDoKjWFVJyEkgCo9H2XP9RZwlmM" +
                "++FhBeczLr6Py1qtkLuBhzFojBJ5ZQ2\\tXbjbWA98mwPE+rcYGYf2ahZ/SsvPAzIezq9rQSZ796JIahyXT/+7" +
                "+nFENgdgDYNnZiP2sMJTEovWz\\tiQTLiYskl6tpaw9jep9kXE9z/uJnxpYThzW15FRoeDolRelW9Zq6ovc5mrWkrvl4JIYxU3" +
                "/AGS2g3\\tew/UfG5qwrZwoDfX4Oa+CutsCX3BIj6ykWuSrqiRsPVuACsS67lgnZ8cTmr0OVBRAso7QL1r6cq7+\\tk8Kx+G+Q" +
                "==;\\nreceived: from [127.0.0.1] (port=44224 helo=gsetalent.com)\\tby md-80.webhostbox.net with " +
                "esmtpa (Exim 4.92)\\t(envelope-from <support@gsetalent.com>)\\tid 1jKNkn-00GrmS-A1\\tfor " +
                "gotlober-maksim@yandex.ru; Fri, 03 Apr 2020 14:57:05 +0000\\nmessage-id: " +
                "<9fae0a9848af6a15348b0b71f750029e@gsetalent.com>\\ndate: Fri, 03 Apr 2020 14:57:05 +0000\\nsubject: " +
                "Confirm Registration\\nfrom: support@gsetalent.com\\nto: gotlober-maksim@yandex.ru\\nmime-version: 1" +
                ".0\\ncontent-type: text/html; charset=utf-8\\ncontent-transfer-encoding: " +
                "quoted-printable\\nx-antiabuse: This header was added to track abuse, please include it with any " +
                "abuse report\\nx-antiabuse: Primary Hostname - md-80.webhostbox.net\\nx-antiabuse: Original Domain -" +
                " yandex.ru\\nx-antiabuse: Originator/Caller UID/GID - [47 12] / [47 12]\\nx-antiabuse: Sender " +
                "Address Domain - gsetalent.com\\nx-bwhitelist: no\\nx-source-ip: 127.0.0.1\\nx-source-l: " +
                "Yes\\nx-exim-id: 1jKNkn-00GrmS-A1\\nx-source: \\nx-source-args: \\nx-source-dir: \\nx-source-sender:" +
                " (gsetalent.com) [127.0.0.1]:44224\\nx-source-auth: support@gsetalent.com\\nx-email-count: " +
                "19\\nx-source-cap: bW9kZWxtcmk7bW9kZWxtcmk7bWQtODAud2ViaG9zdGJveC5uZXQ=\\nx-local-domain: " +
                "yes\\nreturn-path: support@gsetalent.com\\nx-yandex-forward: ea0d826661475bb35ff8e60650d2750d\"," +
                "\"unperson_pure_body\":\"Hi %ShortNumber_163%__***КАК С НУЛЯ ПОЛУЧАТЬ ОТ %ShortNumber_193%р. ЗА 5 " +
                "МИНУТ ПРОСТОЙ РАБОТЫ: %Uri_https_u.to_321146597% ***__%ShortNumber_156%,\\n%FirstName_2215894574% " +
                "you for creating your %Uri_http_gsetalent.com_797204161% account.\\nTo confirm the registration " +
                "please click on the link below:\\n%Uri_http_gsetalent.com_4240728337%\\nPlease disregard this email " +
                "if you did not create an %Uri_http_gsetalent.com_797204161% account.\\n%FirstName_167587396%," +
                "\\n%FirstName_818204924% Talent\",\"gateway_received_date\":\"1585925834\",\"html_body\":\"Hi " +
                "991__***КАК С НУЛЯ ПОЛУЧАТЬ ОТ 1000р. ЗА 5 МИНУТ ПРОСТОЙ РАБОТЫ: https://u.to/_tygFg  ***__570,<br " +
                "/><br />\\r\\n\\r\\nThank you for creating your gsetalent.com account.<br />\\r\\nTo confirm the " +
                "registration please click on the link below:<br />\\r\\nhttp://gsetalent" +
                ".com/validate/GFuF2D4c8Gxcj2Fg<br /><br />\\r\\n\\r\\nPlease disregard this email if you did not " +
                "create an gsetalent.com account.<br /><br />\\r\\n\\r\\nSincerely,<br />\\r\\nGSE " +
                "Talent\\r\\n\\r\\n\\r\\n\",\"unperson_subject\":\"Confirm Registration\"," +
                "\"smtp_id\":\"B03KJa0iPp-v7eONMPh\",\"pure_body\":\"Hi 991__***КАК С НУЛЯ ПОЛУЧАТЬ ОТ 1000р. ЗА 5 " +
                "МИНУТ ПРОСТОЙ РАБОТЫ: https://u.to/_tygFg ***__570,\\nThank you for creating your gsetalent.com " +
                "account.\\nTo confirm the registration please click on the link below:\\nhttp://gsetalent" +
                ".com/validate/GFuF2D4c8Gxcj2Fg\\nPlease disregard this email if you did not create an gsetalent.com " +
                "account.\\nSincerely,\\nGSE Talent\",\"hdr_subject\":\"Confirm Registration\"," +
                "\"built_date\":\"2020-04-11 11:28:54\",\"meta\":\"Content-Type:text/html; charset=UTF-8\"," +
                "\"hdr_from_email\":\"support@gsetalent.com\\n\",\"deobfuscated_subject\":\"confirm registration\"," +
                "\"parsed\":true,\"mimetype\":\"text/html\",\"x_urls\":\"https://u.to/_tygFg\\nhttp://gsetalent" +
                ".com\\nhttp://gsetalent.com/validate/GFuF2D4c8Gxcj2Fg\\n\"}]}";
        String id = "spam_samples_991949281_so_compains_171981210770328957";

        String text =
            relaxedExtractor.extractText(
                TypesafeValueContentHandler.parse(rawMessage)
                    .get("docs")
                    .asList(),
                null);
        List<SearchResult<BasicItem<FastTextEmbedding>, Float>> neighbours =
            relaxedExtractor.neighbours(embedding(text), 1);

        YandexAssert.assertSize(1, neighbours);
        Assert.assertEquals(id, neighbours.get(0).item().id());
        Assert.assertEquals(0., neighbours.get(0).distance(), 1e-3);

        text =
            greedExtractor.extractText(
                TypesafeValueContentHandler.parse(rawMessage)
                    .get("docs")
                    .asList(),
                null);
        neighbours = greedExtractor.neighbours(embedding(text), 1);

        YandexAssert.assertSize(1, neighbours);
        Assert.assertEquals(id, neighbours.get(0).item().id());
        Assert.assertEquals(0., neighbours.get(0).distance(), 1e-3);
    }

    @Test
    public void testExistSampleByText() throws Exception {
        final String text = "здравствуйте в рамках благотворительной акции по поддержке населения во время " +
                "самоизоляции русское лото дарит вам 1 беспроигрышный билет на онлайн розыгрыш всероссийской " +
                "официальной лотереи призовой фонд тиража составляет более одного миллиарда rub главный приз тиража " +
                "более 111 %ShortNumber% %ShortNumber% рублей чтобы зарегистрировать ваш билет и участвовать в " +
                "розыгрыше переходите на официальный сайт российского лото по ссылке %Uri% %Password%";
        List<SearchResult<BasicItem<FastTextEmbedding>, Float>> neighbours =
            relaxedExtractor.neighbours(embedding(text), 1);

        YandexAssert.assertSize(1, neighbours);
        Assert.assertEquals(
            "spam_samples_991949281_so_compains_172262685747116309",
            neighbours.get(0).item().id());
        Assert.assertEquals(0, neighbours.get(0).distance(), 1e-3);

        // Check stop words
        neighbours =
            relaxedExtractor.neighbours(
                embedding(text + " the they который"),
                1);

        YandexAssert.assertSize(1, neighbours);
        Assert.assertEquals(
            "spam_samples_991949281_so_compains_172262685747116309",
            neighbours.get(0).item().id());
        Assert.assertEquals(0, neighbours.get(0).distance(), 1e-3);

        neighbours = greedExtractor.neighbours(embedding(text), 1);
        YandexAssert.assertSize(1, neighbours);
        Assert.assertEquals(
            "spam_samples_991949281_so_compains_172262685747116309",
            neighbours.get(0).item().id());
        Assert.assertEquals(0, neighbours.get(0).distance(), 1e-3);
    }
}

