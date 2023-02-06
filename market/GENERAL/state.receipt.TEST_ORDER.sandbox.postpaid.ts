import {mergeDeepRight} from 'ramda';
import {OrderBuilder} from 'shared/utils/order/OrderBuilder';
import {RU_DATE_WITH_SEPARATED_TIME} from '@yandex-market/b2b-core/shared/constants/date';
import {mockFeature} from '@yandex-market/b2b-core/shared/spec';

import dayjs from 'dayjs';

import {defaultPageState} from './baseState';

export default mergeDeepRight(defaultPageState, {
    state: {
        features: [mockFeature('canUseTrackCode')],
    },
    page: {
        data: {
            order: OrderBuilder.default
                .status('DELIVERED')
                .orderItem({
                    vat: 'VAT_20_120',
                    offerName:
                        'Беспроводные наушники Samsung Беспроводные наушники Samsung Galaxy Buds Live, red (Красный)',
                    price: 7389,
                    count: 2,
                })
                .delivery(deliveryBuilder =>
                    deliveryBuilder.with({
                        serviceName: 'Доставка',
                        price: 549,
                        vat: 'VAT_20_120',
                        liftPrice: 150,
                        liftType: 'CARGO_ELEVATOR',
                    }),
                )
                .with({
                    //
                    fake: true,
                    context: 'SANDBOX',
                    creationDate: '29-12-2021 16:42:37',
                    paymentMethod: 'CASH_ON_DELIVERY',
                    itemsTotal: 14990,
                })
                .creationDate((dayjsFn: typeof dayjs) => {
                    return dayjsFn('27.12.2021, 09:46', RU_DATE_WITH_SEPARATED_TIME);
                })
                .build(),
        },
    },
});
