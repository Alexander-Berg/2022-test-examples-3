package ru.yandex.market.bidding.offerbids;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.bidding.ExchangeProtos;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfferBidsTestUtils {
    static ExchangeProtos.Parcel createParcel() {
        return createParcel(456L, 123L);
    }

    static ExchangeProtos.Parcel createParcel(long partnerId, long feedId) {
        return ExchangeProtos.Parcel.newBuilder()
                .addBids(ExchangeProtos.Bid.newBuilder()
                        .setPartnerId(partnerId)
                        .setFeedId(feedId)
                        .build())
                .build();
    }

    static ExchangeProtos.Parcel createParcelWithManyBids(int bidsCount, long partnerId) {
        return IntStream.range(0, bidsCount)
                .mapToObj(feedId -> ExchangeProtos.Bid.newBuilder()
                        .setPartnerId(partnerId)
                        .setFeedId(feedId)
                        .build())
                .collect(Collector.of(
                        ExchangeProtos.Parcel::newBuilder,
                        ExchangeProtos.Parcel.Builder::addBids,
                        (b1, b2) -> b1.addAllBids(b2.getBidsList()),
                        ExchangeProtos.Parcel.Builder::build
                ));
    }

    static MessageBatch createMessageBatch(byte[] bytes) {

        var messageData = mock(MessageData.class);
        when(messageData.getDecompressedData()).thenReturn(bytes);

        return new MessageBatch(
                "someTopic",
                1,
                List.of(messageData)
        );
    }
}
