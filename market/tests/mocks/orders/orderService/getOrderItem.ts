import {mergeDeepRight} from 'ramda';

import type {OrderItem} from '~/app/bcm/orderService/Backend/types';
import {Vat} from '~/app/constants/vat';

export default (item: Partial<OrderItem> = {}): OrderItem =>
    mergeDeepRight<OrderItem, Partial<OrderItem>>(
        {
            itemId: 1,
            feedId: 201000464,
            offerName: 'Будка DreamBag складная Кошки 40 см голубой',
            pictureUrl: '//avatars.mds.yandex.net/get-mpic/901948/img_id7637861564482752331.jpeg/',
            price: {
                currency: 'RUB',
                value: 2230,
            },
            subsidy: {
                currency: 'RUB',
                value: 0,
            },
            countInDelivery: 1,
            shopSku: 'strundel.731',
            cis: [],
            cargoTypes: [],
            vatRate: Vat.VAT_20_120,
        },
        item,
    ) as OrderItem;
