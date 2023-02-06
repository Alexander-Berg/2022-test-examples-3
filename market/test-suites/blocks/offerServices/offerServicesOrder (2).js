import {makeSuite, makeCase} from 'ginny';

import OrderServiceStatus from '@self/root/src/components/Orders/OrderServiceStatus/__pageObject';
import OrderService from '@self/root/src/components/Orders/OrderServices/OrderService/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_ITEM_SERVICE_STATUS} from '@self/root/src/entities/orderItemService';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import OrderServices from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderServices/__pageObject';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

export default makeSuite('Заказы', {
    id: 'MARKETFRONT-57698',
    environment: 'kadavr',
    feature: 'Доп услуги в заказах',
    issue: 'MARKETFRONT-57687',
    params: {
        ...commonParams.description,
        orderId: 'ID заказа',
        bindKey: 'Ключ привязки заказа',
    },
    defaultParams: {
        ...commonParams.value,
        isAuthWithPlugin: true,
        orderId: generateRandomId(),
        bindKey: `${generateRandomId()}`,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderServiceStatus: () => this.createPageObject(OrderServiceStatus),
                orderService: () => this.createPageObject(OrderService),
                orderServices: () => this.createPageObject(OrderServices),
            });

            await this.browser.yaScenario(
                this,
                'checkoutResource.prepareOrder',
                {
                    region: this.params.region,
                    orders: [{
                        orderId: this.params.orderId,
                        bindKey: this.params.bindKey,
                        items: [{
                            skuId: checkoutItemIds.asus.skuId,
                            services: [{
                                id: '1_42',
                                serviceId: 42,
                                status: ORDER_ITEM_SERVICE_STATUS.CONFIRMED,
                                title: 'Услуга1',
                                date: '22-04-2045 10:00:00',
                            }],
                        }],
                        deliveryType: 'DELIVERY',
                    }],
                    paymentType: 'POSTPAID',
                    paymentMethod: 'CASH_ON_DELIVERY',
                }
            );
        },

        'Отображение услуги на странице подтверждения заказа': makeCase({
            async test() {
                const {orderId} = this.params;
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS_CONFIRMATION, {orderId, bindKey: `${orderId}`, lr: 213});

                await this.browser.scroll(await this.orderServices.getSelector());
                return this.expect(this.orderServices.isVisible())
                    .to.be.equal(true, 'Выводится информация о добавленных услугах');
            },
        }),

        'Отображение услуги на странице заказов': makeCase({
            async test() {
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS, {lr: 213});

                return this.expect(this.orderServiceStatus.isVisible())
                    .to.be.equal(true, 'Выводится статус добавленных услуг');
            },
        }),

        'Отображение услуги на странице заказа': makeCase({
            async test() {
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER, {orderId: this.params.orderId, lr: 213});

                await this.expect(this.orderServiceStatus.isVisible())
                    .to.be.equal(true, 'Выводится статус добавленных услуг');
                await this.browser.scroll(await this.orderService.getSelector());
                return this.expect(this.orderService.isVisible())
                    .to.be.equal(true, 'Выводится информация о добавленных услугах');
            },
        }),
    },
});
