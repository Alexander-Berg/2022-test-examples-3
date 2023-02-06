package ru.yandex.market.admin.ui.client.shop.testing;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import ru.yandex.market.admin.ui.client.Admin;
import ru.yandex.market.admin.ui.client.cmd.UICommand;
import ru.yandex.market.admin.ui.client.data.CellDataFieldsGroup;
import ru.yandex.market.admin.ui.client.data.DataBlock;
import ru.yandex.market.admin.ui.client.i18n.UIConstants;
import ru.yandex.market.admin.ui.client.rpc.CancellableAsyncCallback;
import ru.yandex.market.admin.ui.client.rpc.RemoteServicePool;
import ru.yandex.market.admin.ui.client.shop.ShopPage;
import ru.yandex.market.admin.ui.client.widgets.ExHyperlink;
import ru.yandex.market.admin.ui.client.widgets.ProcessingDialog;
import ru.yandex.market.admin.ui.client.widgets.data.AbstractDataControlsGroup;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsColumnInfo;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsProvider;
import ru.yandex.market.admin.ui.client.widgets.data.DataResultsTable;
import ru.yandex.market.admin.ui.client.widgets.data.LabeledDataFieldContainer;
import ru.yandex.market.admin.ui.client.widgets.data.TextDataField;
import ru.yandex.market.admin.ui.model.testing.UIShopTestingHistoryItem;
import ru.yandex.market.admin.ui.model.testing.UITestedShop;


/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class ShopTestingFieldsGroup extends AbstractDataControlsGroup {

    private static final String CLEAR_SHOP_FATAL_CANCELLED = "shopTesting@clearShopFatalCancelled";
    private VerticalPanel buttonsPanel;
    private Button approveButton;
    private ShopPage shopPage;
    /**
     * История изменения магазинов.
     */
    private DataResultsTable historyTable;
    /**
     * Результат загрузки истории магазина.
     */
    private Object historyLoadResult;

    @SuppressWarnings("checkstyle:magicNumber")
    public ShopTestingFieldsGroup(ShopPage shopPage,
                                  ShopTestingDataBlock shopDataBlock) {
        super(UIConstants.constants.groupShopTesting(), "img/pinion.gif",
                shopDataBlock);

        buttonsPanel = new VerticalPanel();
        buttonsPanel.setWidth("100%");
        buttonsPanel.setVisible(false);
        add(buttonsPanel);

        historyTable = new DataResultsTable();
        historyTable.setDataViewBlockSize(5);
        historyTable.enableSorting(false);
        historyTable.enableHilighting(false);

        ExHyperlink historyLink = new ExHyperlink(UIConstants.constants
                .groupShopTestingViewHistory(), new UICommand() {
            protected void doCommand() {
                UITestedShop testedShop = (UITestedShop) getFieldsGroup()
                        .getDataBlock().getData();
                if (testedShop != null) {
                    Long shopId = testedShop.getLongField(UITestedShop.ID);
                    String shopName = testedShop
                            .getStringField(UITestedShop.NAME);

                    ShopTestingHistoryDialog dialog = new ShopTestingHistoryDialog();
                    dialog.setShop(shopId.longValue(), shopName, historyLoadResult);
                    dialog.show();
                }
            }
        });
        historyTable.setWidth("100%");
        buttonsPanel.add(historyTable);
        buttonsPanel.add(historyLink);
        buttonsPanel.setCellWidth(historyLink, "100%");

        approveButton = new Button(UIConstants.constants
                .shopsTestingActionsSetReady(), new ClickListener() {
            public void onClick(Widget sender) {
                approveShop();
            }
        });
        approveButton.setVisible(false);
        buttonsPanel.add(approveButton);

        this.shopPage = shopPage;
    }

    protected void onCreateSection(CellDataFieldsGroup fg, int index) {
        fg.newLine(false);
        fg.addField(UITestedShop.STATUS, new LabeledDataFieldContainer(
                new TextDataField(),
                UIConstants.constants.shopsTestingStatus(), "80px", "400px"));

        fg.newLine(false);
        fg.addField(UITestedShop.TEST_LOADING, new LabeledDataFieldContainer(
                new TextDataField(), UIConstants.constants
                .shopsTestingTestLoading(), "80px", "80px"));
        fg.addField(UITestedShop.TEST_QUALITY, new LabeledDataFieldContainer(
                new TextDataField(), UIConstants.constants
                .shopsTestingTestQuality(), "80px", "80px"));
        fg.addField(UITestedShop.TEST_CLONING, new LabeledDataFieldContainer(
                new TextDataField(), UIConstants.constants
                .shopsTestingTestCloning(), "80px", "80px"));

        fg.finishLine();
    }

    protected void onDataChanged() {
        super.onDataChanged();

        DataBlock dataBlock = getFieldsGroup().getDataBlock();
        buttonsPanel.setVisible((dataBlock.hasData() && !dataBlock.isEmpty()));

        final UITestedShop shop = (UITestedShop) dataBlock.getData();

        RemoteServicePool.getSecurityService().isActionAllowed(
                CLEAR_SHOP_FATAL_CANCELLED,
                new CancellableAsyncCallback(UIConstants.constants.securityIsActionAllowedOperationName()) {
                    public void onResult(Object result) {
                        boolean actionAllowed = (Boolean) result;
                        if (actionAllowed && shop != null) {
                            showApproveButton(shop);
                        } else {
                            approveButton.setVisible(false);
                        }
                    }
                });
    }

    private void approveShop() {
        UITestedShop shop = (UITestedShop) getFieldsGroup().getDataBlock()
                .getData();
        if (shop == null) {
            throw new RuntimeException("No data in datablock");
        }
        final Long shopId = shop.getLongField(UITestedShop.ID);
        if (shopId == null) {
            throw new RuntimeException("No shopId in datablock");
        }

        final ProcessingDialog waitDialog = new ProcessingDialog();
        waitDialog.setLabel(UIConstants.constants
                .shopsTestingSetReadyOperationName());
        waitDialog.show();

        RemoteServicePool.getDatasourceTestingService()
                .clearShopFatalCancelled(
                        shopId.longValue(),
                        new CancellableAsyncCallback(UIConstants.constants
                                .shopsTestingSetReadyOperationName()) {
                            public void onResult(Object result) {
                                Admin.getInstance().getUI().getShopPage()
                                        .goToShop(shopId.longValue());
                            }

                            public void onFinished() {
                                waitDialog.hide();
                                shopPage.refreshShopTestingData();
                            }
                        });
    }

    /**
     * Loads history for a shop.
     *
     * @param shopId shop id.
     */
    public void loadHistory(final long shopId) {
        historyTable.clear();
        historyLoadResult = null;
        final DataResultsProvider dataProvider =
                new DataResultsProvider(
                        historyTable,
                        UIConstants.constants.shopsTestingHistoryOperationName()
                ) {
                    protected void fillColumnsInfo(List columns) {
                        columns.add(
                                new DataResultsColumnInfo(
                                        UIShopTestingHistoryItem.DATE,
                                        UIConstants.constants.shopsTestingHistoryDate(),
                                        "15%"
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

                        final List newResult = new ArrayList();
                        final List oldResult = (List) result;

                        for (int i = 0; i < Math.min(2, oldResult.size()); i++) {
                            newResult.add(oldResult.get(i));
                        }

                        historyLoadResult = result;
                        super.onResult(newResult);
                    }
                };

        RemoteServicePool.getDatasourceTestingService().getShopTestingHistory(
                shopId,
                dataProvider
        );
    }

    /**
     * В случае, если модерация магазина находится в состоянии TestingStatus#FATAL_CANCELED либо перебрал количество
     * попыток модераци для него будет отрисована кнопка, разрешающая дальнейшую модерацию.
     *
     * @param shop магазин находящийся на модерации (см. SHOPS_WEB.V_SHOPS_IN_TESTING)
     */
    private void showApproveButton(UITestedShop shop) {
        if (fatalCancelled(shop)) {
            approveButton.setVisible(true);
        } else {
            long shopId = shop.getLongField(UITestedShop.ID);
            RemoteServicePool.getDatasourceTestingService().moderationAttemptsExceeded(
                    shopId,
                    new AsyncCallback<Boolean>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            approveButton.setVisible(false);
                        }

                        @Override
                        public void onSuccess(Boolean result) {
                            approveButton.setVisible(result);
                        }
                    });
        }
    }

    private boolean fatalCancelled(UITestedShop shop) {
        Long fatalCancelled = shop.getLongField(UITestedShop.FATAL_CANCELLED);
        return (fatalCancelled != null && fatalCancelled.longValue() == 1);
    }
}
