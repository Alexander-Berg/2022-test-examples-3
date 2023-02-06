package ru.yandex.market.mboc.common.masterdata.services.cutoff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.CutoffState;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff.Key;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffFilter;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class OfferCutoffServiceMock implements OfferCutoffService {

    private Set<OfferCutoff> savedCutoffs = new LinkedHashSet<>();

    public List<OfferCutoff> all() {
        return new ArrayList<>(savedCutoffs);
    }

    @Override
    public List<OfferCutoff> findCutoffs(OfferCutoffFilter filter) {
        return null;
    }

    @Override
    public List<OfferCutoff> findNotUploadedToMbi() {
        return savedCutoffs.stream()
            .filter(c -> c.getStateUploadTs() == null || c.getStateUploadTs().isBefore(c.getStateChangeTs()))
            .collect(Collectors.toList());
    }

    @Override
    public void updateUploadedToMbi(Collection<OfferCutoff> cutoffs) {
        Set<Key> updations = cutoffs.stream().map(OfferCutoff::getKey).collect(Collectors.toSet());
        savedCutoffs.stream()
            .filter(c -> updations.contains(c.getKey()))
            .forEach(c -> c.setStateUploadTs(DateTimeUtils.dateTimeNow()));
    }

    @Override
    public Optional<OfferCutoff> openCutoff(OfferCutoff offerCutoff) {
        OfferCutoff copy = new OfferCutoff().copyFrom(offerCutoff)
            .setState(CutoffState.OPEN)
            .setStateChangeTs(DateTimeUtils.dateTimeNow());
        if (savedCutoffs.add(copy)) {
            return Optional.of(copy);
        }
        return Optional.empty();
    }

    @Override
    public List<OfferCutoff> openCutoffs(Collection<OfferCutoff> cutoffs) {
        List<OfferCutoff> copyList = cutoffs.stream()
            .map(c -> {
                OfferCutoff copy = new OfferCutoff().copyFrom(c);
                copy.setState(CutoffState.OPEN);
                copy.setStateChangeTs(DateTimeUtils.dateTimeNow());
                return copy;
            })
            .collect(Collectors.toList());
        List<OfferCutoff> res = List.copyOf(CollectionUtils.subtract(copyList, savedCutoffs));
        savedCutoffs.addAll(copyList);
        return res;
    }

    @Override
    public Optional<OfferCutoff> closeCutoff(OfferCutoff offerCutoff) {
        OfferCutoff existing = savedCutoffs.stream()
            .filter(co -> co.getKey().equals(offerCutoff.getKey()) && co.getState() == CutoffState.OPEN)
            .findFirst()
            .orElse(null);

        if (existing == null) {
            return Optional.empty();
        }

        return Optional.of(existing.setState(CutoffState.CLOSED));
    }

    @Override
    public Optional<OfferCutoff> closeCutoff(Key cutoffKey) {
        return closeCutoff(new OfferCutoff().setKey(cutoffKey));
    }

    @Override
    public int count(OfferCutoffFilter filter) {
        return 0;
    }

    @Override
    public int countOpenByType(String typeId, List<Integer> suppliersToIgnore, boolean needToUpload) {
        return 0;
    }
}
