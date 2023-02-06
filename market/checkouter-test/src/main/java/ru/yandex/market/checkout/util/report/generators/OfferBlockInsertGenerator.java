package ru.yandex.market.checkout.util.report.generators;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.OfferSeller;
import ru.yandex.market.common.report.model.OrderCancelPolicy;
import ru.yandex.market.common.report.model.PayByYaPlus;
import ru.yandex.market.common.report.model.PromoBound;
import ru.yandex.market.common.report.model.RawParam;
import ru.yandex.market.common.report.model.UnitInfo;
import ru.yandex.market.common.report.model.json.common.Region;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;
import ru.yandex.market.common.report.parser.json.OfferInfoMarketReportJsonParserSettings;

import static java.math.BigDecimal.ZERO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;


/**
 * Патчит json. Вставляет ифнормацию об офферах
 *
 * @author Nikolai Iusiumbeli
 * date: 10/07/2017
 */
public class OfferBlockInsertGenerator extends AbstractCheckouterJsonGenerator<ReportGeneratorParameters> {

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        if (parameters.getOrder() == null && CollectionUtils.isEmpty(parameters.getOffers())) {
            return object;
        }

        Map<FeedOfferId, FoundOffer> offerMap = defaultIfNull(
                parameters.getOffers(),
                Collections.<FoundOffer>emptyList()
        ).stream()
                .collect(Collectors.toMap(FoundOffer::getFeedOfferId, Function.identity(), (e1, e2) -> e1));

        Set<FeedOfferId> keys = new HashSet<>(parameters.getOrder() == null ?
                offerMap.keySet() :
                Sets.union(parameters.getOrder().getItemKeys(), offerMap.keySet()));

        for (FeedOfferId itemKey : keys) {
            if (parameters.overrideItemInfo(itemKey).hideOffer()) {
                continue;
            }

            final FoundOffer offer;
            if (parameters.getOrder() == null) {
                offer = offerMap.get(itemKey);
            } else {
                OrderItem item = parameters.getOrder().firstItemFor(itemKey);
                offer = offerMap.computeIfAbsent(itemKey, key ->
                        FoundOfferBuilder.createFrom(item).build());

                offer.setShopId(parameters.getResponseShopId());
            }

            ItemInfo itemInfo = parameters.overrideItemInfo(itemKey);

            applyOverrides(offer, itemInfo, parameters);

            addJsonPropertyValue(object, "search.results",
                    generateOfferInfo(
                            offer, itemInfo, parameters
                    )
            );
        }

        return object;
    }

    private void applyOverrides(FoundOffer offer, ItemInfo overrideItemInfo, ReportGeneratorParameters parameters) {
        offer.setWareMd5(defaultIfNull(overrideItemInfo.getWareMd5(), offer.getWareMd5()));
        offer.setFeedGroupIdHash(defaultIfNull(overrideItemInfo.getFeedGroupIdHash(), offer.getFeedGroupIdHash()));
        offer.setFeeShow(defaultIfNull(overrideItemInfo.getShowInfo(), offer.getFeeShow()));
        offer.setAtSupplierWarehouse(defaultIfNull(overrideItemInfo.getAtSupplierWarehouse(),
                offer.isAtSupplierWarehouse()));
        offer.setExternalFeedId(defaultIfNull(overrideItemInfo.getExternalFeedId(), offer.getExternalFeedId()));
        offer.setFulfillmentWarehouseId(defaultIfNull(overrideItemInfo.getFulfillmentWarehouseId(),
                offer.getFulfillmentWarehouseId()));
        offer.setPromoType(defaultIfNull(overrideItemInfo.promoType,
                offer.getPromoType()));
        offer.setPromoMd5(defaultIfNull(overrideItemInfo.promoKey,
                offer.getPromoMd5()));

        if (!overrideItemInfo.isHideWeight()) {
            offer.setWeight(defaultIfNull(overrideItemInfo.getWeight(),
                    offer.getWeight()));
        }

        if (!overrideItemInfo.isHideDimensions()) {
            if (overrideItemInfo.getDimensions() != null) {
                offer.setWidth(new BigDecimal(overrideItemInfo.getDimensions().get(0)));
                offer.setHeight(new BigDecimal(overrideItemInfo.getDimensions().get(1)));
                offer.setDepth(new BigDecimal(overrideItemInfo.getDimensions().get(2)));
            } else {
                offer.setWidth(defaultIfNull(offer.getWidth(), BigDecimal.valueOf(10)));
                offer.setHeight(defaultIfNull(offer.getWidth(), BigDecimal.valueOf(21)));
                offer.setDepth(defaultIfNull(offer.getWidth(), BigDecimal.valueOf(20)));
            }
        }

        ItemInfo.Fulfilment fulfilment = overrideItemInfo.getFulfilment();

        if (parameters.getOrder() != null
                && parameters.getOrder().getAcceptMethod() == OrderAcceptMethod.PUSH_API) {
            //FBS, DBS или C&C
            offer.setFulfillment(false);
            offer.setSupplierType(Integer.toString(SupplierType.THIRD_PARTY.getId()));
        } else {
            offer.setFulfillment(Optional.ofNullable(fulfilment)
                    .map(ItemInfo.Fulfilment::getFulfilment)
                    .orElse(offer.isFulfillment() && offer.getShopSku() != null && offer.getSku() != null &&
                            offer.getSupplierId() != null));
            if (overrideItemInfo.getSupplierType() != null) {
                offer.setSupplierType(Integer.toString(overrideItemInfo.getSupplierType().getId()));
            }
        }

        offer.setRealShopId(fulfilment != null && fulfilment.supplierId != null ? fulfilment.supplierId :
                offer.getSupplierId());
        offer.setSupplierId(fulfilment != null && fulfilment.supplierId != null ? fulfilment.supplierId :
                offer.getSupplierId());
        offer.setSku(fulfilment != null && fulfilment.sku != null ? fulfilment.sku : offer.getSku());
        offer.setShopSku(fulfilment != null && fulfilment.shopSku != null ? fulfilment.shopSku : offer.getShopSku());
        offer.setWarehouseId(Optional.ofNullable(fulfilment)
                .map(ItemInfo.Fulfilment::getWarehouseId).orElse(offer.getWarehouseId()));

        offer.setRawParams(overrideItemInfo.getRawParams());

        ItemInfo.Prices prices = overrideItemInfo.getPrices();
        BigDecimal buyerPrice = offer.getPrice();
        Currency buyerCurrency = parameters.getBuyerCurrency();
        offer.setPrice(prices.value != null ? prices.value : buyerPrice);
        offer.setShopPrice(firstNonNull(prices.rawValue, prices.value, buyerPrice));
        offer.setFeedPrice(prices.feedPrice);
        offer.setPriceCurrency(prices.currency != null ? prices.currency : buyerCurrency);
        if (prices.discountOldMin != null) {
            offer.setOldMin(prices.discountOldMin);
        }
        if (prices.oldDiscountOldMin != null) {
            offer.setOldDiscountOldMin(prices.oldDiscountOldMin);
        }

        offer.setSupplierDescription(defaultIfNull(overrideItemInfo.getSupplierDescription(),
                offer.getSupplierDescription()));

        offer.setSupplierWorkSchedule(defaultIfNull(overrideItemInfo.getSupplierWorkSchedule(),
                offer.getSupplierWorkSchedule()));

        offer.setManufacturerCountries(defaultIfNull(overrideItemInfo.getManufacturerCountries(),
                offer.getManufacturerCountries()));

        if (overrideItemInfo.getPayByYaPlus() != null) {
            offer.setPayByYaPlus(overrideItemInfo.getPayByYaPlus());
        }
    }

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/offerBlockStub.json";
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private JsonObject generateOfferInfo(
            FoundOffer givenOffer,
            ItemInfo overrideItemInfo,
            ReportGeneratorParameters parameters
    ) {
        JsonObject offer = loadJson();
        var parserSettings = new OfferInfoMarketReportJsonParserSettings();
        setJsonPropertyValue(offer, parserSettings.getName(), givenOffer.getName());
        setJsonPropertyValue(offer, parserSettings.getShopId(), givenOffer.getShopId());
        setJsonPropertyValue(offer, parserSettings.getFeedId(), givenOffer.getFeedId());
        setJsonPropertyValue(offer, parserSettings.getOfferId(), givenOffer.getShopOfferId());
        setJsonPropertyValue(offer, parserSettings.getShopSku(), givenOffer.getShopSku());
        setJsonPropertyValue(offer, parserSettings.getWareMd5(), givenOffer.getWareMd5());
        setJsonPropertyValue(offer, parserSettings.getWarehouseId(), givenOffer.getWarehouseId());
        setJsonPropertyValue(offer, parserSettings.getSku(), givenOffer.getSku());
        setJsonPropertyValue(offer, parserSettings.getRgb(),
                Optional.ofNullable(givenOffer.getRgb()).map(Color::getValue).orElse(null));

        setJsonPropertyValue(offer, parserSettings.getPromoType(), givenOffer.getPromoType());
        setJsonPropertyValue(offer, parserSettings.getPromoKey(), givenOffer.getPromoMd5());
        setJsonPropertyValue(offer, parserSettings.getPromoStock(), givenOffer.getPromoStock());
        if (givenOffer.getPromoDetails() != null) {
            setJsonPropertyValue(offer, parserSettings.getPromoAnaplanId(),
                    givenOffer.getPromoDetails().getAnaplanId());
            setJsonPropertyValue(offer, parserSettings.getShopPromoId(),
                    givenOffer.getPromoDetails().getShopPromoId());
            setJsonPropertyValue(offer, parserSettings.getPromoDiscountValue(),
                    givenOffer.getPromoDetails().getDiscount());
            setJsonPropertyValue(offer, parserSettings.getPromoPriceValue(),
                    givenOffer.getPromoDetails().getPromoFixedPrice());
            setJsonPropertyValue(offer, parserSettings.getPromoSubsidyValue(),
                    givenOffer.getPromoDetails().getPromoFixedSubsidy());
            setJsonPropertyValue(offer, parserSettings.getHasDcoSubsidy(),
                    givenOffer.getPromoDetails().isHasDcoSubsidy());
            setJsonPropertyValue(offer, parserSettings.getPromoItemsInfoAllowBerubonus(),
                    givenOffer.getPromoDetails().isAllowBeruBonus());
            setJsonPropertyValue(offer, parserSettings.getPromoItemsInfoAllowPromocode(),
                    givenOffer.getPromoDetails().isAllowPromocode());
            setJsonPropertyValue(offer, parserSettings.getPromoStartDate(),
                    stringOf(givenOffer.getPromoDetails().getStartDate(), DateTimeFormatter.ISO_DATE_TIME));
            setJsonPropertyValue(offer, parserSettings.getPromoEndDate(),
                    stringOf(givenOffer.getPromoDetails().getEndDate(), DateTimeFormatter.ISO_DATE_TIME));
        }

        if (CollectionUtils.isNotEmpty(givenOffer.getPromos())) {
            JsonArray promosArray = new JsonArray();
            for (OfferPromo givenOfferPromo : givenOffer.getPromos()) {
                JsonObject promo = new JsonObject();
                setJsonPropertyValue(promo, parserSettings.getPromoKeyForMultiPromo(),
                        givenOfferPromo.getPromoMd5());
                setJsonPropertyValue(promo, parserSettings.getPromoTypeForMultiPromo(),
                        givenOfferPromo.getPromoType());
                setJsonPropertyValue(promo, parserSettings.getPromoStockForMultiPromo(),
                        givenOfferPromo.getPromoStock());
                if (givenOfferPromo.getPromoDetails() != null) {
                    setJsonPropertyValue(promo, parserSettings.getPromoAnaplanIdForMultiPromo(),
                            givenOfferPromo.getPromoDetails().getAnaplanId());
                    setJsonPropertyValue(promo, parserSettings.getShopPromoIdForMultiPromo(),
                            givenOfferPromo.getPromoDetails().getShopPromoId());
                    setJsonPropertyValue(promo, parserSettings.getPromoPriceValueForMultiPromo(),
                            givenOfferPromo.getPromoDetails().getPromoFixedPrice());
                    setJsonPropertyValue(promo, parserSettings.getPromoDiscountValueForMultiPromo(),
                            givenOfferPromo.getPromoDetails().getDiscount());
                    setJsonPropertyValue(promo, parserSettings.getPromoSubsidyValueForMultiPromo(),
                            givenOfferPromo.getPromoDetails().getPromoFixedSubsidy());
                    setJsonPropertyValue(promo, parserSettings.getHasDcoSubsidyForMultiPromo(),
                            givenOfferPromo.getPromoDetails().isHasDcoSubsidy());
                    setJsonPropertyValue(promo, parserSettings.getPromoItemsInfoAllowBerubonusForMultiPromo(),
                            givenOfferPromo.getPromoDetails().isAllowBeruBonus());
                    setJsonPropertyValue(promo, parserSettings.getPromoItemsInfoAllowPromocodeForMultiPromo(),
                            givenOfferPromo.getPromoDetails().isAllowPromocode());
                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoStartDateForMultiPromo(),
                            stringOf(
                                    givenOfferPromo.getPromoDetails().getStartDate(),
                                    DateTimeFormatter.ISO_DATE_TIME
                            )
                    );
                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoEndDateForMultiPromo(),
                            stringOf(
                                    givenOfferPromo.getPromoDetails().getEndDate(),
                                    DateTimeFormatter.ISO_DATE_TIME
                            )
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoPriority(),
                            givenOfferPromo.getPromoDetails().getPriority()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoBucketName(),
                            givenOfferPromo.getPromoDetails().getPromoBucketName()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getCmsDescriptionSemanticId(),
                            givenOfferPromo.getPromoDetails().getCmsDescriptionSemanticId()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoShare(),
                            givenOfferPromo.getPromoDetails().getShare()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoPartnerId(),
                            givenOfferPromo.getPromoDetails().getPartnerId()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoMarketTariffsVersionId(),
                            givenOfferPromo.getPromoDetails().getMarketTariffsVersionId()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoMinOrderTotalThresholds(),
                            givenOfferPromo.getPromoDetails().getMinOrderTotalThresholds()
                    );

                    setJsonPropertyValue(
                            promo,
                            parserSettings.getPromoMaxOfferCashbackThresholds(),
                            givenOfferPromo.getPromoDetails().getMaxOfferCashbackThresholds()
                    );

                    var bounds = givenOfferPromo.getPromoDetails().getBounds();
                    if (CollectionUtils.isNotEmpty(bounds)) {
                        JsonArray promoBoundsArray = new JsonArray();
                        for (PromoBound givenPromoBound : bounds) {
                            JsonObject bound = new JsonObject();
                            setJsonPropertyValue(
                                    bound,
                                    parserSettings.getCountBound(),
                                    givenPromoBound.getCountBound()
                            );
                            setJsonPropertyValue(
                                    bound,
                                    parserSettings.getReceiptBound(),
                                    givenPromoBound.getReceiptBound()
                            );
                            setJsonPropertyValue(
                                    bound,
                                    parserSettings.getCountPercentDiscount(),
                                    givenPromoBound.getCountPercentDiscount()
                            );
                            setJsonPropertyValue(
                                    bound,
                                    parserSettings.getCountAbsoluteDiscount(),
                                    givenPromoBound.getCountAbsoluteDiscount()
                            );
                            setJsonPropertyValue(
                                    bound,
                                    parserSettings.getReceiptPercentDiscount(),
                                    givenPromoBound.getReceiptPercentDiscount()
                            );
                            setJsonPropertyValue(
                                    bound,
                                    parserSettings.getReceiptAbsoluteDiscount(),
                                    givenPromoBound.getReceiptAbsoluteDiscount()
                            );
                            promoBoundsArray.add(bound);
                        }

                        setJsonPropertyValue(promo, parserSettings.getPromoBounds(), promoBoundsArray);
                    }
                }
                promosArray.add(promo);
            }
            setJsonPropertyValue(offer, parserSettings.getPromos(), promosArray);
        }

        setJsonPropertyValue(offer, parserSettings.getPreorder(), givenOffer.isPreorder());
        setJsonPropertyValue(offer, parserSettings.getFulfillment(), givenOffer.isFulfillment());
        setJsonPropertyValue(offer, parserSettings.getVendorId(), givenOffer.getVendorId());
        setJsonPropertyValue(offer, parserSettings.getSupplierId(), givenOffer.getSupplierId());
        setJsonPropertyValue(offer, parserSettings.getSupplierType(), givenOffer.getSupplierType());
        setJsonPropertyValue(offer, parserSettings.getPrice(), givenOffer.getPrice());
        setJsonPropertyValue(offer, parserSettings.getPriceWithoutVat(), givenOffer.getPriceWithoutVat());
        setJsonPropertyValue(offer, parserSettings.getSellerPrice(), givenOffer.getShopPrice());
        setJsonPropertyValue(offer, parserSettings.getFeedPrice(), givenOffer.getFeedPrice());
        setJsonPropertyValue(offer, parserSettings.getPriceCurrency(), givenOffer.getPriceCurrency());
        setJsonPropertyValue(offer, parserSettings.getOldMin(), givenOffer.getOldMin());
        setJsonPropertyValue(offer, parserSettings.getOldDiscountOldMin(), givenOffer.getOldDiscountOldMin());
        setJsonPropertyValue(offer, parserSettings.getFeedGroupIdHash(), givenOffer.getFeedGroupIdHash());
        setJsonPropertyValue(offer, parserSettings.getFeeShow(), givenOffer.getFeeShow());
        setJsonPropertyValue(offer, parserSettings.getHsCode(), givenOffer.getHsCode());
        setJsonPropertyValue(offer, parserSettings.getWeight(), givenOffer.getWeight());
        setJsonPropertyValue(offer, parserSettings.getSellerCurrency(),
                defaultIfNull(givenOffer.getShopCurrency(), parameters.getShopCurrency()));
        setJsonPropertyValue(offer, parserSettings.getHyperId(), givenOffer.getHyperId());
        setJsonPropertyValue(offer, parserSettings.getAtSupplierWarehouse(), givenOffer.isAtSupplierWarehouse());
        setJsonPropertyValue(offer, parserSettings.getExternalFeedId(), givenOffer.getExternalFeedId());
        setJsonPropertyValue(offer, parserSettings.getFulfillmentWarehouseId(), givenOffer.getFulfillmentWarehouseId());
        setJsonPropertyValue(offer, parserSettings.getDimensions(), Stream.of(
                        defaultIfNull(givenOffer.getWidth(), ZERO),
                        defaultIfNull(givenOffer.getHeight(), ZERO),
                        defaultIfNull(givenOffer.getDepth(), ZERO)
                )
                .map(String::valueOf)
                .collect(Collectors.toList()));


        setJsonPropertyValue(offer, parserSettings.getSubsidies(), parameters.isShopSupportsSubsidies());
        setJsonPropertyValue(offer, parserSettings.getReturnDeliveryAddress(),
                parameters.getShopReturnDeliveryAddress());
        fillPayByYaPlus(offer, parserSettings, givenOffer);

        if (parameters.getDeliveryVat() != null) {
            setJsonPropertyValue(offer, parserSettings.getDeliveryVat(), parameters.getDeliveryVat());
        } else {
            removePropertyValue(offer, parserSettings.getDeliveryVat());
        }
        setJsonPropertyValue(offer, parserSettings.getReturnPolicy(), parameters.getReturnPolicy());
        setJsonPropertyValue(offer, "delivery.isPriorityRegion", parameters.isPriorityRegion());
        setJsonPropertyValue(offer, parserSettings.getIsDownloadable(), givenOffer.isDownloadable());
        setJsonPropertyValue(offer, parserSettings.getShopPriorityRegionId(), parameters.getShopPriorityRegionId());
        setJsonPropertyValue(offer, parserSettings.getSellerToUserExchangeRate(),
                parameters.getShopToUserConvertRate());
        setJsonPropertyValue(offer, parserSettings.getSellerComment(), parameters.getSellerComment());

        if (parameters.getItemVat() != null) {
            setJsonPropertyValue(offer, parserSettings.getVat(), parameters.getItemVat());
        } else {
            removePropertyValue(offer, parserSettings.getVat());
        }
        if (parameters.getReportFiltersValue() != null) {
            setJsonPropertyValue(offer, parserSettings.getFilters(), parameters.getReportFiltersValue());
        }

        if (CollectionUtils.isNotEmpty(parameters.getDeliveryPartnerTypes())) {
            setJsonPropertyValue(offer, parserSettings.getDeliveryPartnerTypes(),
                    parameters.getDeliveryPartnerTypes().toArray());
        }

        if (parameters.isEda() != null) {
            setJsonPropertyValue(offer, parserSettings.getIsEda(), parameters.isEda());
        }

        if (parameters.getFoodtechType() != null) {
            setJsonPropertyValue(offer, parserSettings.getFoodtechType(), parameters.getFoodtechType());
        }

        if (parameters.isLargeSize() != null) {
            setJsonPropertyValue(offer, parserSettings.getLargeSize(), parameters.isLargeSize());
        }

        if (parameters.isExpress() != null) {
            setJsonPropertyValue(offer, parserSettings.getIsExpress(), parameters.isExpress());
        }

        if (parameters.isGlobal()) {
            setJsonPropertyValue(offer, "shop.isGlobal", true);
        }

        if (!overrideItemInfo.isHideWeight()) {
            setJsonPropertyValue(offer, parserSettings.getWeight(), givenOffer.getWeight());
        }

        if (!overrideItemInfo.isHideDimensions()) {
            setJsonPropertyValue(offer, parserSettings.getDimensions(), Stream.of(
                            defaultIfNull(givenOffer.getWidth(), ZERO),
                            defaultIfNull(givenOffer.getHeight(), ZERO),
                            defaultIfNull(givenOffer.getDepth(), ZERO)
                    )
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }

        setJsonPropertyValue(offer, parserSettings.getRealShopId(), givenOffer.getRealShopId());
        setJsonPropertyValue(offer, parserSettings.getRealShopId(), givenOffer.getRealShopId());
        setJsonPropertyValue(offer, parserSettings.getSku(), givenOffer.getSku());
        setJsonPropertyValue(offer, parserSettings.getShopSku(), givenOffer.getShopSku());

        if (givenOffer.getWarehouseId() != null) {
            setJsonPropertyValue(offer, parserSettings.getWarehouseId(), givenOffer.getWarehouseId());
        } else {
            removePropertyValue(offer, parserSettings.getWarehouseId());
        }

        if (givenOffer.getSupplierType() != null) {
            setJsonPropertyValue(offer, parserSettings.getSupplierType(), givenOffer.getSupplierType());

        }
        if (givenOffer.getSubscriptionName() != null) {
            setJsonPropertyValue(offer, parserSettings.getSubscriptionName(), givenOffer.getSubscriptionName());
        }

        setJsonPropertyValue(offer, "isPreorder", givenOffer.isPreorder());
        setJsonPropertyValue(offer, "englishName", givenOffer.getEnglishName());
        setJsonPropertyValue(offer, "cargoTypes", givenOffer.getCargoTypes());
        setJsonPropertyValue(offer, "rawParams", generateRawParams(givenOffer.getRawParams()));
        setJsonPropertyValue(offer, "vendor.id", givenOffer.getVendorId());
        setJsonPropertyValue(offer, parserSettings.getCartShowUid(), givenOffer.getCartShowUid());

        JsonObject categoryObject = offer.getAsJsonArray("categories").get(0).getAsJsonObject();
        setJsonPropertyValue(categoryObject, "fullName", givenOffer.getHyperCategoryFullName());
        setJsonPropertyValue(categoryObject, "id", givenOffer.getHyperCategoryId());
        setJsonPropertyValue(offer, parserSettings.getPp(), givenOffer.getPp());

        setJsonPropertyValue(
                offer, "supplierDescription",
                givenOffer.getSupplierDescription()
        );
        setJsonPropertyValue(
                offer, "supplier.workSchedule",
                givenOffer.getSupplierWorkSchedule()
        );
        setJsonPropertyValue(
                offer, "manufacturer.countries",
                mapRegions(givenOffer.getManufacturerCountries())
        );

        if (CollectionUtils.isNotEmpty(parameters.getDeliveryPartnerTypes())) {
            setJsonPropertyValue(offer, "delivery.deliveryPartnerTypes",
                    parameters.getDeliveryPartnerTypes().toArray()
            );
        }

        setJsonPropertyValue(offer, "isYaSubscriptionOffer", parameters.isYaSubscriptionOffer());

        fillPictures(offer, overrideItemInfo);
        fillLocalDeliveryOptionsParameters(offer, givenOffer, parameters);
        fillDeliveryMethods(offer, parameters);
        fillInternalSpecs(offer, givenOffer);
        fillServices(offer, givenOffer);
        fillBnplInfo(offer, givenOffer);
        fillInstallmentsInfo(offer, givenOffer);
        fillResaleSpecs(offer, givenOffer, parserSettings);

        if (givenOffer.getUnitInfo() != null) {
            setJsonPropertyValue(offer, parserSettings.getUnitInfo(),
                    generateUnitInfo(givenOffer.getUnitInfo(), parserSettings));
        }
        if (givenOffer.getQuantityLimits() != null) {
            setJsonPropertyValue(offer, parserSettings.getQuantityLimitStep(),
                    givenOffer.getQuantityLimits().getStep());
        }
        setJsonPropertyValue(offer, parserSettings.getUniqueOffer(), parameters.getUniqueOffer());
        setJsonPropertyValue(offer, parserSettings.getOrderCancelPolicy(),
                generateCancelPolicyData(givenOffer.getOrderCancelPolicy(), parserSettings));
        setJsonPropertyValue(offer, parserSettings.getParallelImport(), givenOffer.isParallelImport());
        setJsonPropertyValue(offer, parserSettings.getParallelImportWarrantyAction(),
                givenOffer.getParallelImportWarrantyAction());
        setJsonPropertyValue(offer,
                parserSettings.getOfferSellerWarrantyPeriod(),
                Optional.ofNullable(givenOffer.getOfferSeller())
                        .map(OfferSeller::getWarrantyPeriod)
                        .orElse(null));
        return offer;
    }

    protected void fillResaleSpecs(JsonObject offer, FoundOffer giveOffer,
                                   OfferInfoMarketReportJsonParserSettings parserSettings) {
        ResaleSpecs resaleSpecs = giveOffer.getResaleSpecs();
        if (resaleSpecs == null) {
            return;
        }

        JsonObject resaleSpecsJson = new JsonObject();
        setJsonPropertyValue(resaleSpecsJson, parserSettings.getResaleReasonValue(), resaleSpecs.getReasonValue());
        setJsonPropertyValue(resaleSpecsJson, parserSettings.getResaleReasonText(), resaleSpecs.getReasonText());
        setJsonPropertyValue(resaleSpecsJson, parserSettings.getResaleConditionValue(),
                resaleSpecs.getConditionValue());
        setJsonPropertyValue(resaleSpecsJson, parserSettings.getResaleConditionText(), resaleSpecs.getConditionText());
        setJsonPropertyValue(offer, parserSettings.getResaleSpecs(), resaleSpecsJson);
    }

    private JsonObject generateCancelPolicyData(OrderCancelPolicy orderCancelPolicy,
                                                OfferInfoMarketReportJsonParserSettings settings) {
        if (orderCancelPolicy == null) {
            return null;
        }

        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, settings.getCancelType(), orderCancelPolicy.getType().getName());
        setJsonPropertyValue(jsonObject, settings.getCancelDaysForCancel(), orderCancelPolicy.getDaysForCancel());
        setJsonPropertyValue(jsonObject, settings.getCancelReason(), orderCancelPolicy.getReason());

        return jsonObject;

    }

    private JsonObject generateUnitInfo(UnitInfo unitInfo,
                                        OfferInfoMarketReportJsonParserSettings settings) {
        if (unitInfo == null) {
            return null;
        }

        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, settings.getMainUnit(), unitInfo.getMainUnit());
        return jsonObject;
    }

    private void fillPayByYaPlus(JsonObject offer,
                                 OfferInfoMarketReportJsonParserSettings parserSettings,
                                 FoundOffer givenOffer) {
        PayByYaPlus givenPayByYaPlus = givenOffer.getPayByYaPlus();
        if (givenPayByYaPlus != null) {
            JsonObject payByYaPlus = new JsonObject();
            setJsonPropertyValue(payByYaPlus, "price", givenPayByYaPlus.getPrice());
            setJsonPropertyValue(offer, parserSettings.getPayByYaPlus(), payByYaPlus);
        }
    }

    private List<JsonObject> mapRegions(List<Region> regions) {
        if (regions == null) {
            return null;
        }
        return regions.stream().map(region -> {
            JsonObject regionJson = new JsonObject();
            regionJson.addProperty("entity", "region");
            region.getId().ifPresent(id -> regionJson.addProperty("id", id));
            region.getName().ifPresent(name -> regionJson.addProperty("name", name));
            region.getLingua().ifPresent(lingua -> {
                JsonObject linguaJson = new JsonObject();
                lingua.getName().ifPresent(name -> {
                    JsonObject linguaNameJson = new JsonObject();
                    name.getAccusative().ifPresent(accusative -> linguaNameJson.addProperty("accusative", accusative));
                    name.getGenitive().ifPresent(genitive -> linguaNameJson.addProperty("genitive", genitive));
                    name.getPreposition().ifPresent(preposition -> linguaNameJson.addProperty(
                            "preposition",
                            preposition
                    ));
                    name.getPrepositional().ifPresent(prepositional -> linguaNameJson.addProperty(
                            "prepositional",
                            prepositional
                    ));
                    setJsonPropertyValue(linguaJson, "name", linguaNameJson);
                });
                setJsonPropertyValue(regionJson, "lingua", linguaJson);
            });
            return regionJson;
        }).collect(Collectors.toList());
    }

    private void fillPictures(JsonObject offer, ItemInfo overrideItemInfo) {
        if (overrideItemInfo.getPicUrl() == null) {
            return;
        }

        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", overrideItemInfo.getPicUrl());

        JsonArray thumbnails = new JsonArray();
        thumbnails.add(thumbnail);

        JsonObject picture = new JsonObject();
        picture.add("thumbnails", thumbnails);

        setJsonPropertyValue(offer, "pictures", Lists.newArrayList(picture));

    }

    private JsonArray generateRawParams(List<RawParam> rawParams) {
        if (rawParams == null) {
            return null;
        }

        JsonArray result = new JsonArray();
        rawParams.stream()
                .map(rp -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("value", rp.getValue());
                    jsonObject.addProperty("name", rp.getName());
                    jsonObject.addProperty("unit", rp.getUnit());
                    return jsonObject;
                }).forEach(result::add);
        return result;
    }

}
