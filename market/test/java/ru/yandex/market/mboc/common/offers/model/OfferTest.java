package ru.yandex.market.mboc.common.offers.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import Market.DataCamp.DataCampContentMarketParameterValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus;
import ru.yandex.market.mboc.common.utils.ObjectsEqualByFieldsUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.blueSupplierUnderBiz1;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.blueSupplierUnderBiz2;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.dsbsSupplierUnderBiz;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.whiteSupplier;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.whiteSupplierUnderBiz;

/**
 * @author yuramalinov
 * @created 20.06.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferTest {
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .exclude(new FieldDefinition<>("marketParameterValues", List.class, Offer.class))
            .seed(16156)
            .build();
    }

    @Test
    public void testSupplierMappingApproved() {
        Offer offer = OfferTestUtils.nextOffer()
            .setSupplierSkuMapping(OfferTestUtils.mapping(1, "supplier test mapping"))
            .setContentSkuMapping(OfferTestUtils.mapping(2, "content test mapping"))
            .setCategoryIdForTests(99L, Offer.BindingKind.SUPPLIER);

        offer.approve(Offer.MappingType.SUPPLIER, Offer.MappingConfidence.CONTENT);

        assertThat(offer.getApprovedSkuMapping())
            .matches(approved -> Offer.Mapping.mappingEqual(approved, offer.getSupplierSkuMapping()));
    }

    @Test
    public void testContentMappingApproved() {
        Offer offer = OfferTestUtils.nextOffer()
            .setSupplierSkuMapping(OfferTestUtils.mapping(1, "supplier test mapping"))
            .setContentSkuMapping(OfferTestUtils.mapping(2, "content test mapping"))
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED);

        offer.approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT);

        assertThat(offer.getApprovedSkuMapping())
            .matches(approved -> Offer.Mapping.mappingEqual(approved, offer.getContentSkuMapping()));

        Offer offer2 = OfferTestUtils.nextOffer()
            .setSupplierSkuMapping(OfferTestUtils.mapping(1, "supplier test mapping"))
            .setContentSkuMapping(OfferTestUtils.mapping(2, "content test mapping"));

        offer2.approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT);

        assertThat(offer.getApprovedSkuMapping())
            .matches(approved -> Offer.Mapping.mappingEqual(approved, offer.getContentSkuMapping()));
    }

    @Test
    public void testBarCodes() {
        int lengthLimit = 10;
        assertThat(Offer.convertBarCode(null, lengthLimit)).isNull();
        assertThat(Offer.convertBarCode("123", lengthLimit)).isEqualTo("123");
        assertThat(Offer.convertBarCode("123  4", lengthLimit)).isEqualTo("123,4");
        assertThat(Offer.convertBarCode("123  4, 5", lengthLimit)).isEqualTo("123,4,5");
        assertThat(Offer.convertBarCode("123|11| 3", lengthLimit)).isEqualTo("123,11,3");
        assertThat(Offer.convertBarCode("123|11|; 3", lengthLimit)).isEqualTo("123,11,3");
        assertThat(Offer.convertBarCode("12|3456,678", lengthLimit)).isEqualTo("12,3456");
        assertThat(Offer.convertBarCode("12|3456,67", lengthLimit)).isEqualTo("12,3456,67");
        assertThat(Offer.convertBarCode("12|3456,6,7", lengthLimit)).isEqualTo("12,3456,6");
    }

    @Test
    public void testCopy() {
        for (int i = 0; i < 10; i++) {
            Offer offer = random.nextObject(Offer.class);
            assertThat(offer).isEqualToComparingFieldByField(new Offer(offer));
        }
    }

    @Test
    public void testEqualsIgnoringTransient() {
        var technicalFields = Set.of(
            "lastVersion",
            "uploadToYtStamp",
            "updated"
        );
        var contentFields = Set.of("offerContent");
        // Test only main fields
        testEqualsIgnoringTransientCase(
            (a, b) -> a.equalsIgnoringTransient(b, true, true),
            Sets.union(technicalFields, contentFields)
        );
        // Test with technical
        testEqualsIgnoringTransientCase(
            (a, b) -> a.equalsIgnoringTransient(b, false, true),
            contentFields
        );
        // Test with content
        testEqualsIgnoringTransientCase(
            (a, b) -> a.equalsIgnoringTransient(b, true, false),
            technicalFields
        );
    }

    private void testEqualsIgnoringTransientCase(BiFunction<Offer, Offer, Boolean> equality,
                                                 Set<String> expectedEqual) {
        // Transient + offerContent
        var alwaysIgnored = Set.of(
            "isOfferContentPresent",
            "marketParameterValues",
            "beruPrice",
            "transientChangeSource",
            "referencePrice",
            "transientBaseOfferSupplierId",
            "transientModifiedBy",
            "transientChangeSourceId",
            "adult",
            "approvedSkuCargoType");
        var equalityChecker = new ObjectsEqualByFieldsUtil<>(
            new Offer(),
            Offer::copy,
            equality,
            alwaysIgnored,
            // Add here fields that can't be randomly generated (e.g. collections)
            Map.of(
                "contentComments", () -> List.of("comment"),
                "additionalTickets", () -> Map.of(Offer.AdditionalTicketType.RECLASSIFICATION, "val"),
                "serviceOffers", () -> List.of(new Offer.ServiceOffer()),
                "commentsFromClab", () -> List.of(new ContentComment(ContentCommentType.CANCELLED, "val")),
                "marketParameterValues", () ->
                    List.of(DataCampContentMarketParameterValue.MarketParameterValue.newBuilder().build())
            )
        );
        equalityChecker.findEquality(random);
        Assertions.assertThat(equalityChecker.getEqualFields()).containsExactlyInAnyOrderElementsOf(expectedEqual);
    }

    @Test
    public void testStatusesModifiedChange() {
        Offer offer = new Offer();
        offer.addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier());
        LocalDateTime processingTsOld = LocalDateTime.parse("2018-10-01T00:00:00.001");
        LocalDateTime acceptanceTsOld = LocalDateTime.parse("2018-10-01T00:00:00.002");
        offer.setProcessingStatusModifiedInternal(processingTsOld);
        offer.setAcceptanceStatusModifiedInternal(acceptanceTsOld);

        offer.setProcessingStatusInternal(ProcessingStatus.PROCESSED)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK);
        assertThat(offer.getProcessingStatusModified()).isEqualTo(processingTsOld);
        assertThat(offer.getAcceptanceStatusModified()).isEqualTo(acceptanceTsOld);

        offer.updateProcessingStatusIfValid(ProcessingStatus.OPEN)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW);
        assertThat(offer.getProcessingStatusModified()).isAfter(processingTsOld);
        assertThat(offer.getAcceptanceStatusModified()).isAfter(acceptanceTsOld);
    }

    @Test
    public void testStatusesModifiedSameStatus() {
        Offer offer = new Offer();

        // To avoid updating processing status modified because offer destination changed
        offer.setOfferDestinationInternal(Offer.MappingDestination.WHITE);

        LocalDateTime processingTsOld = LocalDateTime.parse("2018-10-01T00:00:00.001");
        LocalDateTime acceptanceTsOld = LocalDateTime.parse("2018-10-01T00:00:00.002");
        offer.setProcessingStatusModifiedInternal(processingTsOld);
        offer.setAcceptanceStatusModifiedInternal(acceptanceTsOld);

        offer.updateProcessingStatusIfValid(ProcessingStatus.OPEN)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW);
        assertThat(offer.getProcessingStatusModified()).isEqualTo(processingTsOld);
        assertThat(offer.getAcceptanceStatusModified()).isEqualTo(acceptanceTsOld);
    }

    @Test
    public void testFullComment() {
        assertThat(new Offer().getFullContentComment()).isEqualTo("");
        assertThat(new Offer().setContentComment("test").getFullContentComment()).isEqualTo("test");
        assertThat(new Offer()
            .setContentComments(new ContentComment(ContentCommentType.DEPARTMENT_FROZEN))
            .getFullContentComment()).isEqualTo("Работы в департаменте заморожены");
        assertThat(new Offer()
            .setContentComments(
                new ContentComment(ContentCommentType.DEPARTMENT_FROZEN),
                new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "a", "b"))
            .getFullContentComment()).isEqualTo(
            "Работы в департаменте заморожены\nРасхождение информации в полях: a, b");
        assertThat(new Offer()
            .setContentComments(new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "a", "b"))
            .getFullContentComment()).isEqualTo("Расхождение информации в полях: a, b");
    }

    @Test
    public void testProcessingStatusCorrelation() {
        for (SupplierOffer.Offer.InternalProcessingStatus protoValue :
            SupplierOffer.Offer.InternalProcessingStatus.values()) {

            try {
                ProcessingStatus status = ProcessingStatus.convert(protoValue);
                assertThat(status).isNotNull();
            } catch (IllegalArgumentException e) {
                fail("Failed to find equivalent for " + protoValue);
            }
        }
    }

    @Test
    public void testPriceParsing() {
        Assertions.assertThat(offerWithPrice("123").extractOfferContent().getPriceAsNumber()).isNotEmpty().contains(123.0);
        Assertions.assertThat(offerWithPrice("123.10").extractOfferContent().getPriceAsNumber()).isNotEmpty().contains(123.1);
        Assertions.assertThat(offerWithPrice("123,10").extractOfferContent().getPriceAsNumber()).isNotEmpty().contains(123.1);
        Assertions.assertThat(offerWithPrice("12,123.10").extractOfferContent().getPriceAsNumber()).isNotEmpty().contains(12123.1);
        Assertions.assertThat(offerWithPrice("12,123.10 руб").extractOfferContent().getPriceAsNumber()).isEmpty();
    }

    private Offer offerWithPrice(String price) {
        return new Offer()
            .storeOfferContent(
                OfferContent.builder().extraShopFields(ImmutableMap.of("Цена", price)).build())
            .markLoadedContent();
    }

    @Test
    public void testUpdateApprovedSkuMapping() {
        Offer offer = OfferTestUtils.nextOffer();
        Offer.Mapping approvedMappingFirst = OfferTestUtils.mapping(1L);
        Offer.Mapping approvedMappingSecond = OfferTestUtils.mapping(2L);
        Offer.Mapping zeroMapping = OfferTestUtils.mapping(0L);

        // init mapping - deleted is null
        offer.updateApprovedSkuMapping(approvedMappingFirst, CONTENT);
        assertThat(offer.getApprovedSkuMapping()).isEqualTo(approvedMappingFirst);
        assertThat(offer.getDeletedApprovedSkuMapping()).isNull();

        // update mapping - deleted is unchanged
        offer.updateApprovedSkuMapping(approvedMappingSecond, CONTENT);
        assertThat(offer.getApprovedSkuMapping()).isEqualTo(approvedMappingSecond);
        assertThat(offer.getDeletedApprovedSkuMapping()).isNull();

        // set same mapping - deleted is unchanged
        offer.updateApprovedSkuMapping(approvedMappingSecond, CONTENT);
        assertThat(offer.getApprovedSkuMapping()).isEqualTo(approvedMappingSecond);
        assertThat(offer.getDeletedApprovedSkuMapping()).isNull();

        // delete mapping - deleted is set to deleted value
        offer.updateApprovedSkuMapping(zeroMapping, CONTENT);
        assertThat(offer.getApprovedSkuMapping()).isEqualTo(zeroMapping);
        assertThat(offer.getDeletedApprovedSkuMapping()).isEqualTo(approvedMappingSecond);

        // set mapping after deletion - deleted is unchanged
        offer.updateApprovedSkuMapping(approvedMappingFirst, CONTENT);
        assertThat(offer.getApprovedSkuMapping()).isEqualTo(approvedMappingFirst);
        assertThat(offer.getDeletedApprovedSkuMapping()).isEqualTo(approvedMappingSecond);
    }

    @Test
    public void testNewWhiteOfferDestinationIsWhite() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer.addNewServiceOfferIfNotExistsForTests(whiteSupplier());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }


    @Test
    public void testTrashWhiteOfferDestinationIsWhite() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer.addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.TRASH);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }

    @Test
    public void testAcceptedWhiteOfferDestinationIsWhite() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer.addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.OK);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }

    @Test
    public void testNewBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer.addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testTrashBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.TRASH);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testAcceptedBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.OK);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testNewWhiteAndNewBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testNewWhiteAndTrashBlueOfferDestinationIsWhite() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.TRASH);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }

    @Test
    public void testAcceptedWhiteAndNewBlueOfferDestinationIsWhite() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.OK)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }

    @Test
    public void testAcceptedWhiteAndTrashBlueOfferDestinationIsWhite() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.OK)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.TRASH);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.WHITE);
    }

    @Test
    public void testAcceptedWhiteAndAcceptedBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.OK)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.OK);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testTrashWhiteAndNewBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.TRASH)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testTrashWhiteAndTrashBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .updateAcceptanceStatusForTests(whiteSupplier().getId(), Offer.AcceptanceStatus.TRASH)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.TRASH);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testNewWhiteAndTrashBlueAndNewBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.TRASH)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz2());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testWhiteAndTrashBlueAndAcceptedBlueOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplier())
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz1().getId(), Offer.AcceptanceStatus.TRASH)
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz2())
            .updateAcceptanceStatusForTests(blueSupplierUnderBiz2().getId(), Offer.AcceptanceStatus.OK);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testWhiteAndDsbsOfferDestinationIsDsbs() {
        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplierUnderBiz())
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplierUnderBiz())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);
    }

    @Test
    public void testBlueAndDsbsOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplierUnderBiz())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testWhiteAndBlueAndDsbsOfferDestinationIsBlue() {
        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setServiceOffers(Collections.emptyList());

        offer
            .addNewServiceOfferIfNotExistsForTests(whiteSupplierUnderBiz())
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplierUnderBiz())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
    }

    @Test
    public void testUpdateProcessingStatusModifiedOnDestinationChange() throws Exception {
        Offer offer = OfferTestUtils.nextOffer()
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setServiceOffers(Collections.emptyList())
            .addNewServiceOfferIfNotExistsForTests(dsbsSupplierUnderBiz());
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);
        LocalDateTime before = offer.getProcessingStatusModified();

        // Sleep is bad, but there's no other way to ensure after > before
        Thread.sleep(1);

        offer
            .addNewServiceOfferIfNotExistsForTests(blueSupplierUnderBiz1())
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.BLUE);
        LocalDateTime after = offer.getProcessingStatusModified();

        assertThat(after).isAfter(before);
    }
}
