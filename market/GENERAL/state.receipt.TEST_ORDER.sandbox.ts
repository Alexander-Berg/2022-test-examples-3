import {mergeDeepRight} from 'ramda';
import {OrderBuilder} from 'shared/utils/order/OrderBuilder';

import {defaultPageState} from './baseState';

export default mergeDeepRight(defaultPageState, {
    page: {
        data: {
            order: OrderBuilder.default
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
                })
                .build(),
        },
    },
});
