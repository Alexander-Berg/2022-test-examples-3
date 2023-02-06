package ru.yandex.market.partner.testing;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Required;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.error.EntityNotFoundException;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.servantlet.AbstractCoreServantlet;
import ru.yandex.market.core.testing.TestingDetails;
import ru.yandex.market.core.testing.TestingInfo;
import ru.yandex.market.core.testing.model.DatasourcePremoderationInfo;
import ru.yandex.market.partner.servant.DataSourceable;

/**
 * @author mkasumov
 */
public class GetPremoderationInfoServantlet<Q extends ServRequest & DataSourceable>
        extends AbstractCoreServantlet<Q, ServResponse> {

    private ModerationService moderationService;
    private CutoffService cutoffService;

    @Required
    public void setModerationService(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @Required
    public void setCutoffService(CutoffService cutoffService) {
        this.cutoffService = cutoffService;
    }

    @Override
    public void processWithParams(Q request, ServResponse response) {
        processRequest(request, response);
    }

    protected Pair<DatasourcePremoderationInfo, TestingDetails> processRequest(Q request, ServResponse response) {
        long datasourceId = request.getDatasourceId();

        if (datasourceId <= 0) {
            throw new EntityNotFoundException(datasourceId);
        }

        Pair<DatasourcePremoderationInfo, TestingDetails> premoderationInfo = getPremoderationInfo(datasourceId);

        response.addData(premoderationInfo.first);
        response.addData(premoderationInfo.second);

        return premoderationInfo;
    }

    protected TestingDetails buildTestingDetails(DatasourcePremoderationInfo premoderationInfo) {
        final TestingInfo testingInfo = premoderationInfo.getTestingInfo();
        Collection<CutoffInfo> cutoffs = null;
        if (testingInfo != null) {
            cutoffs = new ArrayList<CutoffInfo>();
            CutoffInfo cutoff = cutoffService.getCutoff(premoderationInfo.getDatasourceId(), CutoffType.FORTESTING);
            if (cutoff != null) {
                cutoffs.add(cutoff);
            }

            final long cutoffId = testingInfo.getCutoffId();
            if (cutoffId > 0) {
                cutoff = cutoffService.getCutoff(cutoffId);
                if (cutoff != null) {
                    cutoffs.add(cutoff);
                }
            }
        }
        return new TestingDetails(testingInfo, cutoffs);
    }

    protected Pair<DatasourcePremoderationInfo, TestingDetails> getPremoderationInfo(long datasourceId) {
        final DatasourcePremoderationInfo premoderationInfo = moderationService.getPremoderationInfo(datasourceId);
        final TestingDetails testingDetails = (premoderationInfo != null) ?
                buildTestingDetails(premoderationInfo) : null;
        return new Pair<>(premoderationInfo, testingDetails);
    }

}
