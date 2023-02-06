package ru.yandex.market.loyalty.admin.yt;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampPromo;
import com.google.protobuf.ByteString;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.loyalty.admin.utils.PromoYtDataChecker;
import ru.yandex.market.loyalty.admin.yt.YtPath.YtCluster;
import ru.yandex.market.loyalty.admin.yt.exception.LoyaltyYtException;
import ru.yandex.market.loyalty.admin.yt.fallback.FallbackYtTableClientProxy;
import ru.yandex.market.loyalty.admin.yt.fallback.YtTableClient;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import Market.Promo.Promo.PromoDetails;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Component
public class PromoYtTestHelper {

    private final FallbackYtTableClientProxy clientProxy;

    public PromoYtTestHelper(FallbackYtTableClientProxy clientProxy) {
        this.clientProxy = clientProxy;
    }

    @SuppressWarnings("unchecked")
    public <T> T withMock(
            @Nonnull ThrowableConsumer<YtreeDataBuilder> dataBuilder,
            @Nonnull Supplier<T> accepter
    ) {
        checkNotNull(dataBuilder);
        checkNotNull(accepter);

        final Map<YtCluster, YtTableClient> originalClients = clientProxy.getYtClients();
        try {
            final Map<YtCluster, YtTableClient> clientMap = Map.of(
                    YtCluster.HAHN, mock(YtTableClient.class),
                    YtCluster.ARNOLD, mock(YtTableClient.class)
            );
            clientProxy.setYtClients(clientMap);

            try {
                YtreeDataBuilder db = new YtreeDataBuilder();
                dataBuilder.accept(db);
                for (YtTableClient clientMock : clientMap.values()) {
                    doAnswer(ans -> {
                        db.build().forEach(ans.getArgument(1));
                        return null;
                    }).when(clientMock)
                            .readTable(any(YtPath.class), any(Consumer.class));

                    when(clientMock.readCreationTime(any(YtPath.class)))
                            .thenReturn(LocalDateTime.now());
                }
            } catch (Exception e) {
                for (YtTableClient clientMock : clientMap.values()) {
                    doAnswer(ans -> {
                        throw new LoyaltyYtException(e.getMessage(), e);
                    }).when(clientMock)
                            .readTable(any(YtPath.class), any(Consumer.class));

                    when(clientMock.readCreationTime(any(YtPath.class)))
                            .thenReturn(LocalDateTime.now());
                }
            }
            return accepter.get();
        } finally {
            clientProxy.setYtClients(originalClients);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T withPromoStorageMock(
            @Nonnull ThrowableConsumer<YtreeDataPromoStorageBuilder> dataBuilder,
            @Nonnull Supplier<T> accepter
    ) {
        checkNotNull(dataBuilder);
        checkNotNull(accepter);

        final Map<YtCluster, YtTableClient> originalClients = clientProxy.getYtClients();
        try {
            final Map<YtCluster, YtTableClient> clientMap = Map.of(
                    YtCluster.HAHN, mock(YtTableClient.class),
                    YtCluster.ARNOLD, mock(YtTableClient.class)
            );
            clientProxy.setYtClients(clientMap);

            try {
                YtreeDataPromoStorageBuilder db = new YtreeDataPromoStorageBuilder();
                dataBuilder.accept(db);
                for (YtTableClient clientMock : clientMap.values()) {
                    doAnswer(ans -> {
                        db.build().forEach(ans.getArgument(1));
                        return null;
                    }).when(clientMock)
                            .readTable(any(YtPath.class), any(Consumer.class));

                    when(clientMock.readCreationTime(any(YtPath.class)))
                            .thenReturn(LocalDateTime.now());
                }
            } catch (Exception e) {
                for (YtTableClient clientMock : clientMap.values()) {
                    doAnswer(ans -> {
                        throw new LoyaltyYtException(e.getMessage(), e);
                    }).when(clientMock)
                            .readTable(any(YtPath.class), any(Consumer.class));

                    when(clientMock.readCreationTime(any(YtPath.class)))
                            .thenReturn(LocalDateTime.now());
                }
            }
            return accepter.get();
        } finally {
            clientProxy.setYtClients(originalClients);
        }
    }

    public void usingMock(
            @Nonnull ThrowableConsumer<YtreeDataBuilder> dataBuilder,
            @Nonnull Runnable accepter
    ) {
        withMock(dataBuilder, () -> {
            accepter.run();
            return null;
        });
    }

    public void usingPromoStorageMock(
            @Nonnull ThrowableConsumer<YtreeDataPromoStorageBuilder> dataBuilder,
            @Nonnull Runnable accepter
    ) {
        withPromoStorageMock(dataBuilder, () -> {
            accepter.run();
            return null;
        });
    }

    @Nonnull
    public static Map.Entry<YtSource, ThrowableConsumer<YtreeDataBuilder>> sourceBuilder(
            @Nonnull YtSource source,
            @Nonnull ThrowableConsumer<YtreeDataBuilder> dataBuilder
    ) {
        return Map.entry(source, dataBuilder);
    }

    public void addNullRecord(YtreeDataBuilder dataBuilder) {
        dataBuilder.promo(
                PromoYtDataChecker.ZERO_FEED_ID,
                PromoYtDataChecker.ZERO_PROMO_KEY,
                PromoDetails.newBuilder()
        );
    }

    public void addNullPromoStorageRecord(YtreeDataPromoStorageBuilder dataBuilder) {
        dataBuilder.promo(
                PromoYtDataChecker.ZERO_FEED_ID,
                PromoYtDataChecker.ZERO_PROMO_KEY,
                NMarket.Common.Promo.Promo.ESourceType.ANAPLAN,
                DataCampPromo.PromoDescription.newBuilder()
        );
    }

    public static class YtreeDataBuilder {
        private Map<Pair<Long, String>, YTreeMapNode> bundles = new HashMap<>();

        @SuppressWarnings("unchecked")
        public YtreeDataBuilder promo(long feedId, String promoKey, PromoDetails.Builder details) {
            bundles.put(Pair.of(feedId, details.getShopPromoId()), mockNode(feedId, promoKey,
                    details.setBinaryPromoMd5(ByteString.copyFrom(DigestUtils.md5(promoKey)))
                            .setFeedId((int) feedId)
                            .build().toByteArray()
            ));
            return this;
        }

        private YTreeMapNode mockNode(Long feedId, String promoId, byte[] promo) {
            YTreeMapNode mockNode = mock(YTreeMapNode.class);
            when(mockNode.getLong("feed_id")).thenReturn(feedId);
            when(mockNode.getBytes("promo")).thenReturn(promo);
            when(mockNode.getString("promo_id")).thenReturn(promoId);
            return mockNode;
        }

        public Collection<YTreeMapNode> build() {
            return bundles.values();
        }
    }

    public static class YtreeDataPromoStorageBuilder {
        private Map<Pair<Integer, String>, YTreeMapNode> bundles = new HashMap<>();

        @SuppressWarnings("unchecked")
        public YtreeDataPromoStorageBuilder promo(
                int businessId, String promoStorageId,
                NMarket.Common.Promo.Promo.ESourceType source,
                DataCampPromo.PromoDescription.Builder description
        ) {
            bundles.put(
                    Pair.of(
                            businessId,
                            description.getPrimaryKey().getPromoId()
                    ),
                    mockNode(
                            businessId,
                            promoStorageId,
                            source.getNumber(),
                            description
                                    .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                            .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                                            .build())
                                    .build().toByteArray()
                    )
            );
            return this;
        }

        public YtreeDataPromoStorageBuilder promo(
                DataCampPromo.PromoDescription description
        ) {
            bundles.put(
                    Pair.of(
                            description.getPrimaryKey().getBusinessId(),
                            description.getPrimaryKey().getPromoId()
                    ),
                    mockNode(
                            description.getPrimaryKey().getBusinessId(),
                            description.getPrimaryKey().getPromoId(),
                            description.getPrimaryKey().getSource().getNumber(),
                            description.toByteArray()
                    )
            );
            return this;
        }

        private YTreeMapNode mockNode(Integer feedId, String promoId, int source, byte[] promo) {
            YTreeMapNode mockNode = mock(YTreeMapNode.class);
            when(mockNode.getInt("business_id")).thenReturn(feedId);
            when(mockNode.getLong("business_id")).thenReturn(Long.valueOf(feedId));
            when(mockNode.getBytes("promo")).thenReturn(promo);
            when(mockNode.getString("promo_id")).thenReturn(promoId);
            when(mockNode.getInt("source")).thenReturn(source);
            return mockNode;
        }

        public Collection<YTreeMapNode> build() {
            return bundles.values();
        }
    }

    @FunctionalInterface
    public static interface ThrowableConsumer<T> {
        void accept(T t) throws Exception;
    }

}
