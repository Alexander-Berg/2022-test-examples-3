package ru.yandex.market.mboc.common.offers.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.mbo.jooq.repo.notification.EntityChangeEvent;
import ru.yandex.market.mbo.jooq.repo.notification.RepositoryObserver;
import ru.yandex.market.mbo.lightmapper.test.LongGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;

public class AntiMappingRepositoryMock
    extends LongGenericMapperRepositoryMock<AntiMapping>
    implements AntiMappingRepository {

    private final AtomicLong uploadStampGenerator = new AtomicLong();

    private final List<RepositoryObserver<AntiMapping>> observers = new ArrayList<>();

    public AntiMappingRepositoryMock() {
        super(AntiMapping::setId, AntiMapping::getId);
    }

    @Override
    public List<AntiMapping> insertBatch(Collection<AntiMapping> antiMappings) {
        notifyObservers(Collections.emptyMap(), antiMappings, RepositoryObserver::onBeforeSave);

        List<AntiMapping> inserted = super.insertBatch(antiMappings);

        notifyObservers(Collections.emptyMap(), inserted, RepositoryObserver::onAfterSave);
        return inserted;
    }

    @Override
    public List<AntiMapping> updateBatch(Collection<AntiMapping> antiMappings, int batchSize) {
        Map<Long, AntiMapping> antiMappingsBefore = findByIdsForUpdate(antiMappings.stream()
            .filter(Predicate.not(AntiMapping::isNew))
            .map(AntiMapping::getId)
            .collect(Collectors.toSet())).stream()
            .collect(Collectors.toMap(AntiMapping::getId, Function.identity()));
        notifyObservers(antiMappingsBefore, antiMappings, RepositoryObserver::onBeforeSave);

        List<AntiMapping> antiMappingsAfter = super.updateBatch(antiMappings, batchSize);

        notifyObservers(antiMappingsBefore, antiMappingsAfter, RepositoryObserver::onAfterSave);

        return antiMappingsAfter;
    }

    @Override
    public void populateUploadStamps(Collection<AntiMapping> antiMappings) {
        if (antiMappings.size() == 0) {
            return;
        }

        List<Long> stamps = Stream
            .iterate(uploadStampGenerator.incrementAndGet(), n -> uploadStampGenerator.incrementAndGet())
            .limit(antiMappings.size())
            .collect(Collectors.toList());

        Iterator<AntiMapping> toPopulateIt = antiMappings.iterator();
        Iterator<Long> stampIt = stamps.iterator();

        while (toPopulateIt.hasNext() && stampIt.hasNext()) {
            AntiMapping antiMapping = toPopulateIt.next();
            Long stamp = stampIt.next();
            antiMapping.setUploadStamp(stamp);
        }
    }

    @Override
    public List<AntiMapping> findByFilter(Filter filter) {
        return findAll().stream().filter(filter::matches).collect(Collectors.toList());
    }

    @Override
    public List<AntiMapping> findByFilterForUpdate(Filter filter) {
        return findByFilter(filter);
    }

    @Override
    public void findByFilterIter(Filter filter, Consumer<Iterator<AntiMapping>> consumer) {
        consumer.accept(findAll().stream().filter(filter::matches).iterator());
    }

    @Override
    public void deleteByOfferIds(List<Long> offerIds) {
        Set<Long> offerIdsSet = new HashSet<>(offerIds);
        List<Long> ids = findAll().stream()
            .filter(am -> offerIdsSet.contains(am.getOfferId()))
            .map(AntiMapping::getId)
            .distinct()
            .collect(Collectors.toList());
        delete(ids);
    }

    private void notifyObservers(
        Map<Long, AntiMapping> befores, Collection<AntiMapping> afters,
        BiConsumer<RepositoryObserver<AntiMapping>, List<EntityChangeEvent<AntiMapping>>> handler
    ) {
        List<EntityChangeEvent<AntiMapping>> events = afters.stream()
            .map(after -> new EntityChangeEvent<>(befores.get(after.getId()), after))
            .collect(Collectors.toList());
        observers.forEach(o -> handler.accept(o, events));
    }

    @Override
    public void addObserver(RepositoryObserver<AntiMapping> observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(RepositoryObserver<AntiMapping> observer) {
        observers.remove(observer);
    }
}
