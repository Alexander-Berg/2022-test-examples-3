package ru.yandex.direct.web.entity.inventori.service;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.validation.defect.ids.DateDefectIds;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.defect.params.DateDefectParams;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.validation.model.WebDefect;

import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_GREATER_THAN_MIN;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_NULL;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.web.core.entity.inventori.validation.CampaignDefectIds.CampaignTypeDefects.INVALID_CAMPAIGN_TYPE;
import static ru.yandex.direct.web.core.entity.inventori.validation.CampaignDefectIds.StrategyDefects.INVALID_CAMPAIGN_STRATEGY;
import static ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds.Gen.CONTAINS_KEYWORD_ADGROUPS;
import static ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds.Gen.NO_SUITABLE_ADGROUPS;
import static ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds.Number.LOW_REACH;

public class InventoriTestWebDefects {

    public static WebDefect webLowReach(String request, long value) {
        return new WebDefect()
                .withPath("")
                .withCode(LOW_REACH.getCode())
                .withValue(request)
                .withText("Совокупный охват (" + value + ") очень мал для расчета прогноза CPM. Расширьте "
                        + "таргетинги в условиях показа")
                .withDescription("Совокупный охват (" + value + ") очень мал для расчета прогноза CPM. Расширьте "
                        + "таргетинги в условиях показа")
                .withParams(new NumberDefectParams().withMin(value));
    }

    public static WebDefect webCampaignNotFound(String path, String value) {
        return new WebDefect()
                .withPath(path)
                .withText("Campaign " + value + " not found")
                .withDescription("Campaign " + value + " not found")
                .withCode(CAMPAIGN_NOT_FOUND.getCode())
                .withValue(value);
    }

    public static WebDefect webInvalidId(String path, String value) {
        return new WebDefect()
                .withPath(path)
                .withText("Invalid value")
                .withDescription("Invalid value")
                .withCode(MUST_BE_VALID_ID.getCode())
                .withValue(value);
    }

    public static WebDefect webInvalidCampaignType(String path, Long campaignId, CampaignType campaignType) {
        return new WebDefect()
                .withPath(path)
                .withCode(INVALID_CAMPAIGN_TYPE.getCode())
                .withText("Кампания с id " + campaignId + " имеет невалидный тип: " + campaignType)
                .withValue(campaignId.toString())
                .withParams(campaignType)
                .withDescription("Кампания с id " + campaignId + " имеет невалидный тип: " + campaignType);
    }

    public static WebDefect webInvalidCampaignStrategy(String path, Long campaignId, StrategyName strategyName) {
        return new WebDefect()
                .withPath(path)
                .withCode(INVALID_CAMPAIGN_STRATEGY.getCode())
                .withText("Кампания с id " + campaignId + " имеет невалидный тип стратегии: " + strategyName)
                .withValue(campaignId.toString())
                .withParams(strategyName)
                .withDescription("Кампания с id " + campaignId + " имеет невалидный тип стратегии: " + strategyName);
    }

    public static WebDefect webMustBeInCollection(String path, String value) {
        return new WebDefect()
                .withPath(path)
                .withText("Invalid value")
                .withDescription("Invalid value")
                .withCode(MUST_BE_IN_COLLECTION.getCode())
                .withValue(value);
    }

    public static WebDefect webCannotBeNull(String path) {
        return new WebDefect()
                .withPath(path)
                .withText("Please fill in the field")
                .withDescription("Please fill in the field")
                .withCode(CANNOT_BE_NULL.getCode());
    }

    public static WebDefect webMustBeGreaterThanMin(String path, String value, Object params) {
        return new WebDefect()
                .withPath(path)
                .withText("The entered value is less than the minimum")
                .withDescription("The entered value is less than the minimum")
                .withCode(MUST_BE_GREATER_THAN_MIN.getCode())
                .withParams(params)
                .withValue(value);
    }

    public static WebDefect webMustBeGreaterThatOrEqualToMin(String path, String value, DateDefectParams params) {
        return new WebDefect()
                .withPath(path)
                .withText("Invalid value")
                .withDescription("Invalid value")
                .withCode(DateDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN.getCode())
                .withParams(params)
                .withValue(value);
    }

    public static WebDefect webMustBeGreaterThatOrEqualToMin(String path, String value, NumberDefectParams params) {
        return new WebDefect()
                .withPath(path)
                .withText("The entered value is less than the minimum")
                .withDescription("The entered value is less than the minimum")
                .withCode(NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN.getCode())
                .withParams(params)
                .withValue(value);
    }

    public static WebDefect webMustBeNull(String path, String value) {
        return new WebDefect()
                .withPath(path)
                .withText("This field must be empty")
                .withDescription("This field must be empty")
                .withCode(MUST_BE_NULL.getCode())
                .withValue(value);
    }

    public static WebDefect webNoSuitableAdGroups(String value) {
        return new WebDefect()
                .withPath("")
                .withText("Invalid value")
                .withDescription("Invalid value")
                .withCode(NO_SUITABLE_ADGROUPS.getCode())
                .withValue(value);
    }

    public static WebDefect webContainsKeywordAdGroups(CpmForecastRequest request) {
        return new WebDefect()
                .withPath("")
                .withCode(CONTAINS_KEYWORD_ADGROUPS.getCode())
                .withText("Расчет не учитывает группы с типом условия показа \"Фразы\"")
                .withValue(request.toString())
                .withDescription("Расчет не учитывает группы с типом условия показа \"Фразы\"");
    }
}
