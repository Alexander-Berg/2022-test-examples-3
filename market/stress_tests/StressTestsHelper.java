package ru.yandex.autotests.market.services.matcher.stress_tests;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.yandex.autotests.market.OfferData;
import ru.yandex.market.ir.http.Matcher;

import static com.google.common.collect.Iterables.transform;

class StressTestsHelper {
    private static final Logger LOG = LogManager.getLogger();
    private static final String REQUEST_METHOD = "POST";
    private static final String URI_ENDPOINT = "MatchBatch";
    private static final String TANK_AMMO_FILE_NAME =
            System.getProperty("requests.tank.ammo.file.name", "requests_tank_ammo_file");
    private static final String PROTOBUF_EXAMPLE_FILE_NAME =
            System.getProperty("requests.protobuf.example.file.name", "requests_protobuf_example");

    private final int offerBatchSize;
    private final int offerBatchCount;
    private final String testingMatcherHostUri;

    StressTestsHelper(String testingHostUri, int offerBatchSize, int offerBatchCount) {
        this.testingMatcherHostUri = testingHostUri;
        this.offerBatchSize = offerBatchSize;
        this.offerBatchCount = offerBatchCount;
    }

    void prepareStressTestsSourceFiles(Iterator<OfferData<Matcher.Offer>> offers) {
        LOG.info("Prepare matcher stress tests {} ({}x{} size x batchSize) for {}",
                TANK_AMMO_FILE_NAME, offerBatchCount, offerBatchSize, testingMatcherHostUri);
        LOG.info("Prepare {} for manual matcher tests", PROTOBUF_EXAMPLE_FILE_NAME);
        Spliterator<List<OfferData<Matcher.Offer>>> spliterator =
                Spliterators.spliteratorUnknownSize(Iterators.partition(offers, offerBatchSize), Spliterator.ORDERED);
        writeRequests(StreamSupport.stream(spliterator, false)
                .flatMap(toOfferBatch())
                .collect(Collectors.toList()));
        LOG.info("Files prepared successfully");
    }

    void writeRequests(List<Matcher.OfferBatch> offerBatches) {
        for (int i = 0; i < offerBatches.size(); i++) {
            if (i == 0) {
                writeRequest(PROTOBUF_EXAMPLE_FILE_NAME, offerBatches.get(i), false, false);
            }
            writeRequest(TANK_AMMO_FILE_NAME, offerBatches.get(i), i != 0, true);
        }
    }

    private void writeRequest(String fileName, Matcher.OfferBatch offerBatch, boolean isAppend, boolean isAmmo) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName, isAppend)) {
            byte[] offersBatchBytes = offerBatch.toByteArray();
            StringBuilder requestBuilder = new StringBuilder();
            if (isAmmo) {
                requestBuilder.append(String.format("%s /%s HTTP/1.1\r\n", REQUEST_METHOD, URI_ENDPOINT))
                        .append(String.format("Content-Length: %d\r\n", offersBatchBytes.length))
                        .append(String.format("Host: %s\r\n\r\n", testingMatcherHostUri));
                int ammoLenBytes =
                        requestBuilder.toString().getBytes(StandardCharsets.UTF_8).length + offersBatchBytes.length;
                requestBuilder.insert(0, String.format("%d\n", ammoLenBytes));
            }
            fileOutputStream.write(requestBuilder.toString().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.write(offersBatchBytes);
            if (isAmmo) {
                fileOutputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            LOG.error("Error during writing matcher stress tests sources", e);
        }
    }

    private Function<List<OfferData<Matcher.Offer>>, Stream<Matcher.OfferBatch>> toOfferBatch() {
        return input -> {
            List<Matcher.OfferBatch> offerBatch = new ArrayList<>();
            offerBatch.add(offersToRequest(input));
            return offerBatch.stream();
        };
    }

    private static Matcher.OfferBatch offersToRequest(Iterable<OfferData<Matcher.Offer>> offers) {
        return Matcher.OfferBatch.newBuilder()
                .addAllOffer(transform(offers, input -> input.getOffer())).build();
    }
}
