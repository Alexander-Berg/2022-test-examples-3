import {
    makeSuite,
    makeCase,
} from 'ginny';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

import OrderConfirmationAuth
    from '@self/root/src/widgets/content/OrderConfirmationAuth/components/OrderConfirmationAuthView/__pageObject';

module.exports = makeSuite('Блок доавторизации.', {
    feature: 'Спасибо за заказ',
    id: 'bluemarket-2460',
    issue: 'BLUEMARKET-3160',
    environment: 'kadavr',
    params: {
        orderId: 'ID заказа',
        bindKey: 'Ключ привязки заказа',
    },
    defaultParams: {
        orderId: generateRandomId(),
        bindKey: `${generateRandomId()}`,
        isAuth: false,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderConfirmationAuth: () => this.createPageObject(OrderConfirmationAuth),
            });

            const {orderId, bindKey} = this.params;

            await this.browser.yaScenario(
                this,
                prepareOrder,
                {
                    status: 'DELIVERED',
                    region: this.params.region,
                    orders: [{
                        orderId,
                        items: [{skuId: checkoutItemIds.asus.skuId}],
                        deliveryType: 'DELIVERY',
                        bindKey: this.params.bindKey,
                    }],
                    paymentType: 'PREPAID',
                    paymentMethod: 'CASH_ON_DELIVERY',
                    additionalKadavrCollections: {},
                }
            );

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS_CONFIRMATION, {orderId, bindKey});
        },

        'по умолчанию': {
            'отображается только гостю и имеет кнопку "Присоединиться" со ссылкой на паспорт': makeCase({
                async test() {
                    const {browser} = this;
                    const selector = await this.orderConfirmationAuth.getSelector();

                    if (this.params.isAuth) {
                        return browser.isVisible(selector)
                            .should.eventually.to.be.equal(
                                false,
                                'Блок доавторизации не должен быть отображен для авторизованного пользователя'
                            );
                    }

                    await browser.isVisible(selector)
                        .should.eventually.to.be.equal(
                            true,
                            'Блок доавторизации должен быть отображен для неавторизованного пользователя'
                        );

                    await this.orderConfirmationAuth.authButtonLink.getText()
                        .should.eventually.to.be.equal(
                            'Присоединиться',
                            'Кнопка должна содержать текст "Присоединиться"'
                        );

                    await this.orderConfirmationAuth.getAuthButtonLinkHref()
                        .should.eventually.to.be.link({
                            pathname: /^\/auth$/,
                            query: {
                                retpath: new RegExp(`/my/orders\\?bindKey=${this.params.bindKey}`),
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
