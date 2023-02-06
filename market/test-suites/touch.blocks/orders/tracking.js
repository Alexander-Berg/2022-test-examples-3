import {
    makeCase,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {checkpoints} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/checkpoints';

import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import {ActionLink} from '@self/root/src/components/OrderActions/Actions/ActionLink/__pageObject';

const orderId = 123456;


module.exports = makeSuite('Трекинг.', {
    feature: 'Трекинг',
    environment: 'kadavr',
    id: 'bluemarket-3256',
    issue: 'BLUEMARKET-9971',
    story: {
        beforeEach() {
            this.setPageObjects({
                orderCard: () => this.createPageObject(OrderCard),
                trackOrderLink: () => this.createPageObject(
                    ActionLink,
                    {
                        parent: this.orderCard,
                        root: `${ActionLink.root}${ActionLink.trackOrderLink}`,
                    }
                ),
                whereIsMyOrderLink: () => this.createPageObject(
                    ActionLink,
                    {
                        parent: this.orderCard,
                        root: `${ActionLink.root}${ActionLink.whereIsMyOrderLink}`,
                    }
                ),
            });
        },

        'Ссылка на трекинг': {
            'для не доставленного заказа называется "Где мой заказ", по клику открывает страницу трекинга': makeCase({
                async test() {
                    await createOrder.call(this, {status: 'DELIVERY'});

                    await this.whereIsMyOrderLink.isVisible()
                        .should.eventually.to.be.equal(false, 'Ссылка "Где мой заказ" не должна быть видна');

                    await this.browser.yaWaitForChangeUrl(
                        () => this.browser.allure.runStep(
                            'Кликаем по "Где мой заказ"',
                            () => this.whereIsMyOrderLink.root.click()
                        )
                    )
                        .should.eventually.to.be.link({
                            pathname: `^/my/order/${orderId}/track$`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipQuery: true,
                        });
                },
            }),

            'для отмененного заказа не отображается ни "Где мой заказ", ни "Отследить заказ"': makeCase({
                async test() {
                    await createOrder.call(this, {status: 'CANCELLED'});

                    await this.browser.isVisible(await this.trackOrderLink.getSelector())
                        .should.eventually.to.be.equal(false, 'Ссылка "Отследить заказ" не должна быть видна');

                    await this.browser.isVisible(await this.whereIsMyOrderLink.getSelector())
                        .should.eventually.to.be.equal(false, 'Ссылка "Где мой заказ" не должна быть видна');
                },
            }),

            'для доставленного заказа не отображается "Отследить заказ"': makeCase({
                async test() {
                    await createOrder.call(this, {status: 'DELIVERED'});

                    await this.browser.isVisible(await this.trackOrderLink.getSelector())
                        .should.eventually.to.be.equal(false, 'Ссылка "Отследить заказ" не должна быть видна');
                },
            }),
        },
    },
});

async function createOrder({status}) {
    const {browser} = this;

    await browser.yaScenario(
        this,
        prepareOrder,
        {
            orders: [{
                orderId,
                deliveryType: 'DELIVERY',
                delivery: {
                    shipments: [{
                        tracks: [
                            {
                                checkpoints,
                            },
                        ],
                    }],
                    tracks: [
                        {
                            checkpoints,
                        },
                    ],
                },
            }],
            status,
            creationDate: '20-04-2020 11:45:10',
            creationDateTimestamp: 1587372310000,
        }
    );

    return this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
}
