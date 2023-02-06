package ru.yandex.market.checkout.util.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.OfferSeller;
import ru.yandex.market.common.report.model.OfferService;
import ru.yandex.market.common.report.model.OrderCancelPolicy;
import ru.yandex.market.common.report.model.PayByYaPlus;
import ru.yandex.market.common.report.model.PromoDetails;
import ru.yandex.market.common.report.model.QuantityLimits;
import ru.yandex.market.common.report.model.UnitInfo;
import ru.yandex.market.common.report.model.json.common.Picture;
import ru.yandex.market.common.report.model.json.common.Region;
import ru.yandex.market.common.report.model.json.credit.InstallmentsInfo;
import ru.yandex.market.common.report.model.json.credit.YandexBnplInfo;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;
import ru.yandex.market.common.report.model.specs.Specs;

public class FoundOfferBuilder {

    private Long feedId;
    private Long priorityRegionId;
    private Long shopId;
    private String name;
    private List<Region> manufacturerCountries;
    private String supplierWorkSchedule;
    private String supplierDescription;
    private String categoryFullName;
    private Integer categoryId;
    private Long vendorId;
    private String offerId;
    private String wareMd5;
    private Currency currency = Currency.RUR;
    private Boolean onStock = true;
    private BigDecimal price;
    private BigDecimal priceWithoutVat;
    private BigDecimal oldDiscountOldMin;
    private BigDecimal oldMin;
    private Integer warehouseId;
    private String shopSku;
    private String marketSku;
    private String cpa;
    private String cartShowUid;
    private Long supplierId;
    private SupplierType supplierType;
    private String promoKey;
    private String promoType;
    private PromoDetails promoDetails;
    private List<OfferPromo> promos = new ArrayList<>();
    private List<String> deliveryPartnerTypes = new ArrayList<>();
    private BigDecimal weight;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal depth;
    private Set<Integer> cargoTypes;
    private String englishName;
    private boolean preorder;
    private Long fulfilmentWarehouseId;
    private Long externalFeedId;
    private Boolean atSupplierWarehouse;
    private Boolean yandexEda;
    private Boolean express;
    private Long modelId;
    private Currency supplierCurrency;
    private Long hsCode;
    private String showInfo;
    private String feedGroupIdHash;
    private String pictureUrl;
    private Boolean isFulfillment;
    private Color color;
    private List<Picture> pictures;
    private Integer pp;
    private boolean loyaltyProgramPartner;
    private boolean digital;
    private boolean bnplAvailable;
    private Set<InstallmentsInfo> installmentsInfos = new HashSet<>();
    private Boolean largeSize;
    private Specs specs;
    private List<OfferService> services;
    private PayByYaPlus payByYaPlus;
    private String unitInfo;
    private Integer quantityLimitStep;
    private OrderCancelPolicy orderCancelPolicy;
    private OfferSeller offerSeller;
    private boolean parallelImport;
    private String parallelImportWarrantyAction;
    private ResaleSpecs resaleSpecs;

    public FoundOfferBuilder() {
        shopId(OrderProvider.SHOP_ID);
        isFulfillment(true);
        color(Color.BLUE);
    }

    @Nonnull
    public static FoundOfferBuilder create() {
        return new FoundOfferBuilder();
    }

    @Nonnull
    public static FoundOfferBuilder createFrom(@Nonnull FoundOffer offer) {
        return Objects.requireNonNull(create()
                .name(offer.getName())
                .feedId(offer.getFeedId())
                .offerId(offer.getShopOfferId())
                .priorityRegionId(offer.getPriorityRegionId())
                .wareMd5(offer.getWareMd5())
                .price(offer.getPrice())
                .weight(offer.getWeight())
                .width(offer.getWidth())
                .height(offer.getHeight())
                .depth(offer.getDepth())
                .currency(offer.getPriceCurrency())
                .supplierId(offer.getSupplierId())
                .supplierType(offer.getSupplierType() == null ? null :
                        SupplierType.getTypeById(Integer.parseInt(offer.getSupplierType())))
                .marketSku(offer.getSku())
                .shopSku(offer.getShopSku())
                .warehouseId(offer.getWarehouseId())
                .feedGroupIdHash(offer.getFeedGroupIdHash())
                .showInfo(offer.getFeeShow()))
                .hsCode(offer.getHsCode())
                .supplierCurrency(offer.getShopCurrency())
                .modelId(offer.getHyperId())
                .atSupplierWarehouse(offer.isAtSupplierWarehouse())
                .externalFeedId(offer.getExternalFeedId())
                .fulfilmentWarehouseId(offer.getFulfillmentWarehouseId())
                .preorder(offer.isPreorder())
                .englishName(offer.getEnglishName())
                .cargoTypes(offer.getCargoTypes())
                .vendorId(offer.getVendorId())
                .categoryId(offer.getHyperCategoryId())
                .categoryFullName(offer.getHyperCategoryFullName())
                .supplierDescription(offer.getSupplierDescription())
                .supplierWorkSchedule(offer.getSupplierWorkSchedule())
                .manufacturerCountries(offer.getManufacturerCountries())
                .pp(offer.getPp())
                .isLoyaltyProgramPartner(offer.isLoyaltyProgramPartner())
                .isFulfillment(true)
                .isDigital(offer.isDownloadable())
                .promoKey(offer.getPromoMd5())
                .promoType(offer.getPromoType())
                .promoDetails(offer.getPromoDetails())
                .promos(offer.getPromos())
                .largeSize(offer.isLargeSize())
                .specs(offer.getSpecs())
                .services(offer.getServices())
                .quantityLimitStep(Optional.ofNullable(offer.getQuantityLimits())
                        .orElse(new QuantityLimits()).getStep())
                .unitInfo(offer.getUnitInfo())
                .orderCancelPolicy(offer.getOrderCancelPolicy())
                .offerSeller(offer.getOfferSeller())
                .parallelImport(offer.isParallelImport())
                .parallelImportWarrantyAction(offer.getParallelImportWarrantyAction())
                .resaleSpecs(offer.getResaleSpecs());
    }

    @Nonnull
    public static FoundOfferBuilder createFrom(@Nonnull OrderItem item) {
        return create()
                .name(item.getOfferName())
                .feedId(item.getFeedId())
                .offerId(item.getOfferId())
                .wareMd5(item.getWareMd5())
                .price(item.getBuyerPrice())
                .priceWithoutVat(item.getPrices().getPriceWithoutVat())
                .weight(item.getWeight())
                .width(item.getWidth())
                .height(item.getHeight())
                .depth(item.getDepth())
                .currency(item.getSupplierCurrency())
                .supplierId(item.getSupplierId())
                .supplierType(item.getSupplierType())
                .marketSku(item.getMsku() == null ? null : String.valueOf(item.getMsku()))
                .shopSku(item.getShopSku())
                .warehouseId(item.getWarehouseId())
                .feedGroupIdHash(item.getFeedGroupIdHash())
                .showInfo(item.getShowInfo())
                .hsCode(item.getHsCode())
                .supplierCurrency(item.getSupplierCurrency())
                .modelId(item.getModelId())
                .atSupplierWarehouse(item.getAtSupplierWarehouse())
                .externalFeedId(item.getExternalFeedId())
                .fulfilmentWarehouseId(item.getFulfilmentWarehouseId())
                .preorder(item.isPreorder())
                .englishName(item.getEnglishName())
                .cargoTypes(item.getCargoTypes())
                .vendorId(item.getVendorId())
                .categoryId(item.getCategoryId())
                .categoryFullName(item.getCategoryFullName())
                .supplierDescription(item.getSupplierDescription())
                .supplierWorkSchedule(item.getSupplierWorkSchedule())
                .manufacturerCountries(item.getManufacturerCountries())
                .pp(item.getPp())
                .isLoyaltyProgramPartner(item.isLoyaltyProgramPartner())
                .isFulfillment(true)
                .isDigital(item.isDigital())
                .quantityLimitStep(item.getCountStep())
                .specs(item.getMedicalSpecsInternal())
                .services(item.getServices() == null ? null :
                        item.getServices().stream()
                                .map(it -> OfferServiceBuilder.createFrom(it).build())
                                .collect(Collectors.toList()))
                .unitInfo(Optional.ofNullable(item.getUnitInfo())
                        .map(it -> {
                            UnitInfo unitInfo = new UnitInfo();
                            unitInfo.setMainUnit(it);
                            return unitInfo;
                        }).orElse(null))
                .orderCancelPolicy(Optional.ofNullable(item.getCancelPolicyData())
                        .map(it -> {
                            OrderCancelPolicy orderCancelPolicy = new OrderCancelPolicy();

                            orderCancelPolicy.setType(it.getType());
                            orderCancelPolicy.setReason(it.getReason());
                            orderCancelPolicy.setDaysForCancel(it.getDaysForCancel());

                            return orderCancelPolicy;
                        }).orElse(null))
                .offerSeller(OfferSellerBuilder.createFrom(item).build())
                .parallelImport(item.isParallelImport())
                .parallelImportWarrantyAction(Optional.ofNullable(item.getParallelImportWarrantyAction())
                        .map(Enum::name)
                        .orElse(null))
                .resaleSpecs(item.getResaleSpecs());
    }

    public FoundOfferBuilder feedId(Long feedId) {
        this.feedId = feedId;
        return this;
    }

    public FoundOfferBuilder priorityRegionId(Long priorityRegionId) {
        this.priorityRegionId = priorityRegionId;
        return this;
    }

    public FoundOfferBuilder shopId(Long shopId) {
        this.shopId = shopId;
        return this;
    }

    public FoundOfferBuilder offerId(String offerId) {
        this.offerId = offerId;
        return this;
    }

    public FoundOfferBuilder wareMd5(String wareMd5) {
        this.wareMd5 = wareMd5;
        return this;
    }

    public FoundOfferBuilder weight(Number weight) {
        if (weight != null) {
            this.weight = BigDecimal.valueOf(weight.longValue());
        }
        return this;
    }

    public FoundOfferBuilder width(Number width) {
        if (width != null) {
            this.width = BigDecimal.valueOf(width.longValue());
        }
        return this;
    }

    public FoundOfferBuilder height(Number height) {
        if (height != null) {
            this.height = BigDecimal.valueOf(height.longValue());
        }
        return this;
    }

    public FoundOfferBuilder depth(Number depth) {
        if (depth != null) {
            this.depth = BigDecimal.valueOf(depth.longValue());
        }
        return this;
    }

    public FoundOfferBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public FoundOfferBuilder isOnStock(Boolean onStock) {
        this.onStock = onStock;
        return this;
    }

    public FoundOfferBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public FoundOfferBuilder priceWithoutVat(BigDecimal priceWithoutVat) {
        this.priceWithoutVat = priceWithoutVat;
        return this;
    }

    public FoundOfferBuilder oldDiscountOldMin(BigDecimal oldDiscountOldMin) {
        this.oldDiscountOldMin = oldDiscountOldMin;
        return this;
    }

    public FoundOfferBuilder oldMin(BigDecimal oldMin) {
        this.oldMin = oldMin;
        return this;
    }

    public FoundOfferBuilder warehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public FoundOfferBuilder shopSku(String shopSku) {
        this.shopSku = shopSku;
        return this;
    }

    public FoundOfferBuilder marketSku(String marketSku) {
        this.marketSku = marketSku;
        return this;
    }

    public FoundOfferBuilder cpa(String cpa) {
        this.cpa = cpa;
        return this;
    }

    public FoundOfferBuilder cartShowUid(String cartShowUid) {
        this.cartShowUid = cartShowUid;
        return this;
    }

    public FoundOfferBuilder supplierId(Long supplierId) {
        this.supplierId = supplierId;
        return this;
    }

    public FoundOfferBuilder supplierType(SupplierType supplierType) {
        this.supplierType = supplierType;
        return this;
    }

    public FoundOfferBuilder promoKey(String promoKey) {
        this.promoKey = promoKey;
        return this;
    }

    public FoundOfferBuilder promoType(String promoType) {
        this.promoType = promoType;
        return this;
    }

    public FoundOfferBuilder promoDetails(PromoDetails promoDetails) {
        this.promoDetails = promoDetails;
        return this;
    }

    public FoundOfferBuilder promos(@Nonnull List<OfferPromo> promos) {
        this.promos = promos;
        return this;
    }

    public FoundOfferBuilder promo(OfferPromo promo) {
        this.promos.add(promo);
        return this;
    }

    public FoundOfferBuilder deliveryPartnerType(String deliveryPartnerType) {
        this.deliveryPartnerTypes.add(deliveryPartnerType);
        return this;
    }

    public FoundOfferBuilder name(String offerName) {
        this.name = offerName;
        return this;
    }

    public FoundOfferBuilder color(Color color) {
        this.color = color;
        return this;
    }

    public FoundOfferBuilder configure(Consumer<FoundOfferBuilder> configurer) {
        configurer.accept(this);
        return this;
    }

    public FoundOfferBuilder manufacturerCountries(List<Region> manufacturerCountries) {
        this.manufacturerCountries = manufacturerCountries;
        return this;
    }

    public FoundOfferBuilder supplierWorkSchedule(String supplierWorkSchedule) {
        this.supplierWorkSchedule = supplierWorkSchedule;
        return this;
    }

    public FoundOfferBuilder supplierDescription(String supplierDescription) {
        this.supplierDescription = supplierDescription;
        return this;
    }

    public FoundOfferBuilder categoryFullName(String categoryFullName) {
        this.categoryFullName = categoryFullName;
        return this;
    }

    public FoundOfferBuilder categoryId(Integer categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public FoundOfferBuilder vendorId(Long vendorId) {
        this.vendorId = vendorId;
        return this;
    }

    public FoundOfferBuilder cargoTypes(Set<Integer> cargoTypes) {
        this.cargoTypes = cargoTypes;
        return this;
    }

    public FoundOfferBuilder englishName(String englishName) {
        this.englishName = englishName;
        return this;
    }

    public FoundOfferBuilder preorder(boolean preorder) {
        this.preorder = preorder;
        return this;
    }

    public FoundOfferBuilder fulfilmentWarehouseId(Long fulfilmentWarehouseId) {
        this.fulfilmentWarehouseId = fulfilmentWarehouseId;
        return this;
    }

    public FoundOfferBuilder externalFeedId(Long externalFeedId) {
        this.externalFeedId = externalFeedId;
        return this;
    }

    public FoundOfferBuilder atSupplierWarehouse(Boolean atSupplierWarehouse) {
        this.atSupplierWarehouse = atSupplierWarehouse;
        return this;
    }

    public FoundOfferBuilder yandexEda(Boolean yandexEda) {
        this.yandexEda = yandexEda;
        return this;
    }

    public FoundOfferBuilder express(Boolean express) {
        this.express = express;
        return this;
    }

    public FoundOfferBuilder modelId(Long modelId) {
        this.modelId = modelId;
        return this;
    }

    public FoundOfferBuilder supplierCurrency(Currency supplierCurrency) {
        this.supplierCurrency = supplierCurrency;
        return this;
    }

    public FoundOfferBuilder hsCode(Long hsCode) {
        this.hsCode = hsCode;
        return this;
    }

    public FoundOfferBuilder showInfo(String showInfo) {
        this.showInfo = showInfo;
        return this;
    }

    public FoundOfferBuilder feedGroupIdHash(String feedGroupIdHash) {
        this.feedGroupIdHash = feedGroupIdHash;
        return this;
    }

    public FoundOfferBuilder isFulfillment(Boolean isFulfillment) {
        this.isFulfillment = isFulfillment;
        return this;
    }

    public FoundOfferBuilder pictures(List<Picture> pictures) {
        this.pictures = pictures;
        return this;
    }

    public FoundOfferBuilder picture(
            String picture,
            int color
    ) {
        if (pictures == null) {
            pictures = new ArrayList<>();
        }
        Picture pic = new Picture();
        pic.setEntity(picture);
        pic.setColor(color);
        pic.setWareMd5(DigestUtils.md5Hex(offerId));
        return this;
    }

    public FoundOfferBuilder pictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
        return this;
    }

    public FoundOfferBuilder pp(Integer pp) {
        this.pp = pp;
        return this;
    }

    public FoundOfferBuilder isLoyaltyProgramPartner(boolean loyaltyProgramPartner) {
        this.loyaltyProgramPartner = loyaltyProgramPartner;
        return this;
    }

    public FoundOfferBuilder isDigital(boolean value) {
        this.digital = value;
        return this;
    }

    public FoundOfferBuilder largeSize(Boolean value) {
        this.largeSize = value;
        return this;
    }

    public FoundOfferBuilder specs(Specs specs) {
        this.specs = specs;
        return this;
    }

    public FoundOfferBuilder services(List<OfferService> services) {
        this.services = services;
        return this;
    }

    public FoundOfferBuilder payByYaPlus(PayByYaPlus payByYaPlus) {
        this.payByYaPlus = payByYaPlus;
        return this;
    }

    public FoundOfferBuilder bnpl(boolean bnplAvailable, InstallmentsInfo... installmentsInfos) {
        this.bnplAvailable = bnplAvailable;
        this.installmentsInfos = Set.of(installmentsInfos);
        return this;
    }

    public FoundOfferBuilder unitInfo(UnitInfo unitInfo) {
        if (unitInfo != null) {
            this.unitInfo = unitInfo.getMainUnit();
        }

        return this;
    }

    public FoundOfferBuilder orderCancelPolicy(OrderCancelPolicy orderCancelPolicy) {
        this.orderCancelPolicy = orderCancelPolicy;

        return this;
    }

    public FoundOfferBuilder quantityLimitStep(Integer quantityLimitStep) {
        this.quantityLimitStep = quantityLimitStep;
        return this;
    }

    public FoundOfferBuilder offerSeller(OfferSeller offerSeller) {
        this.offerSeller = offerSeller;
        return this;
    }

    public FoundOfferBuilder parallelImport(boolean parallelImport) {
        this.parallelImport = parallelImport;
        return this;
    }

    public FoundOfferBuilder parallelImportWarrantyAction(String parallelImportWarrantyAction) {
        this.parallelImportWarrantyAction = parallelImportWarrantyAction;
        return this;
    }

    public FoundOfferBuilder resaleSpecs(ResaleSpecs resaleSpecs) {
        this.resaleSpecs = resaleSpecs;
        return this;
    }

    public FoundOffer build() {
        FoundOffer offer = new FoundOffer();
        offer.setName(name);
        offer.setFeedId(feedId);
        offer.setShopOfferId(offerId);
        offer.setPriorityRegionId(priorityRegionId);
        offer.setWareMd5(wareMd5);
        offer.setOnStock(onStock);
        offer.setPrice(price);
        offer.setPriceWithoutVat(priceWithoutVat);
        offer.setOldMin(oldMin);
        offer.setOldDiscountOldMin(oldDiscountOldMin);
        offer.setWarehouseId(warehouseId);
        offer.setShopSku(shopSku);
        offer.setSku(marketSku);
        offer.setVendorId(vendorId);
        offer.setSupplierId(supplierId);
        offer.setSupplierWorkSchedule(supplierWorkSchedule);
        offer.setSupplierDescription(supplierDescription);
        offer.setManufacturerCountries(manufacturerCountries);
        offer.setHyperId(modelId);
        offer.setHyperCategoryId(categoryId);
        offer.setHyperCategoryFullName(categoryFullName);
        offer.setCargoTypes(cargoTypes);
        offer.setEnglishName(englishName);
        offer.setPreorder(preorder);
        offer.setFulfillmentWarehouseId(fulfilmentWarehouseId);
        offer.setExternalFeedId(externalFeedId);
        if (yandexEda != null) {
            offer.setIsEda(yandexEda);
        }
        if (express != null) {
            offer.setIsExpress(express);
        }
        if (atSupplierWarehouse != null) {
            offer.setAtSupplierWarehouse(atSupplierWarehouse);
        }

        offer.setShopCurrency(supplierCurrency);
        offer.setHsCode(hsCode);
        offer.setFeeShow(showInfo);
        offer.setFeedGroupIdHash(feedGroupIdHash);
        offer.setRgb(color);

        if (supplierType != null) {
            switch (supplierType) {
                case FIRST_PARTY:
                    offer.setSupplierType("1");
                    break;

                case THIRD_PARTY:
                    offer.setSupplierType("3");
                    break;
                default:
                    break;
            }
        }
        offer.setCpa(cpa);
        offer.setCartShowUid(cartShowUid);
        offer.setPromoMd5(promoKey);
        offer.setPromoType(promoType);
        offer.setPromoDetails(promoDetails);
        offer.setPromos(promos);
        offer.setShopId(shopId);
        offer.setWeight(weight);
        offer.setWidth(width);
        offer.setHeight(height);
        offer.setDepth(depth);

        offer.setShopCurrency(currency);
        offer.setPriceCurrency(currency);
        offer.setBaseGeneration(UUID.randomUUID().toString());
        offer.setBid(ThreadLocalRandom.current().nextInt(1, 10000));
        offer.setFulfillment(isFulfillment);
        offer.setUrl(UUID.randomUUID().toString());
        if (StringUtils.isNotEmpty(pictureUrl)) {
            offer.setPictures(Collections.singleton(new OfferPicture(pictureUrl)));
        }
        offer.setPicturesRaw(generateJSONArray());
        offer.setFiltersRaw(generateJSONArray());
        offer.setWarningsRaw(generateJSONObject());
        offer.setDeliveryPartnerTypes(deliveryPartnerTypes);
        offer.setPp(pp);
        offer.setLoyaltyProgramPartner(loyaltyProgramPartner);
        offer.setDownloadable(digital);
        offer.setLargeSize(largeSize);
        offer.setSpecs(specs);
        offer.setServices(services);
        offer.setPayByYaPlus(payByYaPlus);
        if (quantityLimitStep != null) {
            offer.setQuantityLimits(new QuantityLimits(1, quantityLimitStep));
        }
        if (bnplAvailable) {
            YandexBnplInfo bnplInfo = new YandexBnplInfo();
            bnplInfo.setEnabled(bnplAvailable);
            offer.setYandexBnplInfo(bnplInfo);
        }

        if (CollectionUtils.isNotEmpty(installmentsInfos)) {
            offer.setInstallmentsInfoSet(installmentsInfos);
        }

        if (unitInfo != null) {
            UnitInfo reportUnitInfo = new UnitInfo();
            reportUnitInfo.setMainUnit(unitInfo);
            offer.setUnitInfo(reportUnitInfo);
        }
        if (orderCancelPolicy != null) {
            offer.setOrderCancelPolicy(orderCancelPolicy);
        }
        offer.setOfferSeller(offerSeller);
        offer.setParallelImport(parallelImport);
        offer.setParallelImportWarrantyAction(parallelImportWarrantyAction);
        offer.setResaleSpecs(resaleSpecs);
        return offer;
    }

    private JSONArray generateJSONArray() {
        int count = ThreadLocalRandom.current().nextInt(2, 5);
        List<JSONObject> objects = new ArrayList<>();
        while (count-- > 0) {
            objects.add(generateJSONObject());
        }
        return new JSONArray(objects);
    }

    private JSONObject generateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("someProperty", UUID.randomUUID());
        jsonObject.put("anotherProperty", "i some value, dont push me");
        return jsonObject;
    }
}
