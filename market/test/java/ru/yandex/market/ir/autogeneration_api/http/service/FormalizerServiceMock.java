package ru.yandex.market.ir.autogeneration_api.http.service;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.FormalizerService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FormalizerServiceMock implements FormalizerService {

    public static final String ONE_SIZED = "one_sized";
    /**
     * для сверки правильного сохранения ответов кладем в реквест айди тикета, например в yml-парам
     */
    Map<String, Formalizer.FormalizerResponse> specialResponsesByClassifierMagicId;

    public FormalizerServiceMock() {
        this.specialResponsesByClassifierMagicId = new HashMap<>();
        this.specialResponsesByClassifierMagicId.put(ONE_SIZED, Formalizer.FormalizerResponse.newBuilder()
            .addOffer(Formalizer.FormalizedOffer.newBuilder().getDefaultInstanceForType())
            .build());
    }

    @Override
    public Formalizer.FormalizerResponse formalize(Formalizer.FormalizerRequest formalizerRequest) {
        String specialKey = extractControlStringFromClassifierMagicIdInRequest(formalizerRequest);
        if (specialResponsesByClassifierMagicId.containsKey(specialKey)) {
            return specialResponsesByClassifierMagicId.get(specialKey);
        }
        return Formalizer.FormalizerResponse.newBuilder()
            .addAllOffer(
                formalizerRequest.getOfferList().stream()
                    .map(offer -> Formalizer.FormalizedOffer.newBuilder()
                        .addPosition(
                            FormalizerParam.FormalizedParamPosition.newBuilder()
                                .setOptionId(Integer.parseInt(offer.getYmlParam(0).getValue()))
                                .build()
                        ).build()
                    )
                    .collect(Collectors.toList())
            )
            .build();
    }

    public void addSpecialResponse(String classifierMagicId, Formalizer.FormalizerResponse response) {
        specialResponsesByClassifierMagicId.put(classifierMagicId, response);
    }

    private String extractControlStringFromClassifierMagicIdInRequest(Formalizer.FormalizerRequest formalizerRequest) {
        return formalizerRequest.getOfferList().get(0).getClassifierMagicId();
    }

    @Override
    public Formalizer.FormalizerResponse formalizeForAdviser(Formalizer.FormalizerRequest formalizerRequest) {
        return null;
    }

    @Override
    public Formalizer.FormalizerResponse formalizeForSearch(Formalizer.FormalizerRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Formalizer.FormalizerResponse applyRules(Formalizer.RulesRequest rulesRequest) {
        return null;
    }

    @Override
    public Formalizer.FormalizedOffer formalizeSingleOffer(Formalizer.Offer offer) {
        return null;
    }

    @Override
    public Formalizer.FormalizerResponse preFormalize(Formalizer.FormalizerRequest formalizerRequest) {
        return null;
    }

    @Override
    public Formalizer.ReloadResponse reload(Formalizer.ReloadRequest reloadRequest) {
        return null;
    }

    @Override
    public MonitoringResult ping() {
        return null;
    }

    @Override
    public MonitoringResult monitoring() {
        return null;
    }
}
