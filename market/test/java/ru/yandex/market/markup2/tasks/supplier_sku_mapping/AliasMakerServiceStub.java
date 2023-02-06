package ru.yandex.market.markup2.tasks.supplier_sku_mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.markup2.utils.aliasmaker.AliasMakerService;
import ru.yandex.market.mboc.http.SupplierOffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.yandex.market.markup2.utils.mboc.MboCategoryServiceMock.SUPPLIER_NAME_FUNCTION;

/**
 * @author galaev
 * @since 2019-06-11
 */
public class AliasMakerServiceStub extends AliasMakerService {
    private Multimap<Integer, AliasMaker.Offer> offersMap = ArrayListMultimap.create();
    private List<SupplierOffer.ContentTaskResult> mappingResults = new ArrayList<>();
    private List<SupplierOffer.MappingModerationTaskResult> moderationResults =
            new ArrayList<>();

    private boolean mockThrowingException;

    public AliasMakerServiceStub() {
        super(null);
    }

    @Override
    public List<AliasMaker.CategorySuppliers> getSupplierOffersCategories() {
        return getCategorySuppliers();
    }

    @Override
    public List<AliasMaker.CategorySuppliers> getSupplierOffersCategoriesForContentLab() {
        return getCategorySuppliers();
    }

    @Override
    public List<AliasMaker.CategorySuppliers> getMappedSupplierOffersCategories() {
        return getCategorySuppliers();
    }

    @Override
    public List<AliasMaker.CategorySuppliers> getSupplierOffersInClassificationCategories() {
        return getCategorySuppliers();
    }

    private List<AliasMaker.CategorySuppliers> getCategorySuppliers() {
        List<AliasMaker.CategorySuppliers> result = new ArrayList<>();
        offersMap.asMap().forEach((catId, offers) -> {
            Map<Long, Integer> bySupplierId = new HashMap<>();
            AliasMaker.CategorySuppliers.Builder builder = AliasMaker.CategorySuppliers.newBuilder()
                .setCategoryId(catId);
            for (AliasMaker.Offer offer : offers) {
                bySupplierId.merge(offer.getShopId(), 1, Integer::sum);
            }
            bySupplierId.forEach((supId, cnt) -> builder.addSupplierOffersCount(
                AliasMaker.SupplierOffersCount.newBuilder()
                    .setSupplierId(supId)
                    .setSupplierType("3P")
                    .setSupplierName(SUPPLIER_NAME_FUNCTION.apply(supId))
                    .setOffersCount(cnt)
            ));
            result.add(builder.build());
        });
        return result;
    }

    public List<AliasMaker.Offer> getSupplierOffers(Integer categoryId,
                                                    Long supplierId,
                                                    Collection<String> currentOffers,
                                                    boolean withSupplierMappings,
                                                    int needCount,
                                                    boolean throughContentLab) {
        return offersMap.get(categoryId).stream()
            .filter(offer -> !currentOffers.contains(offer.getOfferId()))
            .filter(offer -> offer.getShopId() == supplierId)
            .limit(needCount)
            .collect(Collectors.toList());
    }

    public void putOffers(Integer categoryId, AliasMaker.Offer... offers) {
        putOffers(categoryId, Arrays.asList(offers));
    }

    public void putOffers(Integer categoryId, List<AliasMaker.Offer> offers) {
        offersMap.putAll(categoryId, offers);
    }

    public Collection<AliasMaker.Offer> getAllOffers() {
        return offersMap.values();
    }

    public void clearOffers() {
        offersMap.clear();
    }

    @Override
    public List<AliasMaker.Offer> convertAndEnrich(List<SupplierOffer.Offer> offers) {
        return offers.stream().map(o -> AliasMaker.Offer.newBuilder()
                .setOfferId(String.valueOf(o.getInternalOfferId()))
                .setTrackerTicket(o.getProcessingTicket())
                .build()
        ).collect(Collectors.toList());
    }

}
