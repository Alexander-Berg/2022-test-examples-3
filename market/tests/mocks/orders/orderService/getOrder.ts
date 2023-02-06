import {mergeDeepRight} from 'ramda';

import type {Order} from '~/app/bcm/orderService/Backend/types';
import {Status} from '~/app/entities/order/types/status';
import {SubStatus} from '~/app/entities/order/types/subStatus';

import getOrderItem from './getOrderItem';

export default (order: Partial<Order> = {}): Order =>
    mergeDeepRight<Order, Partial<Order>>(
        {
            orderId: 1,
            shipmentDeadline: {
                type: 'EXACT',
                deadline: '2022-02-14T12:01:00+03:00',
            },
            status: Status.Processing,
            subStatus: SubStatus.Started,
            hasCancellationRequest: false,
            cancellationRequestCreatedAt: null,
            itemsTotal: {
                currency: 'RUB',
                value: 2230,
            },
            subsidyTotal: {
                currency: 'RUB',
                value: 0,
            },
            createdAt: '2022-02-14T10:53:28+03:00',
            updatedAt: '2022-02-14T10:53:28+03:00',
            confirmDeadline: '2022-02-14T12:53:28+03:00',
            deliveryTotal: {
                currency: 'RUB',
                value: 0,
            },
            deliveryDate: {
                fromDate: '2022-03-01',
                toDate: '2022-03-01',
                fromTime: '16:20:00',
                toTime: '17:50:00',
            },
            items: [getOrderItem()],
            realDeliveryDate: '2022-02-14T10:53:28+03:00',
        },
        order,
    ) as Order;
