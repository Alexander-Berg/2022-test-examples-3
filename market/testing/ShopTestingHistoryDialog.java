/**
 * Date: 17.07.2007
 * Time: 17:11:26
 */

package ru.yandex.market.admin.ui.client.shop.testing;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import ru.yandex.market.admin.ui.client.i18n.UIConstants;
import ru.yandex.market.admin.ui.client.rpc.RemoteServicePool;
import ru.yandex.market.admin.ui.client.widgets.ExDialogBox;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsColumnInfo;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsDateColumnInfo;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsProvider;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsTable;
import ru.yandex.market.admin.ui.model.testing.UIShopTestingHistoryItem;

/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class ShopTestingHistoryDialog extends ExDialogBox {

    private DataResultsTable dataResultsTable;

    @SuppressWarnings("checkstyle:magicNumber")
    public ShopTestingHistoryDialog() {
        super(false);
        setText(UIConstants.constants.shopsTestingHistory().replaceAll("###", ""));

        VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.setWidth("700px");
        contentPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        dataResultsTable = new DataResultsTable();
        dataResultsTable.setDataViewBlockSize(5);
        dataResultsTable.enableSorting(false);
        dataResultsTable.enableHilighting(false);
        contentPanel.add(dataResultsTable);
        contentPanel.setCellHeight(dataResultsTable, "100%");

        Button closeButton = new Button();
        closeButton.setText(UIConstants.constants.close());
        closeButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                close();
            }
        });
        contentPanel.add(closeButton);

        setWidget(contentPanel);
    }

    public void setShop(final long shopId, String name) {
        setText(UIConstants.constants.shopsTestingHistory().replaceAll("###", name));
        final DataResultsProvider dataProvider = createDataCallback(shopId);

        RemoteServicePool.getDatasourceTestingService().getShopTestingHistory(
                shopId,
                dataProvider
        );
    }

    /**
     * Creates callback.
     *
     * @param shopId id of the shop.
     * @return data load callback.
     */
    private DataResultsProvider createDataCallback(final long shopId) {
        final DataResultsProvider dataProvider =
                new DataResultsProvider(dataResultsTable, UIConstants.constants.shopsTestingHistoryOperationName()) {
                    protected void fillColumnsInfo(List columns) {
                        columns.add(
                                new DataResultsDateColumnInfo(
                                        UIShopTestingHistoryItem.DATE,
                                        UIConstants.constants.shopsTestingHistoryDate(),
                                        "15%",
                                        DataResultsDateColumnInfo.DATE_TIME_FORMAT
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UIShopTestingHistoryItem.CHANGES,
                                        UIConstants.constants.shopsTestingHistoryChanges(),
                                        "40%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UIShopTestingHistoryItem.ACTOR_NAME,
                                        UIConstants.constants.shopsTestingHistoryActor(),
                                        "15%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UIShopTestingHistoryItem.COMMENT,
                                        UIConstants.constants.shopsTestingHistoryComment(),
                                        "30%"
                                )
                        );
                    }

                    public void onResult(Object result) {
                        if (result == null) {
                            throw new RuntimeException(
                                    UIConstants.constants.shopsTestingHistoryNoData()
                                            .replaceAll(
                                                    "#SHOP#", String.valueOf(shopId)
                                            )
                            );
                        }
                        super.onResult(result);
                        center();
                    }

                    public void onFailed(Throwable caught) {
                        close();
                        super.onFailed(caught);
                    }
                };
        return dataProvider;
    }

    public void setShop(final long shopId, String name, Object history) {
        setText(UIConstants.constants.shopsTestingHistory().replaceAll("###", name));
        final DataResultsProvider dataProvider = createDataCallback(shopId);
        dataProvider.onResult(history);
    }

    public void close() {
        DataResultsProvider provider = dataResultsTable.getActiveProvider();
        if (provider != null) {
            provider.cancel();
        }
        hide();
    }
}
