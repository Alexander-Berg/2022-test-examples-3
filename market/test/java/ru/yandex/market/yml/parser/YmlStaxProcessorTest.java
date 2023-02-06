package ru.yandex.market.yml.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.yml.parser.model.Category;
import ru.yandex.market.yml.parser.model.Condition;
import ru.yandex.market.yml.parser.model.Currency;
import ru.yandex.market.yml.parser.model.DeliveryOption;
import ru.yandex.market.yml.parser.model.DiscountPrice;
import ru.yandex.market.yml.parser.model.Gift;
import ru.yandex.market.yml.parser.model.Offer;
import ru.yandex.market.yml.parser.model.Param;
import ru.yandex.market.yml.parser.model.Product;
import ru.yandex.market.yml.parser.model.Promo;
import ru.yandex.market.yml.parser.model.PromoDiscount;
import ru.yandex.market.yml.parser.model.PromoGift;
import ru.yandex.market.yml.parser.model.Purchase;
import ru.yandex.market.yml.parser.model.Shop;
import ru.yandex.market.yml.parser.model.enums.CurrencyCode;

import static org.junit.jupiter.api.Assertions.assertTrue;


class YmlStaxProcessorTest {
    private static final Logger log = LoggerFactory.getLogger(YmlStaxProcessorTest.class);

    @Test
    public void StandartStaxApiStreamProcessorTest() throws Exception {
        Shop shop;
        // get sample xml from classpath
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("YML_sample.xml");
        try (YmlStaxStreamProcessor processor = YmlProcessorFactory.getStandartStaxStreamProcessor(is)) {
            shop = processor.getShop();
            checkShop(shop);
            YmlStaxStreamProcessor.EntityType entityType;
            while ((entityType = processor.getNextEntityType()) != null) {
                Object[] objects;
                switch (entityType) {
                    case OFFER:
                        objects = processor.getOffersStream()
                                .toArray();
                        checkOffers(objects);
                        break;
                    case GIFT:
                        objects = processor.getGiftsStream()
                                .toArray();
                        checkGifts(objects);
                        break;
                    case PROMO:
                        objects = processor.getPromosStream()
                                .toArray();
                        checkPromos(objects);
                        break;
                    default:
                        Assertions.fail(String.format("Unacceptable value: %s", entityType));
                }
            }
        }
    }

    @Test
    public void AaltotStax2ApiStreamProcessorTest() throws XMLStreamException, IOException, URISyntaxException {
        Shop shop;
        // get sample xml from classpath
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("YML_sample.xml");
        try (YmlStaxStreamProcessor processor
                     = YmlProcessorFactory.getAaltoStreamProcessor(is, YmlProcessorFactory.Stax2Mode.SPEED)) {
            shop = processor.getShop();
            checkShop(shop);
            YmlStaxStreamProcessor.EntityType entityType;
            while ((entityType = processor.getNextEntityType()) != null) {
                Object[] objects = processor.getStream()
                        .toArray();
                switch (entityType) {
                    case OFFER:
                        checkOffers(objects);
                        break;
                    case GIFT:
                        checkGifts(objects);
                        break;
                    case PROMO:
                        checkPromos(objects);
                        break;
                    default:
                        Assertions.fail(String.format("Unacceptable value: %s", entityType));
                }
            }
        }
    }

    private void checkPromos(Object[] promos) {
        //// promos
        Promo promo = (Promo) promos[0];
        Assertions.assertEquals("PromoGift", promo.getId());
        Assertions.assertEquals("gift with purchase", promo.getType());
        Assertions.assertEquals("2020-02-01 09:00:00", promo.getStartDate());
        Assertions.assertEquals("2020-03-01 22:00:00", promo.getEndDate());
        Assertions.assertEquals("Купите бытовую технику марки Brand и получите кружку в подарок.",
                promo.getDescription());
        Assertions.assertEquals("http://best.seller.ru/promos/gift", promo.getUrl());
        PromoDiscount promoDiscount = promo.getDiscount();
        Assertions.assertEquals("currency", promoDiscount.getUnit());
        Assertions.assertEquals(CurrencyCode.RUR, promoDiscount.getCurrency());
        Assertions.assertEquals(300, promoDiscount.getValue());

        //// promo's purchases
        Purchase purchase = promo.getPurchase();
        Assertions.assertEquals(2, purchase.getRequiredQuantity());
        Assertions.assertEquals(1, purchase.getFreeQuantity());
        Product product = purchase.getProducts().get(0);
        Assertions.assertEquals("9012", product.getOfferId());
        product = purchase.getProducts().get(1);
        Assertions.assertEquals("12346", product.getOfferId());
        Assertions.assertEquals("1", product.getCategoryId());
        DiscountPrice discountPrice = product.getDiscountPrice();
        Assertions.assertEquals(CurrencyCode.RUR, discountPrice.getCurrency());
        Assertions.assertEquals(BigDecimal.valueOf(501), discountPrice.getValue());

        //// promo's gifts
        PromoGift promoGift = promo.getPromoGifts().get(0);
        Assertions.assertEquals(9012L, promoGift.getOfferId());
        promoGift = promo.getPromoGifts().get(1);
        Assertions.assertEquals(33L, promoGift.getGiftId());
    }

    private void checkGifts(Object[] gifts) {
        //// gifts
        Gift gift = (Gift) gifts[0];
        Assertions.assertEquals(33L, gift.getId());
        Assertions.assertEquals("Кружка 300 мл Brand 16", gift.getName());
        Assertions.assertEquals("https://best.seller.ru/promos/33.jpg", gift.getPicture());
    }

    private void checkOffers(Object[] offers) throws IOException, URISyntaxException {
        //// offers
        Offer offer = (Offer) offers[0];
        Assertions.assertEquals("Brand", offer.getManufacturer());
        Assertions.assertEquals("9012", offer.getId());
        Assertions.assertEquals(80, offer.getBid());
        Assertions.assertEquals(79, offer.getCbid());
        Assertions.assertEquals("Мороженица Brand 3811", offer.getName());
        Assertions.assertEquals("Brand", offer.getVendor());
        Assertions.assertEquals("vendor.model", offer.getType());
        Assertions.assertEquals("3811", offer.getModel());
        Assertions.assertEquals("A1234567B", offer.getVendorCode());
        Assertions.assertEquals("http://best.seller.ru/product_page.asp?pid=12345", offer.getUrl());
        Assertions.assertEquals(BigDecimal.valueOf(8990), offer.getPrice().getValue());
        Assertions.assertEquals(BigDecimal.valueOf(9990), offer.getOldPrice());
        Assertions.assertEquals("true", offer.getEnableAutoDiscounts());

        Currency currency = offer.getCurrency();
        Assertions.assertEquals(CurrencyCode.RUR, currency.getCurrencyCode());

        Category category = offer.getCategory();
        Assertions.assertEquals(101L, category.getId());

        Assertions.assertEquals("http://best.seller.ru/img/model_12345.jpg", offer.getPicture());
        assertTrue(offer.getDelivery());
        assertTrue(offer.getPickup());
        Assertions.assertEquals(2, offer.getMinQuantity());
        Assertions.assertFalse(offer.getAdult());
        assertTrue(offer.getPickup());
        Assertions.assertEquals(111L, offer.getGroupId());
        assertTrue(offer.getAvailable());
        Assertions.assertFalse(offer.getDownloadable());
        Assertions.assertEquals("P1Y2M10DT2H30M", offer.getExpiry());
        Assertions.assertEquals("Мороженица", offer.getTypePrefix());
        Assertions.assertEquals(readContent("descriptionFirst.txt"), offer.getDescription());

        // offer's delivery options
        //// delivery-options
        DeliveryOption deliveryOption = offer.getDeliveryOptions().get(0);
        Assertions.assertEquals(300, deliveryOption.getCost());
        Assertions.assertEquals("1", deliveryOption.getDays());
        Assertions.assertEquals(18, deliveryOption.getOrderBefore());

        //// offer's pickup-options
        deliveryOption = offer.getPickupOptions().get(0);
        Assertions.assertEquals(300, deliveryOption.getCost());
        Assertions.assertEquals("1-3", deliveryOption.getDays());

        assertTrue(offer.getStore());
        Assertions.assertEquals("Необходима предоплата.", offer.getSalesNotes());
        assertTrue(offer.getManufacturerWarranty());
        Assertions.assertEquals("Китай", offer.getCountryOfOrigin());
        Assertions.assertEquals("4601546021298", offer.getBarcode());

        // offer's param
        Param param = offer.getParams().get(0);
        Assertions.assertEquals("Цвет", param.getName());
        Assertions.assertEquals("белый", param.getValue());
        Assertions.assertEquals("Видимый спектр", param.getUnit());

        // offer's condition
        Condition condition = offer.getCondition();
        Assertions.assertEquals("likenew", condition.getType());
        Assertions.assertEquals("Повреждена упаковка", condition.getValue());

        Assertions.assertEquals(20034L, offer.getCreditTemplate());
        Assertions.assertEquals(3.6, offer.getWeight());
        Assertions.assertEquals("20.1/20.551/22.5", offer.getDimensions());
        Assertions.assertEquals("Срок годности", offer.getExpiryComment());
        Assertions.assertEquals("P1Y", offer.getLifeTimeDays());
        Assertions.assertEquals("Срок службы", offer.getLifeTimeComment());
        Assertions.assertEquals("P6M", offer.getGuaranteePeriodDays());
        Assertions.assertEquals("Гарантия", offer.getGuaranteePeriodComment());
        Assertions.assertEquals("123456", offer.getCertificateOfConformity());
        Assertions.assertEquals(2, offer.getCustomsCommodityCodes().size());
        Assertions.assertEquals("Поставки будут", offer.getSupplyPlan());
        Assertions.assertEquals(10, offer.getTransportUnitSize());
        Assertions.assertEquals(11, offer.getMinShipment());
        Assertions.assertEquals(22, offer.getQuantumOfSupply());
        Assertions.assertEquals(2, offer.getSupplyScheduleDays().size());
        Assertions.assertEquals(7, offer.getDeliveryDurationDays());
        Assertions.assertEquals(2, offer.getBoxCount());


        //// offers
        offer = (Offer) offers[1];
        Assertions.assertEquals("Brand", offer.getManufacturer());
        Assertions.assertEquals("12346", offer.getId());
        Assertions.assertEquals(60, offer.getBid());
        Assertions.assertEquals("Сэндвичница", offer.getTypePrefix());
        Assertions.assertEquals("Brand", offer.getVendor());
        Assertions.assertEquals("vendor.model", offer.getType());
        Assertions.assertEquals("K220Y9", offer.getModel());
        Assertions.assertEquals("A1234567B", offer.getVendorCode());
        Assertions.assertEquals("http://best.seller.ru/product_page.asp?pid=12345", offer.getUrl());
        Assertions.assertEquals(BigDecimal.valueOf(1099.1), offer.getPrice().getValue());
        Assertions.assertEquals(BigDecimal.valueOf(1399), offer.getOldPrice());
        Assertions.assertEquals("false", offer.getEnableAutoDiscounts());
        Assertions.assertNull(offer.getAvailable());

        currency = offer.getCurrency();

        category = offer.getCategory();

        Assertions.assertEquals("http://best.seller.ru/img/device56789.jpg", offer.getPicture());
        Assertions.assertFalse(offer.getDelivery());
        assertTrue(offer.getPickup());
        Assertions.assertEquals("9876543210", offer.getBarcode());
        Assertions.assertEquals(1.03, offer.getWeight());
        Assertions.assertEquals("20.800/23.500/9.000", offer.getDimensions());
        Assertions.assertEquals("Россия", offer.getCountryOfOrigin());
        assertTrue(offer.getManufacturerWarranty());
        Assertions.assertEquals("Наличные, Visa/Mastercard, б/н расчет", offer.getSalesNotes());
        Assertions.assertEquals("Сэндвичница 2 в 1: можно приготовить как сэндвичи, так и вафли.",
                offer.getDescription());

        // offer's param
        param = offer.getParams().get(0);
        Assertions.assertEquals("Мощность", param.getName());
        Assertions.assertEquals("750 Вт", param.getValue());

        //// offer's pickup-options
        deliveryOption = offer.getPickupOptions().get(0);
        Assertions.assertEquals(350, deliveryOption.getCost());
        Assertions.assertEquals("1", deliveryOption.getDays());
        Assertions.assertEquals(12, deliveryOption.getOrderBefore());

        //// offers
        offer = (Offer) offers[2];
        Assertions.assertEquals("04714f11-4132-11ea-0a80-006f0017db10", offer.getId());
        Assertions.assertEquals(BigDecimal.valueOf(2290), offer.getPrice().getValue());
        Assertions.assertEquals("114395098", offer.getMarketSku());
        Assertions.assertEquals(readContent("descriptionSecond.txt"), offer.getDescription());

        currency = offer.getCurrency();
        Assertions.assertEquals(CurrencyCode.RUR, currency.getCurrencyCode());

        Assertions.assertFalse(offer.getDisabled());
        Assertions.assertEquals(1, offer.getCount());
    }

    private String readContent(String filename) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass()
                .getClassLoader()
                .getResourceAsStream(filename))))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private void checkShop(Shop shop) {
        Assertions.assertNotNull(shop);

        //// shop propetries
        Assertions.assertEquals("BestSeller", shop.getName());
        Assertions.assertEquals("Tne Best inc.", shop.getCompany());
        Assertions.assertEquals("http://best.seller.ru", shop.getUrl());
        Assertions.assertEquals("uCoz", shop.getPlatform());
        Assertions.assertEquals("1.0", shop.getVersion());
        Assertions.assertEquals("Технологичные решения", shop.getAgency());
        Assertions.assertEquals("example-email@gmail.com", shop.getEmail());
        DeliveryOption deliveryOption = shop.getDeliveryOptions().get(0);
        Assertions.assertEquals(200, deliveryOption.getCost());
        Assertions.assertEquals("1", deliveryOption.getDays());
        Assertions.assertEquals(200, deliveryOption.getCost());
        Assertions.assertEquals("1", deliveryOption.getDays());

        //// currencies
        // RUR
        Currency currency = shop.getCurrencies().get(0);
        Assertions.assertEquals(CurrencyCode.RUR, currency.getCurrencyCode());
        Assertions.assertEquals("1", currency.getRate());
        // USD
        currency = shop.getCurrencies().get(1);
        Assertions.assertEquals(CurrencyCode.USD, currency.getCurrencyCode());
        Assertions.assertEquals("60", currency.getRate());

        //// categories
        // 1
        Category category = shop.getCategories().get(0);
        Assertions.assertEquals(1L, category.getId());
        Assertions.assertEquals("Бытовая техника", category.getName());
        // 10
        category = shop.getCategories().get(1);
        Assertions.assertEquals(10L, category.getId());
        Assertions.assertEquals("Мелкая техника для кухни", category.getName());
        // 101
        category = shop.getCategories().get(2);
        Assertions.assertEquals(101L, category.getId());
        Assertions.assertEquals("Сэндвичницы и приборы для выпечки", category.getName());

        //// delivery-options
        deliveryOption = shop.getDeliveryOptions().get(0);
        Assertions.assertEquals(200, deliveryOption.getCost());
        Assertions.assertEquals("1", deliveryOption.getDays());

        //// delivery-options
        deliveryOption = shop.getPickupOptions().get(0);
        Assertions.assertEquals(200, deliveryOption.getCost());
        Assertions.assertEquals("1", deliveryOption.getDays());
    }
}
