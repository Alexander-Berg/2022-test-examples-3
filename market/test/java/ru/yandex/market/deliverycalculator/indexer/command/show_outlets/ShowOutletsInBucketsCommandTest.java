package ru.yandex.market.deliverycalculator.indexer.command.show_outlets;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.common.mds.s3.client.content.consumer.FileContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.indexer.command.AbstractTmsCommandTest;
import ru.yandex.market.deliverycalculator.indexer.constant.PbSnMagicConstant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ShowOutletsInBucketsCommandTest extends AbstractTmsCommandTest {

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private ShowOutletsInBucketsCommand showOutletsInBucketsCommand;

    @BeforeEach
    void init() {
        when(resourceLocationFactory.createLocation(any()))
                .thenAnswer(invocation -> ResourceLocation.create("bucket-name", "key"));
        when(mdsS3Client.contains(any())).thenReturn(true);
    }

    @Test
    void bucketsV1InTariffTest() {
        File feedDeliveryOptionsRespFile = createFeedDeliveryOptionsRespFile(
                createFeedDeliveryOptionsRespV1Message()
        );
        when(mdsS3Client.download(any(), any(FileContentConsumer.class)))
                .thenAnswer(invocation -> feedDeliveryOptionsRespFile);

        executeCommand(
                Map.of(
                        "tariff-url", "http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/some-path"
                )
        );

        executeCommand(
                Map.of(
                        "tariff-url", "http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/some-path",
                        "region-id", "213"
                )
        );

        String terminalData = executeCommand(
                Map.of(
                        "tariff-url", "http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/some-path",
                        "region-id", "213",
                        "bucket-id", "100001"
                )
        );

        System.out.println(terminalData);

        String expectedTerminalData =
                "Searching with params:\n" +
                "tariff-url=http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/some-path\n" +
                "region-id=all\n" +
                "bucket-id=all\n" +
                "buckets-type=PICKUP\n" +
                "buckets-version=V1\n" +
                "print-list=false\n" +
                "\n" +
                "Unique outlets count: 7\n" +
                "\n" +
                "Searching with params:\n" +
                "tariff-url=http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/some-path\n" +
                "region-id=213\n" +
                "bucket-id=all\n" +
                "buckets-type=PICKUP\n" +
                "buckets-version=V1\n" +
                "print-list=false\n" +
                "\n" +
                "Unique outlets count: 4\n" +
                "\n" +
                "Searching with params:\n" +
                "tariff-url=http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/some-path\n" +
                "region-id=213\n" +
                "bucket-id=100001\n" +
                "buckets-type=PICKUP\n" +
                "buckets-version=V1\n" +
                "print-list=false\n" +
                "\n" +
                "Unique outlets count: 3";
        Assertions.assertEquals(expectedTerminalData, terminalData);

        FileUtils.deleteQuietly(feedDeliveryOptionsRespFile);
    }

    @Test
    void bucketsV2InTariffTest() {
        File feedDeliveryOptionsRespFile = createFeedDeliveryOptionsRespFile(
                createFeedDeliveryOptionsRespV2Message()
        );
        when(mdsS3Client.download(any(), any(FileContentConsumer.class)))
                .thenAnswer(invocation -> feedDeliveryOptionsRespFile);

        executeCommand(
                Map.of(
                        "tariff-url", "http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/path-2"
                )
        );

        executeCommand(
                Map.of(
                        "tariff-url", "http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/path-2",
                        "buckets-version", "V2"
                )
        );

        String terminalData = executeCommand(
                Map.of(
                        "tariff-url", "http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/path-2",
                        "buckets-type", "POST",
                        "buckets-version", "V2"
                )
        );

        String expectedTerminalData =
                "Searching with params:\n" +
                "tariff-url=http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/path-2\n" +
                "region-id=all\n" +
                "bucket-id=all\n" +
                "buckets-type=PICKUP\n" +
                "buckets-version=V1\n" +
                "print-list=false\n" +
                "\n" +
                "There is no buckets of specified type in tariff\n" +
                "\n" +
                "Searching with params:\n" +
                "tariff-url=http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/path-2\n" +
                "region-id=all\n" +
                "bucket-id=all\n" +
                "buckets-type=PICKUP\n" +
                "buckets-version=V2\n" +
                "print-list=false\n" +
                "\n" +
                "Unique mbi outlets count: 1\n" +
                "Unique lms outlets count: 3\n" +
                "\n" +
                "Searching with params:\n" +
                "tariff-url=http://market-mbi-dev.s3.mdst.yandex.net/delivery-calculator/buckets/path-2\n" +
                "region-id=all\n" +
                "bucket-id=all\n" +
                "buckets-type=POST\n" +
                "buckets-version=V2\n" +
                "print-list=false\n" +
                "\n" +
                "Unique mbi outlets count: 3\n" +
                "Unique lms outlets count: 2";
        Assertions.assertEquals(expectedTerminalData, terminalData);

        FileUtils.deleteQuietly(feedDeliveryOptionsRespFile);
    }

    private String executeCommand(Map<String, String> options) {
        final CommandInvocation commandInvocation = commandInvocation("show-outlets-in-buckets", options);
        showOutletsInBucketsCommand.executeCommand(commandInvocation, terminal());
        return terminalData();
    }

    private static DeliveryCalcProtos.FeedDeliveryOptionsResp createFeedDeliveryOptionsRespV1Message() {
        return DeliveryCalcProtos.FeedDeliveryOptionsResp.newBuilder()
                .setDeliveryOptionsByFeed(
                        DeliveryCalcProtos.DeliveryOptions.newBuilder()
                                .addPickupBuckets(
                                        DeliveryCalcProtos.PickupBucket.newBuilder()
                                                .setBucketId(100001)
                                                .addCarrierIds(52)
                                                .setProgram(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM)
                                                .setTariffId(1647)
                                                .setCurrency("RUR")
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(213)
                                                                .setOutletId(50001)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(213)
                                                                .setOutletId(50002)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(213)
                                                                .setOutletId(50003)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(687)
                                                                .setOutletId(60001)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .build()
                                )
                                .addPickupBuckets(
                                        DeliveryCalcProtos.PickupBucket.newBuilder()
                                                .setBucketId(100002)
                                                .addCarrierIds(52)
                                                .setProgram(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM)
                                                .setTariffId(1647)
                                                .setCurrency("RUR")
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(213)
                                                                .setOutletId(50001)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(213)
                                                                .setOutletId(50002)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(213)
                                                                .setOutletId(50004)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(687)
                                                                .setOutletId(60001)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(687)
                                                                .setOutletId(60002)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .build()
                                )
                                .addPickupBuckets(
                                        DeliveryCalcProtos.PickupBucket.newBuilder()
                                                .setBucketId(100003)
                                                .addCarrierIds(52)
                                                .setProgram(DeliveryCalcProtos.ProgramType.MARKET_DELIVERY_PROGRAM)
                                                .setTariffId(1647)
                                                .setCurrency("RUR")
                                                .addDeliveryOptionGroupOutlets(
                                                        DeliveryCalcProtos.DeliveryOptionsGroupOutlet.newBuilder()
                                                                .setRegion(893)
                                                                .setOutletId(70001)
                                                                .setOptionGroupId(111999)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .setResponseCode(200)
                .build();
    }

    private static DeliveryCalcProtos.FeedDeliveryOptionsResp createFeedDeliveryOptionsRespV2Message() {
        return DeliveryCalcProtos.FeedDeliveryOptionsResp.newBuilder()
                .setDeliveryOptionsByFeed(
                        DeliveryCalcProtos.DeliveryOptions.newBuilder()
                                .addPickupBucketsV2(
                                        DeliveryCalcProtos.PickupOptionsBucket.newBuilder()
                                                .setBucketId(11111)
                                                .addCarrierIds(42)
                                                .setProgram(DeliveryCalcProtos.ProgramType.DAAS)
                                                .setTariffId(6723)
                                                .setCurrency("RUR")
                                                .addPickupDeliveryRegions(
                                                        DeliveryCalcProtos.PickupDeliveryRegion.newBuilder()
                                                                .setRegionId(213)
                                                                .addOutletGroups(
                                                                        DeliveryCalcProtos.OutletGroup.newBuilder()
                                                                                .addOutletId(301)
                                                                                .addAllLmsOutletId(
                                                                                        Arrays.asList(501L, 502L, 503L)
                                                                                )
                                                                                .setDimensions(
                                                                                        DeliveryCalcProtos.OutletDimensions.newBuilder()
                                                                                                .setWidth(1.)
                                                                                                .setHeight(1.)
                                                                                                .setLength(1.)
                                                                                                .setDimSum(3.)
                                                                                                .build()
                                                                                )
                                                                                .build()
                                                                )
                                                                .setOptionGroupId(199)
                                                                .build()
                                                )
                                                .build()
                                )
                                .addPostBucketsV2(
                                        DeliveryCalcProtos.PickupOptionsBucket.newBuilder()
                                                .setBucketId(22222)
                                                .addCarrierIds(42)
                                                .setProgram(DeliveryCalcProtos.ProgramType.DAAS)
                                                .setTariffId(6723)
                                                .setCurrency("RUR")
                                                .addPickupDeliveryRegions(
                                                        DeliveryCalcProtos.PickupDeliveryRegion.newBuilder()
                                                                .setRegionId(782)
                                                                .addOutletGroups(
                                                                        DeliveryCalcProtos.OutletGroup.newBuilder()
                                                                                .addAllOutletId(
                                                                                        Arrays.asList(11L, 12L, 13L)
                                                                                )
                                                                                .addAllLmsOutletId(
                                                                                        Arrays.asList(33L, 44L)
                                                                                )
                                                                                .setDimensions(
                                                                                        DeliveryCalcProtos.OutletDimensions.newBuilder()
                                                                                                .setWidth(1.)
                                                                                                .setHeight(1.)
                                                                                                .setLength(1.)
                                                                                                .setDimSum(3.)
                                                                                                .build()
                                                                                )
                                                                                .build()
                                                                )
                                                                .setOptionGroupId(199)
                                                                .build()
                                                )
                                                .build()
                                )
                )
                .setResponseCode(200)
                .build();
    }

    private static File createFeedDeliveryOptionsRespFile(DeliveryCalcProtos.FeedDeliveryOptionsResp message) {
        File feedDeliveryOptionsRespFile = TempFileUtils.createTempFile();
        feedDeliveryOptionsRespFile.deleteOnExit();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(feedDeliveryOptionsRespFile))) {
            PbSnUtils.writePbSnMessage(PbSnMagicConstant.FEED_DELIVERY_OPTIONS_RESP, message, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return feedDeliveryOptionsRespFile;
    }
}
