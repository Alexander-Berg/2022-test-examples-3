package ru.yandex.market.core.testing;

import ru.yandex.market.core.cutoff.model.CutoffClosingModeration;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.partner.PartnerTypeAwareService;

import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_SELF_DELIVERY;

/**
 * @author zoom
 */
public class DefaultCpaCheckService implements CpaCheckService {

    private final PartnerTypeAwareService partnerTypeAwareService;
    private final FeatureService featureService;

    public DefaultCpaCheckService(PartnerTypeAwareService partnerTypeAwareService,
                                  FeatureService featureService) {
        this.partnerTypeAwareService = partnerTypeAwareService;
        this.featureService = featureService;
    }

    @Override
    public TestingType getTestingType(long shopId) {
        return featureService.getCutoffs(shopId, MARKETPLACE_SELF_DELIVERY).stream()
                .map(FeatureCutoffInfo::getFeatureCutoffType)
                .map(fct -> fct.getCutoffClosingModeration().forProgram(ShopProgram.CPA))
                .filter(CutoffClosingModeration.ForProgram::isRequired)
                .map(CutoffClosingModeration.ForProgram::moderationType)
                .reduce(TestingType.DSBS_LITE_CHECK, TestingType::merge);
    }

    /**
     * CPA-проверки могут проходить только полнотсью настроенные магазины c фича MARKETPLACE_SELF_DELIVERY.
     *
     * @param shopId - идентификатор магазина, который хочет на CPA-проверки
     * @return true - если CPA-проверки возможны, false - в противном случае
     */
    @Override
    public boolean isCheckAllowed(long shopId) {
        boolean isDsbs = partnerTypeAwareService.isMarketSelfDelivery(shopId);

        if (!isDsbs) {
            return false;
        }

        return featureService.getDescription(MARKETPLACE_SELF_DELIVERY)
                .getPrecondition()
                .evaluate(shopId)
                .canEnable();
    }

    @Override
    public boolean isSelfCheckCanBeStarted(long shopId) {
        ShopFeature feature = featureService.getFeature(shopId, MARKETPLACE_SELF_DELIVERY);
        return feature.getStatus() == ParamCheckStatus.DONT_WANT || feature.getStatus() == ParamCheckStatus.NEW;
    }
}
