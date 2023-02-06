package ru.yandex.market.partner.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Required;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.SimpleErrorInfo;
import ru.yandex.common.framework.xml.SimpleNameableCollection;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffMessageInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.cutoff.service.CutoffMessageService;
import ru.yandex.market.core.error.ErrorInfoException;
import ru.yandex.market.core.servantlet.AbstractCoreServantlet;
import ru.yandex.market.partner.servant.DataSourceable;
import ru.yandex.market.partner.testing.model.CutoffMessage;
import ru.yandex.market.partner.testing.model.MessageDetails;

/**
 * @author Antonina Mamaeva mamton@yandex-team.ru
 * Date: 30.10.13
 * Time: 11:05
 * @deprecated перенесен на {@link ru.yandex.market.partner.mvc.controller.cutoff.PartnerCutoffController#getCutoffMessages}
 */
@Deprecated(forRemoval = true)
public class GetCutoffMessagesServantlet<Q extends ServRequest & DataSourceable>
        extends AbstractCoreServantlet<Q, ServResponse> {
    private CutoffMessageService cutoffMessageService;
    private CutoffService cutoffService;

    @Required
    public void setCutoffMessageService(final CutoffMessageService cutoffMessageService) {
        this.cutoffMessageService = cutoffMessageService;
    }

    @Required
    public void setCutoffService(final CutoffService cutoffService) {
        this.cutoffService = cutoffService;
    }

    @Override
    public void processWithParams(final Q request, final ServResponse response) {
        final long datasourceId = request.getDatasourceId();
        if (datasourceId > 0) {
            final List<CutoffMessage> res = new ArrayList<>();
            res.addAll(composeMessages(cutoffService.getCutoffsByDatasource(datasourceId)));
            res.addAll(composeMessages(cutoffService.getCpaCutoffsByDatasource(datasourceId)));

            response.addData(new SimpleNameableCollection("cutoffs", res));
        } else {
            throw new ErrorInfoException(new SimpleErrorInfo("wrong-datasource-id"));
        }
    }

    private List<CutoffMessage> composeMessages(final Map<CutoffType, CutoffInfo> cutoffsByDatasource) {
        if (cutoffsByDatasource != null) {
            // Подготовить мапу отключений.
            final Map<Long, CutoffInfo> cutoffs = new HashMap<>();
            cutoffsByDatasource.forEach((type, info) -> cutoffs.put(info.getId(), info));

            // Найти сообщения.
            final Map<Long, CutoffMessageInfo> messages =
                    cutoffMessageService.getMessagesByCutoffs(cutoffs.keySet());

            // Скомпановать результат.
            return cutoffs.values().stream()
                    .map(cutoff ->
                            new CutoffMessage(cutoff,
                                    MessageDetails.fromCutoffMessageInfo(messages.get(cutoff.getId())))
                    ).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
