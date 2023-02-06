package ru.yandex.market.mboc.app.pipeline.datacamp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;

import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.offers.model.Offer;

class DCPipelineState {
    private Offer offer;
    private DataCampUnitedOffer.UnitedOffer datacampOffer;

    private DCPipelineState(Offer offer, DataCampUnitedOffer.UnitedOffer datacampOffer) {
        this.offer = offer;
        this.datacampOffer = datacampOffer;
    }

    public static DCPipelineState empty() {
        return new DCPipelineState(null, null);
    }

    public static DCPipelineState onlyOffer(Offer offer) {
        return new DCPipelineState(offer, null);
    }

    public static DCPipelineState full(Offer offer,
                                       DataCampContentStatus.ContentSystemStatus datacampStatus) {
        return new DCPipelineState(offer, DataCampUnitedOffer.UnitedOffer.newBuilder().build())
            .modifyDatacampStatus(__ -> datacampStatus.toBuilder());
    }

    public static DCPipelineState copy(DCPipelineState state) {
        return new DCPipelineState(
            state.offer == null ? null : state.offer.copy(),
            state.datacampOffer
        );
    }

    public DCPipelineState modifyOffer(Function<Offer, Offer> modifier) {
        this.offer = modifier.apply(this.offer);
        return this;
    }

    public DCPipelineState modifyDatacampStatus(
        Function<DataCampContentStatus.ContentSystemStatus.Builder,
            DataCampContentStatus.ContentSystemStatus.Builder> modifier
    ) {
        DataCampContentStatus.ContentSystemStatus.Builder currentStatus = Optional.ofNullable(this.datacampOffer)
            .flatMap(uo -> DataCampOfferUtil.Lens.content
                .then(DataCampOfferUtil.Lens.contentStatus)
                .then(DataCampOfferUtil.Lens.contentSystemStatus)
                .apply(uo.getBasic())
                .map(DataCampContentStatus.ContentSystemStatus::toBuilder)
            )
            .orElse(null);
        DataCampContentStatus.ContentSystemStatus.Builder newStatus = modifier.apply(currentStatus);
        DataCampUnitedOffer.UnitedOffer.Builder newUnitedOffer = this.datacampOffer != null
            ? this.datacampOffer.toBuilder() : DataCampUnitedOffer.UnitedOffer.newBuilder();
        newUnitedOffer.getBasicBuilder()
            .getContentBuilder()
            .getStatusBuilder()
            .setContentSystemStatus(newStatus);
        this.datacampOffer = newUnitedOffer.build();
        return this;
    }

    public DCPipelineState modifyDatacampServiceOffers(
        Function<Map<Integer, DataCampOffer.Offer>, Map<Integer, DataCampOffer.Offer>> modifier
    ) {
        Map<Integer, DataCampOffer.Offer> newServiceOffers =
            modifier.apply(this.datacampOffer != null ? new HashMap<>(this.datacampOffer.getServiceMap()) : null);
        DataCampUnitedOffer.UnitedOffer.Builder newUnitedOffer = this.datacampOffer != null
            ? this.datacampOffer.toBuilder()
            : DataCampUnitedOffer.UnitedOffer.newBuilder();
        this.datacampOffer = newUnitedOffer
            .clearService()
            .putAllService(newServiceOffers)
            .build();
        return this;
    }

    public DCPipelineState modifyBasicOffer(
        Function<DataCampOffer.Offer, DataCampOffer.Offer> modifier) {
        DataCampOffer.Offer basicOffer = modifier.apply(datacampOffer.getBasic());
        DataCampUnitedOffer.UnitedOffer.Builder newUnitedOffer = this.datacampOffer != null
            ? this.datacampOffer.toBuilder()
            : DataCampUnitedOffer.UnitedOffer.newBuilder();
        this.datacampOffer = newUnitedOffer.setBasic(basicOffer).build();
        return this;
    }

    public DCPipelineState modifyDatacampServiceOffer(
        int supplierId,
        Function<DataCampOffer.Offer, DataCampOffer.Offer> modifier
    ) {
        DataCampOffer.Offer serviceOffer = this.datacampOffer != null
            ? this.datacampOffer.getServiceMap().get(supplierId) : null;
        DataCampOffer.Offer newServiceOffer = modifier.apply(serviceOffer);
        DataCampUnitedOffer.UnitedOffer.Builder newUnitedOffer = this.datacampOffer != null
            ? this.datacampOffer.toBuilder()
            : DataCampUnitedOffer.UnitedOffer.newBuilder();
        this.datacampOffer = newUnitedOffer
            .putService(supplierId, newServiceOffer)
            .build();
        return this;
    }

    public Offer getOffer() {
        return offer;
    }

    public Optional<DataCampUnitedOffer.UnitedOffer> getDatacampOfferCopy() {
        if (datacampOffer == null) {
            return Optional.empty();
        }
        return Optional.of(DataCampUnitedOffer.UnitedOffer.newBuilder(datacampOffer).build());
    }

    public DataCampContentStatus.ContentSystemStatus getDatacampStatus() {
        return Optional.ofNullable(this.datacampOffer)
            .flatMap(uo -> DataCampOfferUtil.Lens.content
                .then(DataCampOfferUtil.Lens.contentStatus)
                .then(DataCampOfferUtil.Lens.contentSystemStatus)
                .apply(uo.getBasic()))
            .orElse(null);
    }

    public Map<Integer, DataCampOffer.Offer> getDatacampServiceOffers() {
        return this.datacampOffer != null ? this.datacampOffer.getServiceMap() : null;
    }

    public DCPipelineState addBasicVerdict(String errorCode, DataCampExplanation.Explanation.Level level) {
        return modifyBasicOffer(basiOffer -> basiOffer.toBuilder().setResolution(
            DataCampResolution.Resolution.newBuilder()
                .addBySource(DataCampResolution.Verdicts.newBuilder()
                    .addVerdict(DataCampResolution.Verdict.newBuilder()
                        .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                            .addMessages(DataCampExplanation.Explanation.newBuilder()
                                .setCode(errorCode)
                                .setLevel(level)
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build());
    }
}
