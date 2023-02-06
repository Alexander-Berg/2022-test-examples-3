package ru.yandex.market.mboc.common.offers.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent;
import ru.yandex.market.mbo.jooq.repo.notification.RepositoryObserver;
import ru.yandex.market.mboc.common.offers.model.AntiMappingWithServiceOffers;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.model.OfferLite;
import ru.yandex.market.mboc.common.offers.model.OfferShortInfo;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.repository.search.OffersForServiceFilter;
import ru.yandex.market.mboc.common.offers.repository.search.serviceoffer.ServiceOfferQueryExtractor;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierHelper;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class OfferRepositoryMock implements OfferRepository {
    private final AtomicLong stampGenerator = new AtomicLong();
    private final AtomicLong idGenerator = new AtomicLong();
    private final AtomicLong lastVersionGenerator = new AtomicLong();

    private HashMap<Long, Offer> offersMap = new HashMap<>();

    private final HashMap<Long, Optional<String>> lastAuditStaffLogin = new HashMap<>();

    private final List<RepositoryObserver<Offer>> observers = new ArrayList<>();

    public OfferRepositoryMock setOffers(Offer... offers) {
        return setOffers(Arrays.asList(offers));
    }

    public OfferRepositoryMock setOffers(List<Offer> offers) {
        this.offersMap.clear();
        offers.forEach(offer -> {
            Offer copy = new Offer(offer);
            copy.markLoadedContent();
            offersMap.put(copy.getId(), copy);
            recordStaffLogin(offer);
        });
        return this;
    }

    @Override
    public List<Offer> findOffers(OffersFilter offersFilter, boolean forUpdate) {
        Stream<Offer> stream = offersMap.values().stream().filter(offersFilter::matches);
        if (offersFilter.hasOrders()) {
            Comparator<Offer> offerComparator = (o1, o2) -> 0;
            List<OffersFilter.Order> orders = offersFilter.getOrders();
            for (OffersFilter.Order order : orders) {
                Comparator<Offer> fieldComparator = order.getField()::compare;
                if (order.getOrderType() == OffersFilter.OrderType.DESC) {
                    fieldComparator = fieldComparator.reversed();
                }
                offerComparator = offerComparator.thenComparing(fieldComparator);
            }
            stream = stream.sorted(offerComparator);
        }

        if (offersFilter.getOffset() != null) {
            stream = stream.skip(offersFilter.getOffset());
        }
        if (offersFilter.getLimit() != null) {
            stream = stream.limit(offersFilter.getLimit());
        }

        stream = stream.map(Offer::new);

        return stream
            .peek(offer -> {
                if (!offersFilter.isFetchOfferContent()) {
                    offer.storeOfferContent(null);
                    offer.setIsOfferContentPresent(false);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public void findShortOfferInfo(OffersFilter offersFilter, Consumer<OfferShortInfo> consumer) {
        findOffers(offersFilter)
            .stream()
            .map(o -> {
                OfferShortInfo osi = new OfferShortInfo();
                osi.setOfferId(o.getId());
                osi.setTitle(o.getTitle());
                return osi;
            })
            .forEach(consumer);
    }

    @Override
    public void findOffers(NamedParameterJdbcTemplate jdbcTemplate, OffersFilter offersFilter,
                           Consumer<Offer> consumer) {
        findOffers(offersFilter).forEach(consumer);
    }

    @Override
    public List<OfferForService> findOffersForService(OffersForServiceFilter offersForServiceFilter,
                                                      boolean forUpdate) {
        List<Offer> offers = findOffers(offersForServiceFilter.getOffersFilter(), forUpdate);
        List<OfferForService> offersForService = BusinessSupplierHelper.getAllOffersForService(offers);

        if (!offersForServiceFilter.getServiceOfferQueryExtractors().isEmpty()) {
            throw new UnsupportedOperationException("Filter service offer query extractors not implemented");
        }
        return offersForService.stream()
            .filter(offersForServiceFilter::checkOfferForService)
            .map(it -> new OfferForService(it.getBaseOffer(), it.getServiceOffer()))
            .collect(Collectors.toList());
    }

    @Override
    public void findAntiMappingWithServiceOffersByStamp(
        long fromStamp, int count, Consumer<AntiMappingWithServiceOffers> consumer
    ) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<Long> findOfferIds(OffersFilter offersFilter) {
        return findOffers(offersFilter, false).stream().map(Offer::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> findOfferIdsForService(OffersForServiceFilter offersForServiceFilter) {
        return findOffersForService(offersForServiceFilter, false).stream()
            .map(o -> o.getBaseOffer().getId()).collect(Collectors.toList());
    }

    @Override
    public List<Long> findOfferCategoryIds(OffersFilter offersFilter) {
        return findOffers(offersFilter, false)
            .stream()
            .map(Offer::getCategoryId)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findOfferSupplierIds(OffersFilter offersFilter) {
        return findOffers(offersFilter, false)
            .stream()
            .map(Offer::getBusinessId)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<ShopSkuKey> findServiceOfferSkuKeysForCriteria(List<Long> baseOfferIds,
                                                               List<ServiceOfferQueryExtractor> criteriaQueries) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getCount(OffersFilter offersFilter) {
        return findOffers(offersFilter).size();
    }

    @Override
    public long getCountOffersForService(OffersForServiceFilter offersForServiceFilter) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void findOffers(OffersFilter offersFilter, Consumer<Offer> consumer) {
        findOffers(offersFilter).forEach(consumer);
    }

    @Override
    public void findOffersLite(OffersFilter offersFilter, Consumer<OfferLite> consumer) {
        findOffers(offersFilter)
            .stream()
            .map(OfferLiteMapper.OFFER_LITE_MAPPER::mapOfferToOfferLite)
            .forEach(consumer);
    }

    @Override
    public void findOffersStoppable(OffersFilter offersFilter, Predicate<Offer> stoppableConsumer) {
        List<Offer> offers = findOffers(offersFilter);
        for (Offer offer : offers) {
            boolean processNext = stoppableConsumer.test(offer);
            if (!processNext) {
                break;
            }
        }
    }

    @Override
    public List<CategoryStat> countCategoryStat(OffersFilter offersFilter) {
        return countCategoryStatInternal(offersFilter, false);
    }

    @Override
    public List<CategoryStat> countCategoryStatForOfferForService(OffersFilter offersFilter) {
        return countCategoryStatInternal(offersFilter, true);
    }

    private List<CategoryStat> countCategoryStatInternal(OffersFilter offersFilter, boolean countServiceOffers) {
        class MyCatStat {
            private final long categoryId;
            private int offerCount = 0;
            private int offerMappedOnPskuCount = 0;

            private MyCatStat(long categoryId) {
                this.categoryId = categoryId;
            }

            public CategoryStat toCategoryStat() {
                return new CategoryStat(categoryId, offerCount, offerMappedOnPskuCount);
            }
        }
        Map<Long, MyCatStat> catStats = new HashMap<>();
        findOffers(offersFilter)
            .forEach(offer -> {
                Long categoryId = offer.getCategoryId();
                catStats.putIfAbsent(categoryId, new MyCatStat(categoryId));
                catStats.get(categoryId).offerCount += countServiceOffers ? offer.getServiceOffers().size() : 1;
                if (offer.hasApprovedSkuMapping() &&
                    offer.getApprovedSkuMappingConfidence() == Offer.MappingConfidence.PARTNER_SELF) {
                    catStats.get(categoryId).offerMappedOnPskuCount
                        += countServiceOffers ? offer.getServiceOffers().size() : 1;
                }
            });
        return catStats.values().stream()
            .map(MyCatStat::toCategoryStat)
            .sorted(Comparator.comparingDouble(CategoryStat::getCategoryId))
            .collect(Collectors.toList());
    }

    @Override
    public List<SupplierStat> countSupplierStat(OffersFilter offersFilter, boolean onlyCount) {
        Map<Integer, SupplierStat> stat = new HashMap<>();
        findOffers(offersFilter).forEach(offer -> stat.compute(offer.getBusinessId(), (k, v) ->
            new SupplierStat(k,
                v == null ? 1 : v.getOfferCount() + 1,
                onlyCount ? null
                    : Stream.of(v == null ? null : v.getMaxOfferStatusModifiedTs(),
                        offer.getProcessingStatusModified(),
                        offer.getAcceptanceStatusModified()
                    ).filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null))
        ));
        return new ArrayList<>(stat.values());
    }

    @Override
    public List<Long> getOffersCategories() {
        return offersMap.values().stream()
            .map(Offer::getCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public void updateTrackerTicket(String trackerTicket,
                                    Offer.AdditionalTicketType ticketType,
                                    Integer processingTicketId,
                                    List<Long> offerIds) {
        List<Offer> updatedOffers = offerIds.stream()
            .map(id -> checkNotNull(offersMap.get(id), "Can't find offer %s", id))
            .map(Offer::copy)
            .peek(offer -> {
                offer.setTrackerTicket(trackerTicket)
                    .setProcessingTicketId(processingTicketId);
                if (ticketType != null) {
                    offer.addAdditionalTicket(ticketType, trackerTicket);
                }
            })
            .collect(Collectors.toList());

        updateOffers(updatedOffers);
    }

    @Override
    public int insertOffers(Collection<Offer> offers) {
        notifyObservers(Collections.emptyMap(), offers, RepositoryObserver::onBeforeSave);
        offers.forEach(offer -> {
            if (offer.isNew()) {
                offer.setId(idGenerator.incrementAndGet());
            }

            Offer copy = new Offer(offer);
            copy.setLastVersion(lastVersionGenerator.incrementAndGet());
            copy.setUpdated(DateTimeUtils.dateTimeNow());
            copy.markLoadedContent();
            var offerContent = copy.getOfferContent();
            OfferContent.OfferContentBuilder builder;
            if (offerContent != null) {
                builder = OfferContent.copyToBuilder(offerContent);
            } else {
                builder = OfferContent.builder();
            }
            OfferContent build = builder
                .id(offer.getId())
                .build();
            copy.storeOfferContent(build);
            offersMap.put(copy.getId(), copy);
            recordStaffLogin(offer);
        });
        notifyObservers(Collections.emptyMap(), offers, RepositoryObserver::onAfterSave);
        return offers.size();
    }

    @Override
    public int updateOffers(Collection<Offer> offers) {
        Map<Long, Offer> beforeOffers = offers.stream()
            .map(offer -> offersMap.get(offer.getId()))
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        notifyObservers(beforeOffers, offers, RepositoryObserver::onBeforeSave);

        offers.forEach(offer -> {
            if (!offersMap.containsKey(offer.getId())
                || offersMap.get(offer.getId()).getLastVersion() != offer.getLastVersion()) {
                throw new ConcurrentModificationException("Failed to update offer #" + offer.getId()
                    + ": either id is absent or lastVersion changed (" + offer.getLastVersion() + ").");
            }
        });

        offers.forEach(offer -> {
            Offer copy = new Offer(offer);
            copy.setLastVersion(lastVersionGenerator.incrementAndGet());
            copy.setUpdated(DateTimeUtils.dateTimeNow());
            if (!offer.hasLoadedContent()) {
                copy.markLoadedContent();
                try {
                    copy.storeOfferContent(copy.getOfferContentBuilder().id(copy.getId()).build());
                } catch (RuntimeException e) {
                    //set default content
                    copy.storeOfferContent(OfferContent.builder().id(copy.getId()).build());
                }
            } else {
                copy.storeOfferContent(copy.getOfferContentBuilder().id(copy.getId()).build());
            }
            offersMap.put(copy.getId(), copy);
            recordStaffLogin(offer);
        });

        notifyObservers(beforeOffers, offers, RepositoryObserver::onAfterSave);

        return offers.size();
    }

    @Override
    public int removeOffers(Collection<Offer> offers) {
        Map<Long, Offer> beforeOffers = offers.stream()
            .map(offer -> offersMap.get(offer.getId()))
            .collect(Collectors.toMap(Offer::getId, Function.identity()));

        List<EntityChangeEvent<Offer>> offerChanges = offers.stream()
            .map(o -> beforeOffers.get(o.getId()))
            .map(o -> new EntityChangeEvent<>(o, null))
            .collect(Collectors.toList());

        notifyObservers(offerChanges, RepositoryObserver::onBeforeSave);

        offers.forEach(offer -> {
            if (!offersMap.containsKey(offer.getId())
                || offersMap.get(offer.getId()).getLastVersion() != offer.getLastVersion()) {
                throw new ConcurrentModificationException("Failed to update offer #" + offer.getId()
                    + ": either id is absent or lastVersion changed (" + offer.getLastVersion() + ").");
            }
        });

        offers.forEach(offer -> {
            offersMap.remove(offer.getId());
        });

        notifyObservers(offerChanges, RepositoryObserver::onAfterSave);

        return offers.size();
    }

    public void loadOffers(String resourcePath) {
        List<Offer> offers = YamlTestUtil.readOffersFromResources(resourcePath);
        setOffers(offers);
    }

    public Optional<String> getLastAuditStaffLogin(long offerId) {
        return lastAuditStaffLogin.getOrDefault(offerId, Optional.empty());
    }

    private void recordStaffLogin(Offer offer) {
        lastAuditStaffLogin.put(offer.getId(), OfferAuditRecorder.getStaffLoginForOffer(
            new OfferRepositoryImpl.OfferAuditInformation(offer)));
    }

    @Override
    public void populateUploadToYtStamps(Collection<Offer> offers) {
        if (offers.size() == 0) {
            return;
        }

        List<Long> stamps = Stream.iterate(stampGenerator.incrementAndGet(), n -> stampGenerator.incrementAndGet())
            .limit(offers.size()).collect(Collectors.toList());

        Iterator<Offer> offerIt = offers.iterator();
        Iterator<Long> stampIt = stamps.iterator();

        while (offerIt.hasNext() && stampIt.hasNext()) {
            Offer offer = offerIt.next();
            Long stamp = stampIt.next();
            offer.setUploadToYtStamp(stamp);
        }
    }

    @Override
    public Map<Long, Integer> getSupplierIds(OffersFilter filter) {
        return findOffers(filter).stream()
            .collect(Collectors.toMap(Offer::getId, Offer::getBusinessId, (f, s) -> s));
    }

    @Override
    public Set<BusinessSkuKey> findBusinessSkuKeys(OffersForServiceFilter filter) {
        return findOffersForService(filter, false).stream()
            .map(o -> o.getBaseOffer().getBusinessSkuKey())
            .collect(Collectors.toSet());
    }

    @Override
    public List<String> findOfferVendors(OffersFilter filter) {
        return findOffers(filter).stream()
            .map(Offer::getVendor)
            .distinct()
            .sorted(Comparator.comparing(String::toLowerCase))
            .collect(Collectors.toList());
    }

    @Override
    public OfferIdRange findOfferIdRange() {
        return new OfferIdRange(
            offersMap.keySet().stream().min(Long::compareTo).orElse(0L),
            offersMap.keySet().stream().max(Long::compareTo).orElse(0L)
        );
    }

    @Override
    public Map<Long, Integer> countServicePartsForOfferIds(Collection<Long> offerIds) {
        Map<Long, Integer> counts = new HashMap<>();

        findOffers(new OffersFilter().setOfferIds(offerIds)).forEach(
            offer -> counts.put(offer.getId(), offer.getServiceOffers().size())
        );

        return counts;
    }

    @Override
    public void deleteAllInTest() {
        offersMap = new HashMap<>();
    }

    @Override
    public void deleteInTest(int businessId, String shopSku) {
        offersMap.values().removeIf(v -> v.getBusinessSkuKey().equals(new BusinessSkuKey(businessId, shopSku)));
    }

    private void notifyObservers(
        Map<Long, Offer> beforeOffers, Collection<Offer> afterOffers,
        BiConsumer<RepositoryObserver<Offer>, List<EntityChangeEvent<Offer>>> handler
    ) {
        List<EntityChangeEvent<Offer>> events = afterOffers.stream()
            .map(offer -> new EntityChangeEvent<>(beforeOffers.get(offer.getId()), offer))
            .collect(Collectors.toList());
        notifyObservers(events, handler);
    }

    public void notifyObservers(
        @Nonnull List<EntityChangeEvent<Offer>> changeEvents,
        BiConsumer<RepositoryObserver<Offer>, List<EntityChangeEvent<Offer>>> handler
    ) {
        List<EntityChangeEvent<Offer>> events = Collections.unmodifiableList(changeEvents);
        observers.forEach(o -> handler.accept(o, events));
    }

    public void addObserver(RepositoryObserver<Offer> observer) {
        observers.add(observer);
    }

    public void removeObserver(RepositoryObserver<Offer> observer) {
        observers.remove(observer);
    }

    @Mapper
    interface OfferLiteMapper {
        OfferLiteMapper OFFER_LITE_MAPPER = Mappers.getMapper(OfferLiteMapper.class);

        OfferLite mapOfferToOfferLite(Offer offer);
    }
}
