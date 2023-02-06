import {
    makeCase,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import MyOrderOrderStatus from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import DeliveryInfo from '@self/root/src/components/Orders/DeliveryInfo/__pageObject';


module.exports = makeSuite('Предзаказ.', {
    feature: 'Предзаказ',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            beforeEach() {
                this.setPageObjects({
                    myOrderOrderStatus: () => this.createPageObject(MyOrderOrderStatus, {parent: this.myOrder}),
                    deliveryInfo: () => this.createPageObject(DeliveryInfo, {parent: this.myOrder.orderDetails}),
                });

                return createOrder.call(this, 'PREPAID', 'YANDEX');
            },
            'отображается статус "Оформлен предзаказ"': makeCase({
                id: 'bluemarket-2450',
                issue: 'BLUEMARKET-3328',
                test() {
                    const text = 'Оформлен предзаказ';

                    return this.myOrderOrderStatus.getStatusText()
                        .should.eventually.to.be.equal(text, `Статус должен быть "${text}"`);
                },
            }),
            'в доставке отображается текст, что перед доставкой свяжется сотрудник колл-центра': makeCase({
                id: 'bluemarket-2450',
                issue: 'BLUEMARKET-3328',
                test() {
                    const text = 'Перед этим вам позвонит сотрудник колл-центра, чтобы подтвердить доставку.';

                    return this.deliveryInfo.getDeliveryText()
                        .should.eventually.include(
                            text,
                            `Текст должен содержать "${text}"`
                        );
                },
            }),
        },
    },
});

function createOrder(paymentType, paymentMethod) {
    const {browser} = this;

    return browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            region: this.params.region,
            orders: [{
                items: [{skuId: checkoutItemIds.asus.skuId}],
                deliveryType: 'DELIVERY',
            }],
            paymentType,
            paymentMethod,
            status: 'PENDING',
            substatus: 'PREORDER',
            preorder: true,
        }
    )
        .then(response => {
            if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                const orderId = _.get(response, ['orders', 0, 'id']);

                return this.browser.yaOpenPage(this.params.pageId, {orderId});
            }

            return this.browser.yaOpenPage(this.params.pageId);
        });
}
