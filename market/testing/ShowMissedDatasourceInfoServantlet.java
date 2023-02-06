/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 25.06.2007
 * Time: 18:10:39
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.market.partner.testing;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.core.ds.info.DatasourceInformationService;
import ru.yandex.market.core.ds.info.ShopInformation;
import ru.yandex.market.core.servantlet.AbstractCoreServantlet;
import ru.yandex.market.partner.servant.DataSourceable;

/**
 * @author ashevenkov
 */
public class ShowMissedDatasourceInfoServantlet<Q extends ServRequest & DataSourceable>
        extends AbstractCoreServantlet<Q, ServResponse> {

    private DatasourceInformationService datasourceInformationService;

    @Required
    public void setDatasourceInformationService(DatasourceInformationService datasourceInformationService) {
        this.datasourceInformationService = datasourceInformationService;
    }

    @Override
    public void processWithParams(Q request, ServResponse response) {
        final long datasourceId = request.getDatasourceId();
        if (datasourceId > 0) {
            List<ShopInformation> list = datasourceInformationService.getMissedDatasourceInfo(datasourceId);
            if (list != null) {
                response.addData(list);
            }
        }
    }
}
