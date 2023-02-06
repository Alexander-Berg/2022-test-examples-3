import {mergeDeepRight} from 'ramda';

import type {OrderDetails} from '~/app/bcm/orderService/Backend/types';
import {PaymentMethod, PaymentType} from '~/app/entities/order/types/payment';
import {DeliveryType} from '~/app/entities/delivery/types';
import {Status} from '~/app/entities/order/types/status';
import {SubStatus} from '~/app/entities/order/types/subStatus';
import {Vat} from '~/app/constants/vat';

import getOrderItem from './getOrderItem';

export default (order: Partial<OrderDetails> = {}): OrderDetails =>
    mergeDeepRight<OrderDetails, Partial<OrderDetails>>(
        {
            orderId: 33055960,
            merchantOrderId: '33055960',
            status: Status.Delivered,
            subStatus: SubStatus.DeliveryServiceDelivered,
            paymentType: PaymentType.Prepaid,
            paymentMethod: PaymentMethod.Yandex,
            hasCancellationRequest: false,
            cancellationRequestCreatedAt: null,
            marketBranded: false,
            itemsTotal: {
                currency: 'RUB',
                value: 352,
            },
            subsidyTotal: {
                currency: 'RUB',
                value: 0,
            },
            createdAt: '2022-03-14T11:43:31+03:00',
            updatedAt: '2022-03-14T11:43:31+03:00',
            shipmentDeadline: {
                type: 'EXACT',
                deadline: '2022-03-14T12:52:00+03:00',
            },
            deliveryInfo: {
                realDeliveryDate: '2022-03-14T12:13:31+03:00',
                deliveryTotal: {
                    currency: 'RUB',
                    value: 0,
                },
                partnerDeliveryId: '',
                deliveryType: DeliveryType.Delivery,
                deliverySubsidy: {
                    currency: 'RUB',
                    value: 0,
                },
                deliveryVatRate: Vat.VAT_20,
                liftType: null,
                liftPrice: {
                    currency: 'RUB',
                    value: 0,
                },
                address: {
                    city: 'Москва',
                    country: 'Россия',
                    floor: null,
                    house: '16',
                    postcode: '119021',
                    street: 'улица Льва Толстого',
                    subway: null,
                    apartment: '1',
                },
                fromDate: '2022-03-14',
                toDate: '2022-03-14',
                fromTime: '13:50:00',
                toTime: '15:20:00',
            },
            note: 'dont call me',
            merchantNote: null,
            cancellationRequestSubstatus: null,
            confirmDeadline: '2022-03-14T12:13:31+03:00',
            buyer: {
                uid: '1444604344',
                firstName: 'Заказ',
                middleName: '',
                lastName: 'Тестовый',
                phone: '79999686048',
                email: 'express.expressov@yandex.ru',
            },
            items: [getOrderItem()],
            editRequest: {
                lines: null,
                deliveryDates: null,
            },
            warehouseIds: [666],
        },
        order,
    ) as OrderDetails;
