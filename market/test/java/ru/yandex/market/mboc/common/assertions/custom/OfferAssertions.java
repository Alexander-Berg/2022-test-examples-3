package ru.yandex.market.mboc.common.assertions.custom;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.Objects;
import org.assertj.core.internal.TypeComparators;

import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.MbocComparators;

import static org.assertj.core.internal.TypeComparators.defaultTypeComparators;

/**
 * @author s-ermakov
 */
public class OfferAssertions extends AbstractObjectAssert<OfferAssertions, Offer> {

    protected Iterables iterables = Iterables.instance();
    private Objects objects = Objects.instance();
    private TypeComparators comparatorByType = defaultTypeComparators();

    public OfferAssertions(Offer actual) {
        super(actual, OfferAssertions.class);
        comparatorByType.put(Offer.Mapping.class, MbocComparators.OFFERS_MAPPING_COMPARATOR);
        comparatorByType.put(LocalDateTime.class, MbocComparators.LOCAL_DATE_TIME_COMPARATOR);
    }

    public static OfferAssertions assertThat(Offer actual) {
        return new OfferAssertions(actual)
            .usingComparatorForType(MbocComparators.OFFERS_MAPPING_COMPARATOR, Offer.Mapping.class)
            .usingComparatorForType(MbocComparators.LOCAL_DATE_TIME_COMPARATOR, LocalDateTime.class);
    }

    @Override
    public OfferAssertions isEqualTo(Object expected) {
        super.usingDefaultComparator();

        return super.isEqualToIgnoringGivenFields(expected,
            "lastVersion",
            "uploadToYtStamp",
            "deletedApprovedSkuMapping",
            "isOfferContentPresent",
            "contentProcessed");
    }

    public OfferAssertions isEqualToIgnoreContent(Object expected) {
        super.usingDefaultComparator();

        return super.isEqualToIgnoringGivenFields(expected,
            "lastVersion",
            "uploadToYtStamp",
            "deletedApprovedSkuMapping",
            "isOfferContentPresent",
            "offerContent",
            "contentProcessed");
    }

    public OfferAssertions isEqualToWithoutYtStamp(Object expected) {
        super.usingDefaultComparator();

        return super.isEqualToIgnoringGivenFields(expected,
            "lastVersion",
            "uploadToYtStamp",
            "isOfferContentPresent");
    }

    @Override
    public OfferAssertions isNotEqualTo(Object other) {
        super.usingComparator(MbocComparators.OFFERS_GENERAL);
        return super.isNotEqualTo(other);
    }

    /**
     * Same logic as in {@link MbocComparators#equalsWithCreatedUpdated(Offer, Offer)}.
     */
    public OfferAssertions isEqualWithCreatedUpdated(Offer expected) {
        super.usingComparator(MbocComparators.OFFERS_WITH_CREATED_UPDATED);
        return super.isEqualTo(expected);
    }

    public OfferAssertions isNotEqualWithCreatedUpdated(Offer expected) {
        super.usingComparator(MbocComparators.OFFERS_WITH_CREATED_UPDATED);
        return super.isNotEqualTo(expected);
    }

    public OfferAssertions hasContentMapping(long mappingId) {
        super.isNotNull();
        if (actual.getContentSkuMapping() == null) {
            failWithMessage("Expected offer to have content mapping <%d>, actual is <%s>",
                mappingId, actual.getContentSkuMapping());
        }
        objects.assertEqual(info, actual.getContentSkuMapping().getMappingId(), mappingId);
        return myself;
    }

    public OfferAssertions hasContentMapping(int mappingId, Offer.SkuType skuType) {
        return hasContentMapping(new Offer.Mapping(mappingId, LocalDateTime.MIN, skuType));
    }

    public OfferAssertions hasContentMapping(Offer.Mapping expectedMapping) {
        super.isNotNull();
        objects.assertIsEqualToIgnoringGivenFields(info, actual.getContentSkuMapping(), expectedMapping,
            Collections.emptyMap(), comparatorByType);
        return myself;
    }

    public OfferAssertions hasSupplierMapping(long mappingId) {
        super.isNotNull();
        if (actual.getSupplierSkuMapping() == null) {
            failWithMessage("Expected offer to have supplier mapping <%d>, actual is <%s>",
                mappingId, actual.getSupplierSkuMapping());
        }
        objects.assertEqual(info, actual.getSupplierSkuMapping().getMappingId(), mappingId);
        return myself;
    }

    public OfferAssertions hasSupplierMapping(int mappingId) {
        return hasSupplierMapping(new Offer.Mapping(mappingId, LocalDateTime.MIN));
    }

    public OfferAssertions hasSupplierMapping(Offer.Mapping expectedMapping) {
        super.isNotNull();
        objects.assertIsEqualToIgnoringGivenFields(info, actual.getSupplierSkuMapping(), expectedMapping,
            Collections.emptyMap(), comparatorByType);
        return myself;
    }

    public OfferAssertions hasSuggestedMapping(long mappingId) {
        super.isNotNull();
        if (actual.getSuggestSkuMapping() == null) {
            failWithMessage("Expected offer to have suggested mapping <%d>, actual is <%s>",
                mappingId, actual.getSuggestSkuMapping());
        }
        objects.assertEqual(info, actual.getSuggestSkuMapping().getMappingId(), mappingId);
        return myself;
    }

    public OfferAssertions hasSuggestedMapping(int mappingId) {
        return hasSuggestedMapping(new Offer.Mapping(mappingId, LocalDateTime.MIN));
    }

    public OfferAssertions hasSuggestedMapping(Offer.Mapping expectedMapping) {
        super.isNotNull();
        objects.assertIsEqualToIgnoringGivenFields(info, actual.getSuggestSkuMapping(), expectedMapping,
            Collections.emptyMap(), comparatorByType);
        return myself;
    }

    public OfferAssertions hasApprovedMapping(long mappingId) {
        super.isNotNull();
        if (actual.getApprovedSkuMapping() == null) {
            failWithMessage("Expected offer to have approved mapping <%d>, actual is <%s>",
                mappingId, actual.getApprovedSkuMapping());
        }
        objects.assertEqual(info, actual.getApprovedSkuMapping().getMappingId(), mappingId);
        return myself;
    }

    public OfferAssertions hasApprovedMapping(Offer.Mapping expectedMapping) {
        super.isNotNull();
        objects.assertIsEqualToIgnoringGivenFields(info, actual.getApprovedSkuMapping(), expectedMapping,
            Collections.emptyMap(), comparatorByType);
        return myself;
    }

    public OfferAssertions doesNotHaveApprovedMapping() {
        super.isNotNull();

        if (actual.hasApprovedSkuMapping()) {
            failWithMessage("Expected offer to have no approved mapping, actual is <%s>",
                actual.getApprovedSkuMapping());
        }

        return myself;
    }

    public OfferAssertions doesNotHaveContentMapping() {
        super.isNotNull();

        if (actual.hasContentSkuMapping()) {
            failWithMessage("Expected offer to have no content mapping, actual is <%s>",
                actual.getContentSkuMapping());
        }

        return myself;
    }

    public OfferAssertions doesNotHaveCategoryId() {
        super.isNotNull();

        if (actual.getCategoryId() != null) {
            failWithMessage("Expected offer to have no category id, actual is <%d>",
                actual.getCategoryId());
        }

        return myself;
    }

    public OfferAssertions doesNotHaveVendorId() {
        super.isNotNull();

        if (actual.getVendorId() != null) {
            failWithMessage("Expected offer to have no category id, actual is <%d>",
                actual.getVendorId());
        }

        return myself;
    }

    public OfferAssertions doesNotHaveContentStatusActiveError() {
        super.isNotNull();
        if (actual.getContentStatusActiveError() != null) {
            failWithMessage("Expected offer to have no content status active error, actual is <%s>",
                actual.getContentStatusActiveError());
        }
        return myself;
    }

    public OfferAssertions hasContentStatusActiveError() {
        super.isNotNull();
        objects.assertNotNull(info, actual.getContentStatusActiveError());
        return myself;
    }

    public OfferAssertions hasContentStatusActiveError(ErrorInfo expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentStatusActiveError(), expected);
        return myself;
    }

    public OfferAssertions hasComment(String comment) {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentComment(), comment);
        return myself;
    }

    public OfferAssertions hasContentComments(ContentComment... comments) {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentComments(), Arrays.asList(comments));
        return myself;
    }

    public OfferAssertions hasNoContentComments() {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentComments(), Collections.emptyList());
        return myself;
    }

    public OfferAssertions hasBindingKind(Offer.BindingKind bindingKind) {
        super.isNotNull();
        objects.assertEqual(info, actual.getBindingKind(), bindingKind);
        return myself;
    }

    public OfferAssertions hasAcceptanceStatus(Offer.AcceptanceStatus acceptanceStatus) {
        super.isNotNull();
        objects.assertEqual(info, actual.getAcceptanceStatus(), acceptanceStatus);
        return myself;
    }

    public OfferAssertions hasProcessingStatus(Offer.ProcessingStatus processingStatus) {
        super.isNotNull();
        objects.assertEqual(info, actual.getProcessingStatus(), processingStatus);
        return myself;
    }

    public OfferAssertions hasContentProcessingStatus(Offer.ContentProcessingStatus status) {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentProcessingStatus(), status);
        return myself;
    }

    public OfferAssertions hasCategoryId(long categoryId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getCategoryId(), categoryId);
        return myself;
    }

    public OfferAssertions hasMappedCategoryId(long categoryId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMappedCategoryId(), categoryId);
        return myself;
    }

    public OfferAssertions doesNotHaveMappedCategoryId() {
        super.isNotNull();
        if (actual.getMappedCategoryId() != null) {
            failWithMessage("Expected offer to have no mapped category id, actual is <%s>",
                actual.getMappedCategoryId());
        }
        return myself;
    }

    public OfferAssertions hasSupplierCategoryId(long categoryId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getSupplierCategoryId(), categoryId);
        return myself;
    }

    public OfferAssertions hasSupplierCategoryMappingStatus(Offer.MappingStatus status) {
        super.isNotNull();
        objects.assertEqual(info, actual.getSupplierCategoryMappingStatus(), status);
        return myself;
    }

    public OfferAssertions doesNotHaveSupplierCategoryId() {
        super.isNotNull();
        if (actual.getSupplierCategoryId() != null) {
            failWithMessage("Expected offer to have no supplier category id, actual is <%s>",
                actual.getSupplierCategoryId());
        }
        return myself;
    }

    public OfferAssertions hasApprovedSkuMappingConfidence(Offer.MappingConfidence approvedSkuMappingConfidence) {
        super.isNotNull();
        objects.assertEqual(info, actual.getApprovedSkuMappingConfidence(), approvedSkuMappingConfidence);
        return myself;
    }

    public OfferAssertions hasMappedCategoryConfidence(Offer.MappingConfidence mappedCategoryConfidence) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMappedCategoryConfidence(), mappedCategoryConfidence);
        return myself;
    }

    public OfferAssertions hasMappedModelConfidence(Offer.MappingConfidence mappedModelConfidence) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMappedModelConfidence(), mappedModelConfidence);
        return myself;
    }

    public OfferAssertions hasClassifierCategoryId(long categoryId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getClassifierCategoryId(), categoryId);
        return myself;
    }

    public OfferAssertions hasClassifierConfidence(Double classifierConfidence) {
        super.isNotNull();
        objects.assertEqual(info, actual.getClassifierConfidence(), classifierConfidence);
        return myself;
    }

    public OfferAssertions hasSupplierId(int supplierId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getBusinessId(), supplierId);
        return myself;
    }

    public OfferAssertions hasModelId(long modelId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getModelId(), modelId);
        return myself;
    }

    public OfferAssertions doesNotHaveModelId() {
        super.isNotNull();

        if (actual.getModelId() != null) {
            failWithMessage("Expected offer to have no model id mapping, actual is <%s>",
                actual.getModelId());
        }

        return myself;
    }

    public OfferAssertions hasMappedModelId(long modelId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMappedModelId(), modelId);
        return myself;
    }

    public OfferAssertions hasVendorId(int vendorId) {
        super.isNotNull();
        objects.assertEqual(info, actual.getVendorId(), vendorId);
        return myself;
    }

    public OfferAssertions hasMappingsEqualAndNotNull(Offer.MappingType mappingTypeFirst,
                                                      Offer.MappingType mappingTypeSecond) {
        super.isNotNull();
        objects.assertEqual(info, mappingTypeFirst.get(actual), mappingTypeSecond.get(actual));
        return myself;
    }

    public OfferAssertions hasMappingModifiedBy(String userName) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMappingModifiedBy(), userName);
        return myself;
    }

    public OfferAssertions hasModifiedByLogin(String userName) {
        super.isNotNull();
        objects.assertEqual(info, actual.getModifiedByLogin(), userName);
        return myself;
    }

    public OfferAssertions hasMappingDestination(Offer.MappingDestination expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.getMappingDestination(), expected);
        return myself;
    }

    public OfferAssertions hasTitle(String title) {
        super.isNotNull();
        objects.assertEqual(info, actual.getTitle(), title);
        return myself;
    }

    public OfferAssertions hasShopCategoryName(String shopCategoryName) {
        super.isNotNull();
        objects.assertEqual(info, actual.getShopCategoryName(), shopCategoryName);
        return myself;
    }

    public OfferAssertions hasDescription(String description) {
        super.isNotNull();
        objects.assertEqual(info, actual.extractOfferContent().getDescription(), description);
        return myself;
    }

    public OfferAssertions hasVendor(String vendor) {
        super.isNotNull();
        objects.assertEqual(info, actual.getVendor(), vendor);
        return myself;
    }

    public OfferAssertions hasVendorCode(String vendorCode) {
        super.isNotNull();
        objects.assertEqual(info, actual.getVendorCode(), vendorCode);
        return myself;
    }

    public OfferAssertions hasBarcodes(String... barcodes) {
        super.isNotNull();
        iterables.assertContainsExactlyInAnyOrder(info, actual.getAllBarCodes(), barcodes);
        return myself;
    }

    public OfferAssertions hasSupplierSkuMappingStatus(Offer.MappingStatus expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.getSupplierSkuMappingStatus(), expected);
        return myself;
    }

    public OfferAssertions hasContentLabState(Offer.ContentLabState expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentLabState(), expected);
        return myself;
    }

    public OfferAssertions hasContentLabMessage(String expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.getContentLabMessage(), expected);
        return myself;
    }

    public OfferAssertions hasAutomaticClassification(boolean expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.isAutomaticClassification(), expected);
        return myself;
    }

    public OfferAssertions hasReprocessRequested(boolean expected) {
        super.isNotNull();
        objects.assertEqual(info, actual.isReprocessRequested(), expected);
        return myself;
    }

    public OfferAssertions pskuHasContentMappings() {
        super.isNotNull();
        objects.assertEqual(info, actual.isPskuHasContentMappings(), true);
        return myself;
    }

    public OfferAssertions pskuHasNoContentMappings() {
        super.isNotNull();
        objects.assertEqual(info, actual.isPskuHasContentMappings(), false);
        return myself;
    }
}
