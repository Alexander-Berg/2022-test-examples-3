package ru.yandex.market.mboc.common.services.mstat.yt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ru.yandex.market.ir.yt.util.tables.YtClientWrapper;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.mstat.ExportContext;
import ru.yandex.market.mboc.common.services.mstat.MstatOfferState;
import ru.yandex.market.mboc.common.services.mstat.SnapShotContext;
import ru.yandex.market.yt.util.table.YtTableService;
import ru.yandex.market.yt.util.table.model.YtTableModel;

/**
 * @author apluhin
 * @created 2/20/21
 */
public class StubLoader extends BaseYtMstatTableLoader {

    public static String DESTINATION_EXPORT_TABLE = "//test/test_destination_table";
    public static String DESTINATION_EXPORT_TMP_TABLE = "//test/tmp_table";

    public StubLoader(UnstableInit<YtClientWrapper> ytClientWrapper, UnstableInit<YtTableService> ytTableService,
                      YtTableModel tableModel) {
        super(ytClientWrapper, ytTableService, tableModel, "BaseYtMstatTableLoader");
    }

    @Override
    protected void convertEntityToYt(Collection<Offer> offer,
                           ExportContext context,
                           Consumer<Map<String, Object>> convertedConsumer) {
        return;
    }

    //раскладываем по сервисным офферам
    @Override
    protected List<Map<String, Object>> extractKeys(MstatOfferState state) {
        return state.getServiceOffers().stream().map(it -> Map.of(
            "key1", state.getOfferId(),
            "key2", (Object) it.getSupplierId()
        )).collect(Collectors.toList());
    }

    @Override
    protected int getInsertYtRowsTransactionLimit() {
        return DEFAULT_YT_ROWS_TRANSACTION_LIMIT;
    }

    @Override
    public SnapShotContext prepareContext() {
        return new SnapShotContext("baseName", "test sql", DESTINATION_EXPORT_TABLE,
            DESTINATION_EXPORT_TMP_TABLE, "//path/sourceTableName");
    }
}
