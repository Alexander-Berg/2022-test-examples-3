import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getLogisticGroup = getGroupGetter('common.sidebar.unified:group.logistics');

// links
export const fulfillmentSupply = getLinkItem(
    'common.sidebar.unified:supply',
    'market-partner:html:fulfillment-supply:get',
);
export const shipments = getLinkItem('common.sidebar.unified:shipments', 'market-partner:html:shipments:get');
export const warehouses = getLinkItem(
    'common.sidebar.unified:warehouses',
    'market-partner:html:platform-supplier-warehouse-list:get',
);
export const fulfillmentSupplyOnDemand = getLinkItem(
    'common.sidebar.unified:supply-on-demand',
    'market-partner:html:fulfillment-supply-on-demand:get',
);
export const fulfillmentWithdraw = getLinkItem(
    'common.sidebar.unified:withdraw-utilization',
    'market-partner:html:fulfillment-withdraw:get',
);
export const stocks = getLinkItem('common.sidebar.unified:stocks', 'market-partner:html:stocks:get');
export const fulfillmentStatStocks = getLinkItem(
    'common.sidebar.unified:fulfillment-stat-stocks',
    'market-partner:html:fulfillment-stat-stocks:get',
);
export const fulfillmentStatDaily = getLinkItem(
    'common.sidebar.unified:stat-daily',
    'market-partner:html:fulfillment-stat-daily:get',
);
export const fulfillmentStatStorageDetails = getLinkItem(
    'common.sidebar.unified:stat-storage-details',
    'market-partner:html:fulfillment-stat-storage-details:get',
);
export const fulfillmentStatStocksMovement = getLinkItem(
    'common.sidebar.unified:stat-stocks-movement',
    'market-partner:html:fulfillment-stat-stocks-movement:get',
);
export const unredeemedOrdersWithdraw = getLinkItem(
    'common.sidebar.unified:unredeemed-orders-withdraw',
    'market-partner:html:unredeemed-orders-withdraw:get',
);
