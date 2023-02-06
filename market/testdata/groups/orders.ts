import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getOrdersGroup = getGroupGetter('common.sidebar.unified:group.orders');

// links
export const ordersList = getLinkItem('common.sidebar.unified:orders-list', 'market-partner:html:orders-list:get');
export const fulfillmentOrders = getLinkItem(
    'common.sidebar.unified:orders-list',
    'market-partner:html:fulfillment-orders:get',
);
export const partnerOrders = getLinkItem(
    'common.sidebar.unified:partner-orders-list',
    'market-partner:html:partner-orders-list:get',
);
export const businessStatOrders = getLinkItem(
    'common.sidebar.unified:orders-stat',
    'market-partner:html:business-orders-report:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const fulfillmentStatDataMatrix = getLinkItem(
    'common.sidebar.unified:stat-data-matrix',
    'market-partner:html:fulfillment-stat-data-matrix:get',
);

export const returnsList = getLinkItem('common.sidebar.unified:returns-list', 'market-partner:html:returns:get');

export const businessOrders = getLinkItem(
    'common.sidebar.unified:business-orders',
    'market-partner:html:business-orders:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
