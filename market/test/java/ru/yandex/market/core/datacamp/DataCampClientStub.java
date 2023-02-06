package ru.yandex.market.core.datacamp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncSearch;
import com.google.protobuf.Timestamp;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

/**
 * Планируется выпилить этот класс целиком. Тикет: MBI-64995
 * Вместо него используйте моки.
 */
@Deprecated
public class DataCampClientStub extends DataCampClient {
    /**
     * Если id партнера заканчивается на этот суфикс, считаем, у него нет офферов.
     */
    public static final long NO_OFFER_PARTNER_ID_SUFFIX = 321;

    private static final Comparator<String> KEY_COMPARATOR = String::compareTo;
    private static final Comparator<DataCampOffer.Offer> OFFER_COMPARATOR =
            Comparator.comparing(DataCampClientStub::getOfferKey);

    private final List<DataCampOffer.Offer> primordialOffers;

    private List<DataCampOffer.Offer> offers;

    private final Map<String, Market.DataCamp.DataCampUnitedOffer.UnitedOffer> businessUnitedOffers = new HashMap<>();

    private final List<String> requestParams = new ArrayList<>();

    private boolean useFilterByPartnerId = false;

    public DataCampClientStub() throws IOException {
        this("stub.test.json");
    }

    public DataCampClientStub(String sourceFilename) throws IOException {
        super("", HttpClientBuilder.create().build());
        InputStream data = new ClassPathResource(sourceFilename, DataCampClientStub.class).getInputStream();
        SyncSearch.SearchResponse.Builder responseBuilder = SyncSearch.SearchResponse.newBuilder();
        JsonFormat.merge(new BufferedReader(new InputStreamReader(data)), responseBuilder);
        primordialOffers = responseBuilder.getOfferList().stream()
                .sorted(OFFER_COMPARATOR)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        reset();
    }

    public DataCampClientStub(List<DataCampOffer.Offer> offers) {
        super("", HttpClientBuilder.create().build());
        primordialOffers = offers;
        reset();
    }

    public void setUseFilterByPartnerId(boolean useFilterByPartnerId) {
        this.useFilterByPartnerId = useFilterByPartnerId;
    }

    private static String getOfferKey(DataCampOffer.Offer offer) {
        return offer.getIdentifiers().getOfferId();
    }

    private static boolean isNoOfferPartner(long partnerId) {
        return partnerId % 1000 == NO_OFFER_PARTNER_ID_SUFFIX;
    }

    public void reset() {
        offers = primordialOffers.stream()
                .map(offer -> DataCampOffer.Offer.newBuilder(offer).build())
                .collect(Collectors.toList());
        requestParams.clear();
        useFilterByPartnerId = false;
        businessUnitedOffers.clear();
    }

    @Override
    public SyncSearch.SearchResponse searchOffers(long partnerId, Long businessId, boolean isOriginal,
                                                  SyncSearch.SearchRequest request) {
        if (offers.isEmpty() || isNoOfferPartner(partnerId)) {
            return SyncSearch.SearchResponse.newBuilder()
                    .setMeta(SyncSearch.SearchMeta.newBuilder()
                            .setTotalAvailable(0)
                            .setTotalResponse(0)
                            .setPaging(SyncSearch.PageInfo.newBuilder().setIsLastPage(true).setIsFirstPage(true))
                    ).build();
        }

        Comparator<String> keyComparator = request.getForward()
                ? KEY_COMPARATOR
                : KEY_COMPARATOR.reversed();
        Comparator<DataCampOffer.Offer> comparator =
                Comparator.comparing(DataCampClientStub::getOfferKey, keyComparator);
        List<DataCampOffer.Offer> result = offers.stream()
                .filter(o -> !useFilterByPartnerId || partnerId == o.getIdentifiers().getShopId())
                .sorted(comparator)
                .dropWhile(o -> request.hasPosition() && keyComparator.compare(getOfferKey(o), request.getPosition()) <= 0)
                .limit(request.getPageSize())
                .sorted(OFFER_COMPARATOR)
                .collect(Collectors.toList());

        SyncSearch.PageInfo.Builder pageInfo = SyncSearch.PageInfo.newBuilder();

        boolean isFirst;
        boolean isLast;

        if (!result.isEmpty()) {
            String start = result.get(0).getIdentifiers().getOfferId();
            String end = result.get(result.size() - 1).getIdentifiers().getOfferId();

            isFirst = offers.stream().map(DataCampClientStub::getOfferKey).map(start::compareTo).noneMatch(i -> i > 0);
            isLast = offers.stream().map(DataCampClientStub::getOfferKey).map(end::compareTo).noneMatch(i -> i < 0);

            pageInfo.setStart(start);
            pageInfo.setEnd(end);
        } else {
            isFirst = !request.getForward();
            isLast = request.getForward();
        }

        pageInfo.setIsFirstPage(isFirst);
        pageInfo.setIsLastPage(isLast);


        return SyncSearch.SearchResponse.newBuilder()
                .addAllOffer(result)
                .setMeta(SyncSearch.SearchMeta.newBuilder()
                        .setTotalAvailable(offers.size())
                        .setTotalResponse(result.size())
                        .setPaging(pageInfo)
                ).build();
    }

    @Override
    public SearchBusinessOffersResult searchBusinessOffers(SearchBusinessOffersRequest searchRequest) {
        var builder = SearchBusinessOffersResult.builder();
        if (Optional.ofNullable(searchRequest.getPricePresence()).orElse(false)) {
            var offersWithPrice = offers.stream()
                    .filter(DataCampOffer.Offer::hasPrice)
                    .filter(o -> !useFilterByPartnerId || searchRequest.getPartnerId() == o.getIdentifiers().getShopId())
                    .map(o -> DataCampUnitedOffer.UnitedOffer.newBuilder()
                            .setBasic(o).build())
                    .collect(Collectors.toList());
            builder.setOffers(offersWithPrice);
            builder.setTotalCount(offersWithPrice.size());
        }
        return builder.build();
    }

    @Override
    public SyncGetOffer.GetOfferResponse getOffer(long partnerId, String offerId, Integer warehouseId,
                                                  @Nullable Long businessId) {
        requestParams.add(String.format("getOffer: %s, %s, %s", partnerId, offerId, warehouseId));
        DataCampOffer.Offer offer = findById(offerId);
        SyncGetOffer.GetOfferResponse.Builder builder = SyncGetOffer.GetOfferResponse.newBuilder();
        Optional.ofNullable(offer).ifPresent(builder::setOffer);
        return builder.build();
    }

    @Override
    public SyncChangeOffer.FullOfferResponse changeOfferStatus(long partnerId, String offerId,
                                                               @Nullable Long businessId,
                                                               DataCampOfferStatus.OfferStatus status
    ) {
        Timestamp ts = status.getDisabled(0).getMeta().getTimestamp();
        requestParams.add(String.format("changeStatus: %s, %s, %s", partnerId, offerId,
                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).toEpochMilli()));
        SyncChangeOffer.FullOfferResponse.Builder builder = SyncChangeOffer.FullOfferResponse.newBuilder();
        DataCampOffer.Offer offer = findById(offerId);
        if (offer != null) {
            DataCampOffer.Offer.Builder renewed = DataCampOffer.Offer.newBuilder(offer);
            renewed.setStatus(DataCampOfferStatus.OfferStatus.newBuilder().setPublishByPartner(
                    calculateStatus(status)).build());
            builder.addOffer(renewed.build());
            offers.remove(offer);
            offers.add(renewed.build());
        }

        return builder.build();
    }

    @Override
    public SyncChangeOffer.FullOfferResponse changeOfferPrice(long partnerId, String offerId,
                                                              @Nullable Long businessId,
                                                              DataCampOfferPrice.OfferPrice price
    ) {
        Timestamp ts = price.getBasic().getMeta().getTimestamp();
        requestParams.add(String.format("changePrice: %s, %s, %s", partnerId, offerId,
                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).toEpochMilli()));
        SyncChangeOffer.FullOfferResponse.Builder builder = SyncChangeOffer.FullOfferResponse.newBuilder();
        DataCampOffer.Offer offer = findById(offerId);
        if (offer != null) {
            DataCampOffer.Offer.Builder renewed = DataCampOffer.Offer.newBuilder(offer);
            renewed.setPrice(price);
            builder.addOffer(renewed.build());
            offers.remove(offer);
            offers.add(renewed.build());
        }
        return builder.build();
    }

    @Override
    public SyncChangeOffer.FullOfferResponse changeOfferPrices(long partnerId, Long businessId,
                                                               List<DataCampOffer.Offer> datacampOffers) {
        datacampOffers.forEach(o -> {
            Timestamp ts = o.getPrice().getBasic().getMeta().getTimestamp();
            requestParams.add(String.format("changePrice: %s, %s, %s, %s", partnerId, o.getIdentifiers().getOfferId(),
                    o.getIdentifiers().getWarehouseId(),
                    Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).toEpochMilli()));
        });
        SyncChangeOffer.FullOfferResponse.Builder builder = SyncChangeOffer.FullOfferResponse.newBuilder();
        datacampOffers.forEach(o -> {
            DataCampOfferIdentifiers.OfferIdentifiers identifiers = o.getIdentifiers();
            DataCampOffer.Offer offer = findById(identifiers.getOfferId());
            if (offer != null) {
                DataCampOffer.Offer.Builder renewed = DataCampOffer.Offer.newBuilder(offer);
                renewed.setPrice(o.getPrice());

                DataCampOfferIdentifiers.OfferIdentifiers.Builder identifierBuilder =
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder();
                identifierBuilder.setOfferId(identifiers.getOfferId());
                if (identifiers.hasWarehouseId()) {
                    identifierBuilder.setWarehouseId(identifiers.getWarehouseId());
                }
                renewed.setIdentifiers(identifierBuilder.build());
                builder.addOffer(renewed.build());
                offers.remove(offer);
                offers.add(renewed.build());
            }
        });
        return builder.build();
    }

    @Override
    public <T> HttpResponse changeSchema(long partnerId, Long businessId, T changeSchemaDTO) {
        throw new UnsupportedOperationException("Deprecated. Use mocked client");
    }

    @Override
    public SyncChangeOffer.FullOfferResponse deleteOffer(long partnerId, String offerId,
                                                         @Nullable Integer warehouseId, @Nullable Long businessId) {
        throw new UnsupportedOperationException("Deprecated. Use mocked client");
    }

    @Override
    public SyncChangeOffer.FullOfferResponse getOffers(long partnerId, Long businessId,
                                                       SyncChangeOffer.ChangeOfferRequest request) {
        throw new UnsupportedOperationException("Deprecated. Use mocked client");
    }

    @Override
    public SyncGetOffer.GetUnitedOffersResponse getBusinessUnitedOffer(long businessId,
                                                                       Collection<String> offerIds,
                                                                       @Nullable Long partnerId) {
        throw new UnsupportedOperationException("Deprecated. Use mocked client");
    }

    @Override
    public OffersBatch.UnitedOffersBatchResponse getBusinessUnitedOffers(long businessId,
                                                                         Collection<String> offerIds,
                                                                         Long partnerId) {
        throw new UnsupportedOperationException("Deprecated. Use mocked client");
    }

    public void addWarehouse(long partnerId, long feedId, long warehouseId) {
        requestParams.add(String.format("add_warehouse: %s, %s, %s", partnerId, feedId, warehouseId));
    }

    private DataCampOffer.Offer findById(String id) {
        return offers.stream()
                .filter(offer -> offer.getIdentifiers().getOfferId().equals(id))
                .findFirst().orElse(null);
    }

    private DataCampOfferStatus.SummaryPublicationStatus calculateStatus(DataCampOfferStatus.OfferStatus status) {
        DataCampOfferMeta.Flag flag =
                status.getDisabledList().stream().filter(DataCampOfferMeta.Flag::getFlag).findAny().orElse(null);
        return flag == null
                ? DataCampOfferStatus.SummaryPublicationStatus.AVAILABLE
                : DataCampOfferStatus.SummaryPublicationStatus.HIDDEN;
    }

    public List<String> getRequestParams() {
        return requestParams;
    }
}
