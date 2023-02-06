package ru.yandex.market.marketpromo.core.test.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampOfferPromos.Promo;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampPartnerInfo;
import NMarketIndexer.Common.Common;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.util.OfferDataConverter;
import ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames;
import ru.yandex.market.marketpromo.model.Category;
import ru.yandex.market.marketpromo.model.CategoryManager;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferPropertiesCore;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.OfferPromoBase;
import ru.yandex.market.marketpromo.model.DatacampOfferPromo;

import static java.util.Objects.requireNonNullElse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Category.F_CATEGORY_ID;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Category.F_FULLNAME;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Category.F_LEAF;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Category.F_NAME;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Category.F_PARENT_ID;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Category.F_PUBLISHED;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Promo.F_DESCRIPTION_BINARY;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.Promo.F_ID;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.User.F_FIRST_NAME;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.User.F_LAST_NAME;
import static ru.yandex.market.marketpromo.core.data.source.yt.YtFieldNames.User.F_STAFF_LOGIN;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.YT_ACTIVE;

@Component
@Profile("!" + YT_ACTIVE)
public class YtTestHelper {

    @Autowired
    private YtTables ytTables;

    @Autowired
    private Cypress cypress;

    @Autowired
    private OfferDataConverter offerDataConverter;

    public void mockOffersResponse(@Nonnull List<DatacampOffer> offers) {
        configureYTreeMapModeMockAnswer(offers.stream()
                .map(offer -> YTree.mapBuilder()
                        .key(YtFieldNames.Offer.F_BUSINESS_ID).value(offer.getBusinessId())
                        .key(YtFieldNames.Offer.F_WAREHOUSE_ID).value(offer.getWarehouseId())
                        .key(YtFieldNames.Offer.F_OFFER_ID).value(offer.getShopSku())
                        .key(YtFieldNames.Offer.F_OFFER).value(DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setBusinessId((int) offer.getBusinessId())
                                        .setShopId((int) offer.getShopId())
                                        .setWarehouseId((int) offer.getWarehouseId())
                                        .setFeedId((int) offer.getFeedId())
                                        .setOfferId(offer.getShopSku())
                                        .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                                .setShopSku(offer.getShopSku())
                                                .setMarketSkuId(requireNonNullElse(offer.getMarketSku(), -1L))
                                                .build())
                                        .build())
                                .setStatus(offer.getDisabled() ?
                                        DataCampOfferStatus.OfferStatus.newBuilder()
                                                .addDisabled(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build()) :
                                        DataCampOfferStatus.OfferStatus.newBuilder()
                                )
                                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                                        .setCreationTs(offer.getCreatedAt().toEpochSecond(ZoneOffset.UTC))
                                        .setModificationTs(offer.getUpdatedAt().toEpochSecond(ZoneOffset.UTC))
                                        .build())
                                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                                .setProductName(Objects.requireNonNull(offer.getName()))
                                                .setCategoryId(Objects.requireNonNull(offer.getCategoryId()).intValue())
                                                .build())
                                        .build())
                                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                                .setBinaryPrice(
                                                        offerDataConverter.convertToPriceExpression(offer.getPrice())
                                                )
                                                .setBinaryOldprice(
                                                        offer.getBasePrice() != null ?
                                                                offerDataConverter.convertToPriceExpression(
                                                                        offer.getBasePrice()) :
                                                                Common.PriceExpression.newBuilder().build()
                                                )
                                                .build())
                                        .build())
                                .setPromos(DataCampOfferPromos.OfferPromos.newBuilder()
                                        .setAnaplanPromos(DataCampOfferPromos.MarketPromos.newBuilder()
                                                .setAllPromos(DataCampOfferPromos.Promos.newBuilder()
                                                        .addAllPromos(offer.getPotentialPromos().values().stream()
                                                                .map(this::offerPromoBaseToPromo)
                                                                .collect(Collectors.toUnmodifiableList()))
                                                        .build())
                                                .setActivePromos(DataCampOfferPromos.Promos.newBuilder()
                                                        .addAllPromos(offer.getActivePromos().values().stream()
                                                                .map(this::offerPromoMechanicToPromo)
                                                                .collect(Collectors.toUnmodifiableList()))
                                                        .build())
                                                .build())
                                        .build())
                                .setPartnerInfo(DataCampPartnerInfo.PartnerInfo.newBuilder()
                                        .setSupplierType(offer.getSupplierType().getReportCode())
                                        .build())
                                .build().toByteArray())
                        .buildMap())
                .collect(Collectors.toUnmodifiableList()));
    }

    @Nonnull
    private Promo offerPromoBaseToPromo(@Nonnull OfferPromoBase promoBase) {
        Promo.Builder b = Promo.newBuilder()
                .setId(promoBase.getId());

        if (promoBase.getBasePrice() != null) {
            b.setDirectDiscount(Promo.DirectDiscount.newBuilder()
                    .setBasePrice(offerDataConverter.convertToPriceExpression(promoBase.getBasePrice()))
                    .build());
        }

        return b.build();
    }

    @Nonnull
    private Promo offerPromoMechanicToPromo(@Nonnull DatacampOfferPromo promoMechanic) {
        Promo.Builder b = Promo.newBuilder()
                .setId(promoMechanic.getPromoBase().getId());

        switch (promoMechanic.getType()) {
            case CHEAPEST_AS_GIFT:
                b.setCheapestAsGift(Promo.CheapestAsGift.newBuilder()
                        .build());
                break;
            case DIRECT_DISCOUNT:
                DirectDiscountOfferPropertiesCore offerProperties =
                        (DirectDiscountOfferPropertiesCore) promoMechanic.getMechanicsProperties();
                Promo.DirectDiscount.Builder pb = Promo.DirectDiscount.newBuilder();

                if (offerProperties.getFixedPrice() != null) {
                    pb.setPrice(offerDataConverter.convertToPriceExpression(offerProperties.getFixedPrice()));
                }
                if (offerProperties.getFixedBasePrice() != null) {
                    pb.setBasePrice(offerDataConverter.convertToPriceExpression(offerProperties.getFixedBasePrice()));
                }

                b.setDirectDiscount(pb.build());
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return b.build();
    }

    public void mockPromoDescriptionResponse(@Nonnull Map<String, byte[]> promoIdToProto) {
        List<YTreeMapNode> mockedNodes = new ArrayList<>();
        for (String promoId : promoIdToProto.keySet()) {
            mockedNodes.add(YTree.mapBuilder()
                    .key(F_ID).value(promoId)
                    .key(F_DESCRIPTION_BINARY).value(promoIdToProto.get(promoId))
                    .buildMap());
        }
        configureYTreeMapModeMockAnswer(mockedNodes);
    }

    public void mockCatmanCategoriesResponse(@Nonnull List<CategoryManager> categoryManagers) {
        List<YTreeMapNode> mockedNodes = new ArrayList<>();
        for (CategoryManager cm : categoryManagers) {
            for (Long categoryId : cm.getCategoryIds()) {
                mockedNodes.add(YTree.mapBuilder()
                        .key(F_STAFF_LOGIN).value(cm.getStaffLogin())
                        .key(F_FIRST_NAME).value(cm.getFirstName())
                        .key(F_LAST_NAME).value(cm.getLastName())
                        .key(F_CATEGORY_ID).value(categoryId)
                        .buildMap());
            }
        }
        configureYTreeMapModeMockAnswer(mockedNodes);
    }

    public void mockCategoryTreeResponse(@Nonnull List<Category> categories) {
        List<YTreeMapNode> mockedNodes = new ArrayList<>();
        for (Category category : categories) {
            YTreeMapNode rowAnswerMock = Mockito.mock(YTreeMapNode.class);
            when(rowAnswerMock.getLong(eq(F_CATEGORY_ID))).thenReturn(category.getCategoryId());
            when(rowAnswerMock.getLong(eq(F_PARENT_ID))).thenReturn(category.getParentId());
            when(rowAnswerMock.getBool(eq(F_LEAF))).thenReturn(category.isLeaf());
            when(rowAnswerMock.getBool(eq(F_PUBLISHED))).thenReturn(category.isPublished());
            when(rowAnswerMock.getString(eq(F_NAME))).thenReturn(category.getName());
            when(rowAnswerMock.getString(eq(F_FULLNAME))).thenReturn(category.getFullName());
            mockedNodes.add(rowAnswerMock);
        }
        configureYTreeMapModeMockAnswer(mockedNodes);
    }

    public void mockCategoryTreeResponse() {
        mockCategoryTreeResponse(CategoriesTestHelper.defaultCategoryList());
    }

    private void configureYTreeMapModeMockAnswer(@Nonnull List<YTreeMapNode> mockedNodes) {
        mockCypress();
        doAnswer(ans -> {
            var consumer = (Consumer<YTreeMapNode>) ans.getArguments()[2];
            mockedNodes.forEach(consumer);
            return null;
        }).when(ytTables).read(any(), any(), any(Consumer.class));
    }

    private void mockCypress() {
        doAnswer(ans -> {
            YTreeNode node = mock(YTreeNode.class);
            when(node.stringValue())
                    .thenReturn(LocalDateTime.now()
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            return node;
        }).when(cypress).get(any(YPath.class));
    }

}
