package ru.yandex.market.mbi.datacamp.saas.impl;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMeta;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DisabledBySourceAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.PartnerStatus;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SupplyStatus;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferAttributes;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.saas.search.response.SaasSearchDocument;

import static Market.DataCamp.DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING;
import static Market.DataCamp.DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED;

/**
 * Абстрактный класс для теста запросов в SaaS.
 * Date: 21.06.2021
 * Project: arcadia-market_mbi_datacamp-client
 *
 * @author alexminakov
 */
public abstract class AbstractSaasDatacampTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    protected static final long T_BUSINESS_ID = 10462389L;

    protected static final Integer T_CONTENT_STATUS
            = DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY.getNumber();

    protected static final List<PartnerStatus> T_PARTNER_STATUSES = List.of(
            new PartnerStatus(10462382L, PartnerStatus.PartnerStatusType.HIDDEN),
            new PartnerStatus(10462383L, PartnerStatus.PartnerStatusType.AVAILABLE),
            new PartnerStatus(10462384L, PartnerStatus.PartnerStatusType.HIDDEN)
    );

    protected static final List<SupplyStatus> T_SUPPLY_STATUSES = List.of(
            new SupplyStatus(10462382L, DataCampOfferContent.SupplyPlan.Variation.WILL_SUPPLY),
            new SupplyStatus(10462383L, DataCampOfferContent.SupplyPlan.Variation.WONT_SUPPLY),
            new SupplyStatus(10462384L, DataCampOfferContent.SupplyPlan.Variation.ARCHIVE)
    );

    protected static final List<DisabledBySourceAttribute> T_DISABLED_BY_SOURCES = List.of(
            new DisabledBySourceAttribute(10462382L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API),
            new DisabledBySourceAttribute(10462382L, DataCampOfferMeta.DataSource.MARKET_PRICELABS),
            new DisabledBySourceAttribute(10462383L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
    );

    protected SaasOfferFilter.Builder getDefaultOfferFilterBuilder() {
        return fillDefaultOfferAttributesBuilder(SaasOfferFilter.newBuilder())
                .setBusinessId(T_BUSINESS_ID)
                .setPrefix(T_BUSINESS_ID)
                .setPageRequest(SeekSliceRequest.firstN(1))
                .addResultOfferStatuses(10462383L, List.of(PUBLISHED, NOT_PUBLISHED_CHECKING))
                .addResultContentStatuses(
                        List.of(
                                DataCampContentStatus.ResultContentStatus.CardStatus.HAS_CARD_MARKET,
                                DataCampContentStatus.ResultContentStatus.CardStatus.NO_CARD_NEED_CONTENT
                        )
                )
                .addMarketCategoryIds(List.of(339091L));
    }

    protected static <T extends SaasOfferAttributes.Builder<T>> T fillDefaultOfferAttributesBuilder(
            @Nonnull T builder
    ) {
        return builder
                .setDocType(SaasDocType.OFFER)
                .addShopIds(List.of(10462382L, 10462383L, 10462384L))
                .addVendors(List.of("Ikea"))
                .addCategoryIds(List.of(587098539L))
                .setVariantId("301428000")
                .setGroupId(20112002L)
                .addPartnerStatuses(T_PARTNER_STATUSES)
                .addContentStatusesCPA(List.of(DataCampContentStatus.OfferContentCpaState.forNumber(2)))
                .addSupplyStatuses(T_SUPPLY_STATUSES)
                .addDisabledBySource(T_DISABLED_BY_SOURCES);
    }

    protected <T> T readJson(@Nonnull String docJsonpath, @Nonnull Class<T> tClass) throws IOException {
        return MAPPER.readValue(getClass().getResourceAsStream(docJsonpath), tClass);
    }

    protected SaasSearchDocument convertToDoc(@Nonnull ObjectNode saasDocJson) throws IOException {
        return MAPPER.readValue(saasDocJson.toString(), SaasSearchDocument.class);
    }
}
