import type {GetOrdersResult} from '~/app/bcm/orderService/Client/OrdersAppClient/types';
import type {Order} from '~/app/bcm/orderService/Backend/types';

export default (orders: Order[] = []): GetOrdersResult => ({
    pager: {
        pageSize: 10,
        currentPage: 1,
        totalCount: 9,
        hasNext: false,
        hasPrev: false,
    },
    orders,
});
