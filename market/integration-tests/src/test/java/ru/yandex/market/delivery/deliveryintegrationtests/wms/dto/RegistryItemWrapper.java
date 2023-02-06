package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.Korobyte;
import ru.yandex.market.logistic.api.model.common.NonconformityType;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.common.UnitCount;
import ru.yandex.market.logistic.api.model.common.UnitCountType;
import ru.yandex.market.logistic.api.model.common.UnitInfo;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.utils.DateTime;

@Getter
public class RegistryItemWrapper {

    private RegistryItem registryItem;
    private boolean checkImei;
    private boolean checkSn;
    private boolean checkCis;
    private boolean hasDuplicates;

    private List<UnitCountType> anomalyTypes = new ArrayList<>();
    private List<NonconformityType> nonconformityTypes = new ArrayList<>();

    public RegistryItemWrapper(RegistryItem registryItem) {
        this.registryItem = registryItem;
    }

    public RegistryItemWrapper(long vendorId, String article, String name, String lot, String barcode,
                               boolean hasLifeTime) {
        this.registryItem = RegistryItem.builder(
                        UnitInfo.builder()
                                .setCompositeId(
                                        CompositeId.builder(List.of(
                                                new PartialId(PartialIdType.ARTICLE, article),
                                                new PartialId(PartialIdType.VENDOR_ID, Long.toString(vendorId))
                                        )).build()
                                )
                                .setCounts(List.of(new UnitCount.UnitCountBuilder()
                                        .setCountType(UnitCountType.FIT)
                                        .setQuantity(1)
                                        .build()
                                ))
                                .setKorobyte(new Korobyte.KorobyteBuiler(
                                        10, 11, 12, new BigDecimal(2))
                                        .setWeightNet(new BigDecimal("0.26"))
                                        .setWeightTare(new BigDecimal("0.1"))
                                        .build()
                                )
                                .build())
                .setName(name)
                .setPrice(new BigDecimal("12.34"))
                .setManufacturedDate(new DateTime("2020-12-01T11:00:00+03:00"))
                .setHasLifeTime(hasLifeTime)
                .setLifeTime(365)
                .setBoxCount(0)
                .setRemainingLifetimes(new RemainingLifetimes(
                        new ShelfLives(new ShelfLife(60), new ShelfLife(70)),
                        new ShelfLives(new ShelfLife(30), new ShelfLife(80))))
                .setUpdated(new DateTime("2021-05-19T13:31:54+03:00"))
                .setBarcodes(List.of(new Barcode.BarcodeBuilder(barcode).setSource(BarcodeSource.UNKNOWN).build()))
                .build();
    }

    public RegistryItemWrapper addAnomalyType(UnitCountType anomalyType) {
        this.getAnomalyTypes().add(anomalyType);
        return this;
    }

    public RegistryItem getRegistryItem() {
        return registryItem;
    }

    public String getArticle() {
        return getRegistryItem().getUnitInfo().getCompositeId().getPartialIds().stream()
                .filter(partialId -> partialId.getIdType().equals(PartialIdType.ARTICLE))
                .findAny().map(PartialId::getValue)
                .orElse(null);
    }

    public String getName() {
        return getRegistryItem().getName();
    }

    public Long getVendorId() {
        return getRegistryItem().getUnitInfo().getCompositeId().getPartialIds().stream()
                .filter(partialId -> partialId.getIdType().equals(PartialIdType.VENDOR_ID))
                .findAny().map(partialId -> Long.valueOf(partialId.getValue())).orElse(null);
    }

    public boolean isShelfLife() {
        return getRegistryItem().getHasLifeTime();
    }

    public int getQuantity() {
        return new Long(getRegistryItem().getUnitInfo().getCounts().stream()
                .map(UnitCount::getQuantity)
                .collect(Collectors.summarizingInt(Integer::intValue)).getSum()).intValue();
    }

    public String getIdentityValue(PartialIdType partialIdType) {
        return registryItem.getUnitInfo().getCounts().get(0).getUnitIds().stream()
                .flatMap(compositeId -> compositeId.getPartialIds().stream())
                .filter(partialId -> partialId.getIdType().equals(partialIdType))
                .findFirst()
                .map(PartialId::getValue)
                .orElse(null);
    }

    public List<String> getIdentityValues(PartialIdType partialIdType) {
        return registryItem.getUnitInfo().getCounts().get(0).getUnitIds().stream()
                .flatMap(compositeId -> compositeId.getPartialIds().stream())
                .filter(partialId -> partialId.getIdType().equals(partialIdType))
                .map(PartialId::getValue)
                .collect(Collectors.toList());
    }

    public String getBarcode() {
        return registryItem.getBarcodes().stream()
                .filter(barcode -> StringUtils.isNotEmpty(barcode.getCode()))
                .findAny()
                .map(Barcode::getCode)
                .orElse(null);
    }

    public UnitCount getCount(UnitCountType countType) {
        return registryItem.getUnitInfo().getCounts().stream()
                .filter(count -> count.getCountType().equals(countType))
                .findAny()
                .orElse(null);
    }
}
