package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SingleVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

/**
 * @author dmserebr
 * @date 23/10/2020
 */
public class VerdictTestUtil {
    private VerdictTestUtil() {
    }

    public static SskuVerdictResult createOkSskuVerdictResult(ShopSkuKey shopSkuKey,
                                                              Instant mdmVersionId,
                                                              VerdictFeature... features) {
        return createOkSskuVerdictResult(shopSkuKey, mdmVersionId, null, features);
    }

    public static SskuVerdictResult createOkSskuVerdictResult(ShopSkuKey shopSkuKey,
                                                              Instant mdmVersionId,
                                                              Long contentVersionId,
                                                              VerdictFeature... features) {
        SskuVerdictResult verdictResult = new SskuVerdictResult();
        verdictResult.setKey(shopSkuKey);
        verdictResult.setValid(true);
        verdictResult.setKey(shopSkuKey);
        Map<VerdictFeature, SingleVerdictResult> singleVerdictResults = new LinkedHashMap<>();
        for (VerdictFeature feature : features) {
            singleVerdictResults.put(feature, VerdictGeneratorHelper.createOkVerdict(feature));
        }
        verdictResult.setSingleVerdictResults(singleVerdictResults);
        verdictResult.setMdmVersionTs(mdmVersionId);
        verdictResult.setUpdatedTs(mdmVersionId);
        if (contentVersionId != null) {
            verdictResult.setContentVersionId(contentVersionId);
        }
        return verdictResult;
    }

    public static SskuVerdictResult createForbiddingSskuVerdictResult(
        ShopSkuKey shopSkuKey,
        Instant mdmVersionId,
        Map<VerdictFeature, SingleVerdictResult> singleVerdictResults,
        Long contentVersionId
    ) {
        SskuVerdictResult verdictResult = new SskuVerdictResult();
        verdictResult.setKey(shopSkuKey);
        verdictResult.setValid(false);
        verdictResult.setKey(shopSkuKey);
        verdictResult.setSingleVerdictResults(singleVerdictResults);
        verdictResult.setMdmVersionTs(mdmVersionId);
        verdictResult.setUpdatedTs(mdmVersionId);
        if (contentVersionId != null) {
            verdictResult.setContentVersionId(contentVersionId);
        }
        return verdictResult;
    }

    public static SskuVerdictResult createForbiddingSskuVerdictResult(ShopSkuKey shopSkuKey,
                                                                      Instant mdmVersionId,
                                                                      List<VerdictFeature> forbiddenFeatures,
                                                                      List<ErrorInfo> errors,
                                                                      Long contentVersionId,
                                                                      List<VerdictFeature> okFeatures) {
        Map<VerdictFeature, SingleVerdictResult> singleVerdictResults = new HashMap<>();
        forbiddenFeatures.forEach(feature -> singleVerdictResults.put(feature,
            VerdictGeneratorHelper.createForbiddingVerdict(feature, errors)));
        okFeatures.forEach(feature -> singleVerdictResults.put(feature,
            VerdictGeneratorHelper.createOkVerdict(feature)));
        return createForbiddingSskuVerdictResult(shopSkuKey, mdmVersionId, singleVerdictResults, contentVersionId);
    }

    public static SskuVerdictResult createForbiddingSskuVerdictResult(ShopSkuKey shopSkuKey,
                                                                      Instant mdmVersionId,
                                                                      VerdictFeature forbiddingFeature,
                                                                      List<ErrorInfo> errors,
                                                                      VerdictFeature... okFeatures) {
        return createForbiddingSskuVerdictResult(shopSkuKey, mdmVersionId, List.of(forbiddingFeature),
            errors, null, List.of(okFeatures));
    }

    public static SskuPartnerVerdictResult createOkSskuPartnerVerdictResult(ShopSkuKey shopSkuKey,
                                                                            Instant mdmVersionId,
                                                                            Instant partnerVersionId) {
        SskuPartnerVerdictResult verdictResult = new SskuPartnerVerdictResult();
        verdictResult.setValid(true);
        verdictResult.setKey(shopSkuKey);
        verdictResult.setSingleVerdictResults(Map.of(VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED)));
        verdictResult.setMdmVersionTs(mdmVersionId);
        verdictResult.setPartnerVersionTs(partnerVersionId);
        verdictResult.setUpdatedTs(mdmVersionId.compareTo(partnerVersionId) > 0 ? mdmVersionId : partnerVersionId);
        return verdictResult;
    }

    public static SskuPartnerVerdictResult createForbiddingSskuPartnerVerdictResult(ShopSkuKey shopSkuKey,
                                                                                    Instant mdmVersionId,
                                                                                    Instant partnerVersionId,
                                                                                    List<ErrorInfo> errors) {
        SskuPartnerVerdictResult verdictResult = new SskuPartnerVerdictResult();
        verdictResult.setValid(false);
        verdictResult.setKey(shopSkuKey);
        verdictResult.setSingleVerdictResults(Map.of(VerdictFeature.UNSPECIFIED,
            VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED, errors)));
        verdictResult.setMdmVersionTs(mdmVersionId);
        verdictResult.setPartnerVersionTs(partnerVersionId);
        verdictResult.setUpdatedTs(mdmVersionId.compareTo(partnerVersionId) > 0 ? mdmVersionId : partnerVersionId);
        return verdictResult;
    }
}
