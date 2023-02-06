package ru.yandex.market.admin.ui.client.shop.testing;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import ru.yandex.market.admin.ui.client.AbstractPage;
import ru.yandex.market.admin.ui.client.Admin;
import ru.yandex.market.admin.ui.client.PageTitle;
import ru.yandex.market.admin.ui.client.dic.ShopTestingTypesFilterDictionary;
import ru.yandex.market.admin.ui.client.i18n.UIConstants;
import ru.yandex.market.admin.ui.client.navigation.HistoryToken;
import ru.yandex.market.admin.ui.client.navigation.HistoryTokens;
import ru.yandex.market.admin.ui.client.rpc.CancellableAsyncCallback;
import ru.yandex.market.admin.ui.client.rpc.RemoteServicePool;
import ru.yandex.market.admin.ui.client.widgets.ControlsGroup;
import ru.yandex.market.admin.ui.client.widgets.ExButton;
import ru.yandex.market.admin.ui.client.widgets.MessageBox;
import ru.yandex.market.admin.ui.client.widgets.ProcessingDialog;
import ru.yandex.market.admin.ui.client.widgets.data.CustomWidgetColumnInfo;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsColumnInfo;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsTable;
import ru.yandex.market.admin.ui.client.widgets.data.DeferredDataResultsProvider;
import ru.yandex.market.admin.ui.client.widgets.data.ListBoxDataField;
import ru.yandex.market.admin.ui.client.widgets.data.TextDataField;
import ru.yandex.market.admin.ui.model.SerializableID;
import ru.yandex.market.admin.ui.model.StringID;
import ru.yandex.market.admin.ui.model.testing.UIShopTestingTypeFilter;
import ru.yandex.market.admin.ui.model.testing.UITestedShop;
import ru.yandex.market.admin.ui.service.SortOrder;

/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class ShopsTestingPage extends AbstractPage {

    private static final SerializableID ACTIONS_COLUMN_ID = new StringID("ACTIONS_COLUMN_ID");

    private static final String TOKEN_PARAM_SHOP_TESTING_TYPE_FILTER = "type";
    private static final String TOKEN_PARAM_SHOP_IDS_FILTER = "shop_ids";

    private VerticalPanel container;
    private DataResultsTable dataResultsTable;
    private ListBoxDataField shopTestingTypeFilterField;
    private TextDataField shopIdsFilterField;
    private UIShopTestingTypeFilter shopTestingFilter;
    private long[] shopIdsFilter;
    private ShopTestingHistoryDialog historyDialog;

    @SuppressWarnings("checkstyle:magicNumber")
    public ShopsTestingPage() {
        container = new VerticalPanel();
        initWidget(container);

        setWidth("100%");
        setHeight("");

        container.add(new PageTitle(UIConstants.constants.shopsTesting()));
        setWindowTitle(UIConstants.constants.shopsTesting());

        ControlsGroup filtersGroup = new ControlsGroup(UIConstants.constants.filters());
        container.add(filtersGroup);

        Grid filtersPanel = new Grid(1, 8);
        filtersPanel.setCellPadding(4);
        filtersGroup.add(filtersPanel);
        HTMLTable.CellFormatter cf = filtersPanel.getCellFormatter();
        int column = 0;

        shopTestingTypeFilterField = new ListBoxDataField();
        shopTestingTypeFilterField.setDictionary(Admin.getDictionary(ShopTestingTypesFilterDictionary.TYPE));
        shopTestingTypeFilterField.edit();
        shopTestingTypeFilterField.setWidth("200px");
        filtersPanel.setText(0, column++, UIConstants.constants.shopTestingFilterNameTestingType());
        filtersPanel.setWidget(0, column++, shopTestingTypeFilterField);
        cf.setWidth(0, column++, "10px");

        shopIdsFilterField = new TextDataField();
        shopIdsFilterField.edit();
        shopIdsFilterField.setWidth("300px");
        filtersPanel.setText(0, column++, UIConstants.constants.shopTestingFilterNameShopIds());
        filtersPanel.setWidget(0, column++, shopIdsFilterField);
        cf.setWidth(0, column++, "10px");

        Button applyButton = new Button(
                UIConstants.constants.apply(),
                (ClickListener) sender -> {
                    HistoryToken token = new HistoryToken(
                            HistoryTokens.SHOPS_TESTING_PAGE,
                            new String[]{
                                    TOKEN_PARAM_SHOP_TESTING_TYPE_FILTER,
                                    shopTestingTypeFilterField.getStringValue(),
                                    TOKEN_PARAM_SHOP_IDS_FILTER,
                                    shopIdsFilterField.getStringValue()
                            });
                    Admin.getInstance().getNavigation().goTo(token);
                });
        filtersPanel.setWidget(0, column++, applyButton);

        Button resetButton = new Button(
                UIConstants.constants.shopTestingResetFilters(),
                (ClickListener) sender -> resetFilters());
        filtersPanel.setWidget(0, column++, resetButton);

        dataResultsTable = new DataResultsTable();
        container.add(dataResultsTable);

        addHistoryToken(HistoryTokens.SHOPS_TESTING_PAGE);
    }

    public static void setReadableFieldsForTestedShop(UITestedShop shop) {
        final Long one = 1L;
        String status = "";
        if (one.equals(shop.getLongField(UITestedShop.APPROVRED))) {
            status = UIConstants.constants.shopsTestingStatusApproved();
        } else if (one.equals(shop.getLongField(UITestedShop.READY))) {
            status = UIConstants.constants.shopsTestingStatusReady();
        } else if (one.equals(shop.getLongField(UITestedShop.FATAL_CANCELLED))) {
            status = UIConstants.constants.shopsTestingStatusFinallyCancelled();
        } else if (one.equals(shop.getLongField(UITestedShop.CANCELLED))) {
            status = UIConstants.constants.shopsTestingStatusCancelled();
        } else if (one.equals(shop.getLongField(UITestedShop.IN_PROGRESS))) {
            status = UIConstants.constants.shopsTestingStatusInProgress();
        } else {
            status = UIConstants.constants.shopsTestingStatusShouldBeTested();
        }
        shop.setField(UITestedShop.STATUS, status);

        final String required = UIConstants.constants.shopsTestingTestRequired();
        final String passed = UIConstants.constants.shopsTestingTestPassed();
        shop.setField(UITestedShop.TEST_LOADING,
                (one.equals(shop.getLongField(UITestedShop.TEST_LOADING))) ? required : passed);
        shop.setField(UITestedShop.TEST_QUALITY,
                (one.equals(shop.getLongField(UITestedShop.TEST_QUALITY))) ? required : passed);
        shop.setField(UITestedShop.TEST_CLONING,
                (one.equals(shop.getLongField(UITestedShop.TEST_CLONING))) ? required : passed);
    }

    public void onActivate(HistoryToken historyToken) {
        super.onActivate(historyToken);

        String shopNewbieType = historyToken.getStringParam(TOKEN_PARAM_SHOP_TESTING_TYPE_FILTER);
        String shopIds = historyToken.getStringParam(TOKEN_PARAM_SHOP_IDS_FILTER);

        try {
            shopTestingTypeFilterField.setValue(new UIShopTestingTypeFilter(shopNewbieType));
        } catch (Exception e) {
            shopTestingTypeFilterField.setValue(UIShopTestingTypeFilter.ALL);
        }
        shopIdsFilterField.setValue(shopIds);

        applyFilters();
    }

    private void refresh() {
        requestData(null, null);
    }

    private void requestData(final SortOrder sortOrder, final SerializableID sortColumn) {
        DeferredDataResultsProvider provider =
                new DeferredDataResultsProvider(dataResultsTable, UIConstants.constants.shopsTestingOperationName()) {
                    public void loadMoreData(int fromIndex, int toIndex) {
                        RemoteServicePool.getDatasourceTestingService().getShopsInTesting(
                                shopTestingFilter, shopIdsFilter,
                                sortColumn, sortOrder, fromIndex, toIndex, this);
                    }

                    public void onResult(Object result) {
                        List data = (List) result;
                        for (Object aData : data) {
                            setReadableFieldsForTestedShop((UITestedShop) aData);
                        }

                        super.onResult(result);

                        dataResultsTable.enableHilighting(true);
                        dataResultsTable.enableSorting(true);

                        dataResultsTable.setSorting(sortColumn, sortOrder);
                    }

                    protected void fillColumnsInfo(List columns) {
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.ID,
                                        UIConstants.constants.shopsTestingId(),
                                        "5%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.NAME,
                                        UIConstants.constants.shopsTestingName(),
                                        "25%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.STATUS,
                                        UIConstants.constants.shopsTestingStatus(),
                                        "40%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.TEST_LOADING,
                                        UIConstants.constants.shopsTestingTestLoading(),
                                        "10%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.TEST_QUALITY,
                                        UIConstants.constants.shopsTestingTestQuality(),
                                        "10%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.TEST_CLONING,
                                        UIConstants.constants.shopsTestingTestCloning(),
                                        "10%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.DURATION,
                                        UIConstants.constants.shopsTestingDuration(),
                                        "5%"
                                )
                        );
                        columns.add(
                                new DataResultsColumnInfo(
                                        UITestedShop.ITERATION,
                                        UIConstants.constants.shopsTestingIteration(),
                                        "5%"
                                )
                        );
                        columns.add(
                                new CustomWidgetColumnInfo(
                                        ACTIONS_COLUMN_ID,
                                        UIConstants.constants.shopsTestingActions(),
                                        "10%", uiDataModel -> {
                                    final UITestedShop testedShop = (UITestedShop) uiDataModel;
                                    Long fatalCancelled = testedShop.getLongField(UITestedShop.FATAL_CANCELLED);
                                    Widget buttons = null;
                                    if (fatalCancelled != null && fatalCancelled != 0) {
                                        buttons = new ExButton(
                                                UIConstants.constants.shopsTestingActionsSetReady(),
                                                sender -> setShopReadyForTesting(dataResultsTable, testedShop));
                                    }
                                    return buttons;
                                })
                        );
                    }

                    public void onSortOrderChange(int columnIndex, SerializableID field, SortOrder sortOrder) {
                        requestData(sortOrder, field);
                    }

                    public void onDataRowClick(int dataRowIndex, int columnIndex, Object data) {
                        if (data == null) {
                            throw new RuntimeException(UIConstants.constants.errorInvalidRemoteData());
                        }

                        UITestedShop shop = (UITestedShop) data;
                        Long shopId = shop.getLongField(UITestedShop.ID);
                        if (shopId == null) {
                            throw new RuntimeException(UIConstants.constants.errorInvalidRemoteData());
                        }

                        if (historyDialog == null) {
                            historyDialog = new ShopTestingHistoryDialog();
                        }
                        historyDialog.setShop(shopId, shop.getStringField(UITestedShop.NAME));
                        historyDialog.show();
                    }
                };

        dataResultsTable.setResults(provider);
    }

    private void setShopReadyForTesting(final DataResultsTable dataResultsTable, final UITestedShop testedShop) {
        Long shopId = testedShop.getLongField(UITestedShop.ID);
        if (shopId == null) {
            return;
        }

        final ProcessingDialog dialog =
                new ProcessingDialog(UIConstants.constants.pleaseWait(), UIConstants.constants.receiveingData());
        dialog.show();

        RemoteServicePool.getDatasourceTestingService().clearShopFatalCancelled(
                shopId,
                new CancellableAsyncCallback(UIConstants.constants.shopsTestingSetReadyOperationName()) {
                    public void onResult(Object result) {
                        UITestedShop refreshedTestedShop = (UITestedShop) result;
                        setReadableFieldsForTestedShop(refreshedTestedShop);
                        List data = dataResultsTable.getResults().getData();
                        int index = data.indexOf(testedShop);
                        data.set(index, refreshedTestedShop);
                        dataResultsTable.refreshPage();
                    }

                    public void onFinished() {
                        dialog.hide();
                    }
                });
    }

    private void applyFilters() {
        UIShopTestingTypeFilter newShopTestingFilter = (UIShopTestingTypeFilter) shopTestingTypeFilterField.getValue();

        String shopIdsList = shopIdsFilterField.getStringValue();
        long[] newIdsFilter;
        if (shopIdsList == null || shopIdsList.trim().length() == 0) {
            newIdsFilter = null;

        } else {
            String[] ids = shopIdsList.split(",");
            newIdsFilter = new long[ids.length];
            for (int i = 0; i < ids.length; i++) {
                try {
                    newIdsFilter[i] = Long.parseLong(ids[i]);
                } catch (NumberFormatException e) {
                    new MessageBox(
                            UIConstants.constants.error(),
                            UIConstants.constants.shopTestingInvalidShopId() + ids[i],
                            MessageBox.ICON_WARNING
                    ).show();
                    return;
                }
            }
        }

        shopTestingFilter = newShopTestingFilter;
        shopIdsFilter = newIdsFilter;
        refresh();
    }

    private void resetFilters() {
        shopTestingTypeFilterField.setValue(UIShopTestingTypeFilter.ALL);
        shopIdsFilterField.setValue("");
    }

    public void refresh(UIShopTestingTypeFilter shopTestingFilter, long[] idsFilter) {
        StringBuilder ids = new StringBuilder();
        if (idsFilter != null) {
            for (long anIdsFilter : idsFilter) {
                ids.append(anIdsFilter).append(",");
            }
            if (ids.toString().endsWith(",")) {
                ids = new StringBuilder(ids.substring(0, ids.length() - 1));
            }
        }

        HistoryToken token = new HistoryToken(
                HistoryTokens.SHOPS_TESTING_PAGE,
                new String[]{TOKEN_PARAM_SHOP_TESTING_TYPE_FILTER, shopTestingFilter.getStringId(),
                        TOKEN_PARAM_SHOP_IDS_FILTER, ids.toString()});
        Admin.getInstance().getNavigation().goTo(token);
    }
}
