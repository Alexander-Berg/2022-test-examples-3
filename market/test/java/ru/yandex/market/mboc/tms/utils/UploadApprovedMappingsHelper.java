package ru.yandex.market.mboc.tms.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

/**
 * @author apluhin
 * @created 10/28/20
 */
public class UploadApprovedMappingsHelper {

    private static long nextStamp = 0;

    public static void trace(String fileName, Collection<?> coll) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            coll.forEach(pw::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <E> List<E> mul(List<E> list, int m) {
        List<E> res = new ArrayList<>(list.size() * m);
        for (int i = 0; i < m; i++) {
            res.addAll(list);
        }
        return res;
    }

    public static Offer createOffer(Supplier supplier) {
        return createOffer(12345, supplier);
    }

    public static Offer createWhiteOffer(Supplier supplier) {
        Offer offer = new Offer();
        offer.setBusinessId(supplier.getId());
        offer.setModelId(11L);
        offer.setMappedModelId(11L, Offer.MappingConfidence.CONTENT);
        offer.setTitle("White offer");
        offer.setShopSku("12345");
        offer.setShopCategoryName("test category");
        offer.setIsOfferContentPresent(true);
        offer.storeOfferContent(OfferContent.initEmptyContent());
        offer.setMappingDestination(Offer.MappingDestination.WHITE);
        offer.setUploadToYtStamp(nextStamp++);
        offer.addNewServiceOfferIfNotExistsForTests(supplier);
        offer.setVendor("vendor");
        offer.setVendorId(12);
        offer.setCategoryIdForTests(13L, Offer.BindingKind.APPROVED);
        offer.approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.CONTENT);
        offer.setUploadToYtStamp(nextStamp++);
        return offer;
    }

    public static Offer createUploadToYtOffer(
        long mappingId,
        Offer.MappingType mappingType,
        Offer.MappingDestination destination,
        Supplier supplier,
        String shopSku
    ) {
        Offer offer = new Offer();
        offer.setBusinessId(supplier.getId());
        offer.setModelId(11L);
        offer.setTitle("Approved offer");
        offer.setShopSku(shopSku);
        offer.setShopCategoryName("test category");
        offer.setIsOfferContentPresent(true);
        offer.storeOfferContent(OfferContent.initEmptyContent());
        offer.setCategoryIdForTests(99L, Offer.BindingKind.APPROVED);

        Offer.Mapping mapping = new Offer.Mapping(
            mappingId,
            LocalDateTime.of(2018, Month.APRIL, 28, 15, 26)
        );
        mappingType.set(offer, mapping);

        offer.setBarCode("44444");
        offer.approve(mappingType, Offer.MappingConfidence.CONTENT);
        offer.setUploadToYtStamp(nextStamp++);
        if (supplier.getType() != MbocSupplierType.BUSINESS) {
            offer.addNewServiceOfferIfNotExistsForTests(supplier);
        }
        offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        offer.approve(mappingType, Offer.MappingConfidence.CONTENT);
        offer.setUploadToYtStamp(nextStamp++);
        offer.setMappingDestination(destination);
        return offer;
    }

    public static Offer createOffer(long mappingId, Supplier supplier) {
        Offer offer = new Offer();
        offer.setBusinessId(supplier.getId());
        offer.setModelId(11L);

        offer.setTitle("Content offer");
        offer.setShopSku("12345");
        offer.setShopCategoryName("test category");
        offer.setIsOfferContentPresent(true);
        offer.storeOfferContent(OfferContent.initEmptyContent());
        offer.setContentSkuMapping(
            new Offer.Mapping(
                mappingId,
                LocalDateTime.of(2018, Month.APRIL, 28, 15, 26)
            )
        );
        offer.setBarCode("44444");
        if (supplier.getType() != MbocSupplierType.BUSINESS) {
            offer.addNewServiceOfferIfNotExistsForTests(supplier);
        }
        offer.setVendor("vendor");
        offer.setVendorId(12);
        offer.setCategoryIdForTests(13L, Offer.BindingKind.APPROVED);
        offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        offer.approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT);
        offer.setUploadToYtStamp(nextStamp++);
        return offer;
    }

    public static Offer createOffer(Supplier supplier, long mappingId) {
        return createOffer(mappingId, supplier);
    }

    public static Offer createNonProcessedOffer(Supplier supplier) {
        Offer offer = new Offer();
        offer.setBusinessId(supplier.getId());
        offer.setModelId(22L);
        offer.setTitle("Processed offer");
        offer.setShopSku("54321");
        offer.setShopCategoryName("test category 2");
        offer.setIsOfferContentPresent(true);
        offer.storeOfferContent(
            OfferContent.builder().description("test description").build());
        offer.setSuggestSkuMapping(
            new Offer.Mapping(
                54321,
                LocalDateTime.of(2018, Month.APRIL, 28, 15, 26)
            )
        );
        offer.addNewServiceOfferIfNotExistsForTests(supplier);
        offer.setVendor("vendor");
        offer.setVendorId(12);
        offer.setCategoryIdForTests(13L, Offer.BindingKind.APPROVED);
        return offer;
    }

    public static Offer createUploadedOffer() {
        Offer resultOffer = new Offer();
        resultOffer.setBusinessId(3);
        resultOffer.setModelId(33L);
        resultOffer.setTitle("Processed offer & uploaded offer");
        resultOffer.setShopSku("12345");
        resultOffer.setIsOfferContentPresent(true);
        resultOffer.setShopCategoryName("test category");
        resultOffer.storeOfferContent(OfferContent.initEmptyContent());
        resultOffer.setCategoryIdForTests(99L, Offer.BindingKind.APPROVED);
        resultOffer.setContentSkuMapping(
            new Offer.Mapping(
                12345,
                LocalDateTime.of(2018, Month.APRIL, 28, 15, 26)
            )
        );
        resultOffer.approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT);
        resultOffer.setUploadToYtStamp(DateTimeUtils.instantNow().getEpochSecond());
        return resultOffer;
    }

}
