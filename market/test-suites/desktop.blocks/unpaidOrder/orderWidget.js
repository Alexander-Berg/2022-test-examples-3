import assert from 'assert';
import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import OrderCard from '@self/root/src/components/OrderCard/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';

import unpaidOrderPaymentProcess from '@self/root/src/spec/hermione/test-suites/desktop.blocks/unpaidOrder/paymentProcess';


module.exports = makeSuite('Виджет неоплаченного заказа.', {
    environment: 'kadavr',
    feature: 'Дооплата',
    params: {
        pageId: 'Идентификатор страницы',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.params.pageId, 'Param pageId must be defined');

                this.setPageObjects({
                    orderCard: () => this.createPageObject(OrderCard),
                });
            },
        },
        prepareSuite(unpaidOrderPaymentProcess, {
            params: {
                paymentButtonText: 'Оплатить',
            },
            pageObjects: {
                orderPaymentButton() {
                    return this.createPageObject(Button, {
                        parent: this.orderCard,
                        root: OrderCard.primaryButton,
                    });
                },
            },
        })
    ),
});
