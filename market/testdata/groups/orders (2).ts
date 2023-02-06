import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getOrdersGroup = getGroupGetter('common.sidebar.unified:group.orders');

// links
export const ordersList = getLinkItem('common.sidebar.unified:orders-list', 'market-partner:html:orders-list:get');
export const fulfillmentOrders = getLinkItem(
    'common.sidebar.unified:orders-list',
    'market-partner:html:fulfillment-orders:get',
);
export const statOrders = getLinkItem('common.sidebar.unified:orders-stat', 'market-partner:html:stat-orders:get');
export const fulfillmentStatOrders = getLinkItem(
    'common.sidebar.unified:orders-stat',
    'market-partner:html:fulfillment-stat-orders:get',
);
export const fulfillmentStatDataMatrix = getLinkItem(
    'common.sidebar.unified:stat-data-matrix',
    'market-partner:html:fulfillment-stat-data-matrix:get',
);

export const returnsList = getLinkItem('common.sidebar.unified:returns-list', 'market-partner:html:returns:get');
