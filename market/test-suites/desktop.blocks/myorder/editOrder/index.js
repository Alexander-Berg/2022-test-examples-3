import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import DeliveryInfo from '@self/root/src/components/Orders/DeliveryInfo/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

import editOrderDeliveryDate from '@self/root/src/spec/hermione/test-suites/blocks/orderEdit/deliveryDate';
import storageProlongation from '@self/root/src/spec/hermione/test-suites/blocks/orderEdit/storageProlongation';

module.exports = makeSuite('Редактирование заказа.', {
    environment: 'kadavr',

    story: mergeSuites(
        {
            beforeEach() {
                return this.setPageObjects({
                    deliveryInfo: () => this.createPageObject(DeliveryInfo),
                    changeDeliveryDateLink: () => this.createPageObject(Link, {
                        parent: this.DeliveryInfo,
                        root: DeliveryInfo.changeDeliveryDateLink,
                    }),
                });
            },
        },
        prepareSuite(editOrderDeliveryDate, {}),
        prepareSuite(storageProlongation, {})
    ),
});
