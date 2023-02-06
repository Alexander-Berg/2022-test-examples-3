import {getGroupItem, getLinkItem} from '../../../utils/mocks';

export const company = getLinkItem('common.sidebar.tpl:company-list', 'market-partner:html:tpl-company-list:get');
const orders = getLinkItem('common.sidebar.tpl:order-list.child', 'market-partner:html:tpl-order-list:get');
const ordersDistribution = getLinkItem(
    'common.sidebar.tpl:order-distribution-list',
    'market-partner:html:tpl-order-distribution-list:get',
);
const couriers = getLinkItem('common.sidebar.tpl:courier-list.child', 'market-partner:html:tpl-courier-list:get');
const transports = getLinkItem('common.sidebar.tpl:transport-list', 'market-partner:html:tpl-transport-list:get');
export const shiftList = getLinkItem('common.sidebar.tpl:shift-list', 'market-partner:html:tpl-shift-list:get');
export const schedules = getLinkItem('common.sidebar.tpl:schedules', 'market-partner:html:tpl-schedules:get');
export const ordersUpload = getLinkItem(
    'common.sidebar.tpl:orders-upload',
    'market-partner:html:tpl-orders-upload:get',
);
export const courierList = getGroupItem('common.sidebar.tpl:courier-list', [couriers, transports]);
export const orderList = getGroupItem('common.sidebar.tpl:order-list', [orders, ordersDistribution]);
