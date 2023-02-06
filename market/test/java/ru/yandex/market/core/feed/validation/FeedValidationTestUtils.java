package ru.yandex.market.core.feed.validation;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.assortment.model.AssortmentFeedValidationRequest;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidation;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationInfo;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationType;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.validation.model.UnitedValidationInfoWrapper;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.core.tax.model.VatRate;

/**
 * Класс утилита для валидации фидов
 * Date: 09.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class FeedValidationTestUtils {

    private FeedValidationTestUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static AssortmentValidation createAssortmentValidation(long validationId, long partnerId,
                                                                  RemoteResource remoteResource,
                                                                  AssortmentValidationType validationType) {
        return createAssortmentValidation(validationId, partnerId, remoteResource, validationType, null, null);
    }

    @Nonnull
    public static AssortmentValidation createAssortmentValidation(long validationId, long partnerId,
                                                                  RemoteResource remoteResource,
                                                                  AssortmentValidationType validationType,
                                                                  @Nullable Long uploadId,
                                                                  @Nullable List<String> parsingFields) {
        return new AssortmentValidation.Builder()
                .setId(validationId)
                .setStatus(FeedProcessingResult.PROCESSING)
                .setRequestTime(Instant.now())
                .setRequest(new AssortmentFeedValidationRequest.Builder()
                        .setPartnerId(partnerId)
                        .setType(validationType)
                        .setSupplierType(SupplierType.REAL_SUPPLIER)
                        .setParsingFields(parsingFields)
                        .setVatRate(VatRate.NO_VAT)
                        .setResource(remoteResource)
                        .setUploadId(uploadId)
                        .build())
                .build();
    }

    @Nonnull
    public static UnitedValidationInfoWrapper createUnitedValidationInfoWrapper(long validationId, long partnerId,
                                                                                RemoteResource remoteResource,
                                                                                CampaignType type,
                                                                                AssortmentValidationType validationType,
                                                                                @Nullable Long uploadId,
                                                                                @Nullable List<String> parsingFields) {
        var assortmentValidation = createAssortmentValidation(validationId,
                partnerId, remoteResource, validationType, uploadId, parsingFields);
        var validationInfo = AssortmentValidationInfo.of(assortmentValidation);

        return new UnitedValidationInfoWrapper(validationInfo, type);
    }

    @Nonnull
    public static UnitedValidationInfoWrapper createUnitedValidationInfoWrapper(long validationId, long partnerId,
                                                                                RemoteResource remoteResource,
                                                                                CampaignType type) {
        return createUnitedValidationInfoWrapper(validationId, partnerId, remoteResource,
                type, AssortmentValidationType.MAPPING_WITH_PRICES, null, null);
    }
}
