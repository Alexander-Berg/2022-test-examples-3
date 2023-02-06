package ru.yandex.market.deliverycalculator.workflow.test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import ru.yandex.market.common.test.matcher.JsonMatcher;
import ru.yandex.market.common.test.matcher.XmlMatcher;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.PickupOptionsBucket;
import ru.yandex.market.deliverycalculator.workflow.FeedParserWorkflowPreparedTariff;

import static ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.DeliveryOptions;
import static ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.DeliveryOptionsBucket;
import static ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.DeliveryOptionsGroup;
import static ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.PickupBucket;
import static ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos.PostBucket;
import static ru.yandex.market.deliverycalculator.test.TestUtils.extractFileContent;

@ParametersAreNonnullByDefault
public abstract class AbstractTariffWorkflowTest extends FunctionalTest {

    /**
     * Проверить на соответствие ожиданиям <i>protobuf</i> сообщения, переводя их в <i>json</i> формат и
     * сравнимая с указанными json файлами.
     *
     * @param expectedByActual ключ - <i>protobuf</i> сообщение, значение - путь до <i>json</i> файла, которому
     *                         соответствует сообщение, переведенное в <i>json</i> формат
     */
    protected void softlyAssertProtobufAsJson(Map<Message, String> expectedByActual) {
        expectedByActual.forEach((message, pathToJson) ->
                softlyAssertJsonEquals(JsonFormat.printToString(message),
                        extractFileContent(pathToJson)
                )
        );
    }

    /**
     * Проверить на соответствие ожиданиям <i>protobuf</i> сообщения, переводя их в <i>json</i> формат и
     * сравнимая с указанными json файлами.
     *
     * @param actual             ключ - id сообщения, значение - <i>protobuf</i> сообщение
     * @param clazz              класс, чей класслоадер может загрузить файл
     * @param prefixPathExpected префикс для формирования имён файлов с ожидаемыми данными
     */
    protected void softlyAssertProtobufAsJson(Map<? extends Number, ? extends Message> actual,
                                              Class<?> clazz, String prefixPathExpected) {
        actual.forEach((id, message) ->
                softlyAssertJsonEquals(JsonFormat.printToString(message),
                        StringTestUtil.getString(clazz, prefixPathExpected + id + ".json")
                )
        );
    }

    /**
     * Проверить на соответствие строки как <i>XML</i>.
     *
     * @param actualXml   актуальный <i>XML</i>
     * @param expectedXml ожидаемый <i>XML</i>
     */
    protected void softlyAssertXmlEquals(String actualXml, String expectedXml) {
        softlyAssertEquals(new XmlMatcher(expectedXml), actualXml);
    }

    private void softlyAssertJsonEquals(String dataset, String expectedDataset) {
        softlyAssertEquals(new JsonMatcher(expectedDataset), dataset);
    }

    private void softlyAssertEquals(Matcher<? extends String> matcher, String dataset) {
        Description description = new StringDescription();
        matcher.describeTo(description);
        softly.assertThat(dataset).matches(matcher::matches, buildErrorMessage(dataset, matcher, description));
    }

    @Nonnull
    private static String buildErrorMessage(
            String dataset,
            Matcher<? extends String> matcher,
            Description description
    ) {
        matcher.describeMismatch(dataset, description);
        return description.toString();
    }

    @Nonnull
    protected DeliveryOptions getDeliveryOptionsByFeed(FeedParserWorkflowPreparedTariff preparedTariff) {
        return preparedTariff.getFeedDeliveryOptionsResp(1, 1)
                .getDeliveryOptionsByFeed();
    }

    @Nonnull
    protected Map<Long, DeliveryOptionsGroup> groupOptionGroupById(DeliveryOptions deliveryOptionsByFeed) {
        return deliveryOptionsByFeed.getDeliveryOptionGroupsList().stream()
                .collect(Collectors.toMap(
                        DeliveryOptionsGroup::getDeliveryOptionGroupId,
                        Function.identity()
                ));
    }

    @Nonnull
    protected Map<Long, DeliveryOptionsBucket> groupCourierBucketsById(DeliveryOptions deliveryOptionsByFeed) {
        return deliveryOptionsByFeed.getDeliveryOptionBucketsList().stream()
                .collect(Collectors.toMap(
                        DeliveryOptionsBucket::getDeliveryOptBucketId,
                        Function.identity()
                ));
    }

    @Nonnull
    protected Map<Long, PickupBucket> groupPickupBucketsById(DeliveryOptions deliveryOptionsByFeed) {
        return deliveryOptionsByFeed.getPickupBucketsList().stream()
                .collect(Collectors.toMap(PickupBucket::getBucketId, Function.identity()));
    }

    @Nonnull
    protected Map<Long, PickupOptionsBucket> groupPickupOptionsBucketsById(DeliveryOptions deliveryOptionsByFeed) {
        return deliveryOptionsByFeed.getPickupBucketsV2List().stream()
                .collect(Collectors.toMap(PickupOptionsBucket::getBucketId, Function.identity()));
    }

    @Nonnull
    protected Map<Long, PostBucket> groupPostBucketsById(DeliveryOptions deliveryOptionsByFeed) {
        return deliveryOptionsByFeed.getPostBucketsList().stream()
                .collect(Collectors.toMap(PostBucket::getBucketId, Function.identity()));
    }
}
