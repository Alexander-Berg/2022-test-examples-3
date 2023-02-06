package ru.yandex.autotests.market.services.matcher.stress_tests;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import ru.yandex.autotests.market.OfferData;
import ru.yandex.autotests.market.ServiceOfferBuilder;
import ru.yandex.autotests.market.Utils;
import ru.yandex.autotests.market.services.matcher.MatcherOfferDataBuilder;
import ru.yandex.bolts.function.Function;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.ir.http.Matcher;

public class Main {
    private static final String TESTING_MATCHER_HOST_URI =
            System.getProperty("matcher.testing.host.uri",
                    "sas2-1744-bf9-sas-market-test--51a-10886.gencfg-c.yandex.net:10886");
    private static final int OFFER_BATCH_SIZE =
            Integer.parseInt(System.getProperty("matcher.offer.batch.size", "256"));
    private static final int OFFER_BATCH_COUNT =
            Integer.parseInt(System.getProperty("matcher.offer.batch.count", "24"));

    private Main() {
    }

    public static void main(String... args) {
        Yt yt = YtUtils.http(Utils.YT_HTTP_PROXY, Utils.YT_TOKEN);
        StressTestsHelper stressTestsHelper
                = new StressTestsHelper(TESTING_MATCHER_HOST_URI, OFFER_BATCH_SIZE, OFFER_BATCH_COUNT);
        ServiceOfferBuilder<Matcher.Offer> matcherOfferDataBuilder = new MatcherOfferDataBuilder();

        Function<Iterator<YTreeMapNode>, String> executeOffer = iterator -> {
            Iterator<OfferData<Matcher.Offer>> it = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                    .limit((long) OFFER_BATCH_SIZE * OFFER_BATCH_COUNT)
                    .map(matcherOfferDataBuilder::build)
                    .iterator();
            stressTestsHelper.prepareStressTestsSourceFiles(it);
            return null;
        };


        yt.tables().read(YPath.simple(Utils.getInputTablePath()), YTableEntryTypes.YSON, executeOffer);
    }
}
