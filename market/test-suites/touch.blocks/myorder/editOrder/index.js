import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {OrderInfo} from '@self/root/src/components/OrderInfo/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

import editOrderDeliveryDate from '@self/root/src/spec/hermione/test-suites/blocks/orderEdit/deliveryDate';
import storageProlongation from '@self/root/src/spec/hermione/test-suites/blocks/orderEdit/storageProlongation';

module.exports = makeSuite('Редактирование заказа.', {
    environment: 'kadavr',

    story: mergeSuites(
        {
            beforeEach() {
                return this.setPageObjects({
                    deliveryInfo: () => this.createPageObject(OrderInfo),
                    changeDeliveryDateLink: () => this.createPageObject(Link, {
                        parent: this.deliveryInfo,
                        root: OrderInfo.changeDeliveryDateLink,
                    }),
                });
            },
        },
        prepareSuite(editOrderDeliveryDate, {}),
        prepareSuite(storageProlongation, {})
    ),
});
