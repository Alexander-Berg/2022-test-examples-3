import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// eslint-disable-next-line no-restricted-imports
import {get} from 'lodash/fp';
import ReturnOrderButton from '@self/root/src/components/Orders/OrderReturnButton/__pageObject';
import {asus} from '@self/root/src/spec/hermione/configs/checkout/items';
import {preparePageForReturnInfo} from '@self/root/src/spec/hermione/scenarios/orders';

module.exports = makeSuite('Переход на страницу возврата', {
    feature: 'Заявление на возврат',
    params: {
        items: 'Товары в заказе',
    },
    defaultParams: {
        item: {
            skuId: asus.skuId,
            offerId: asus.offerId,
            wareMd5: asus.offerId,
            count: 1,
            id: 123456,
        },
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    returnGoodsButton: () => this.createPageObject(ReturnOrderButton, {parent: this.myOrder}),
                });

                const order = await this.browser.yaScenario(this, preparePageForReturnInfo, {
                    orderItems: [this.params.item],
                    returnableItems: [this.params.item],
                });
                const orderId = get(['orders', '0', 'id'], order);
                this.params.orderId = orderId;
                if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                    return this.browser.yaOpenPage(this.params.pageId, {orderId});
                }
                return this.browser.yaOpenPage(this.params.pageId, {});
            },
        },
        {
            'Для заказа в статусе DELIVERED': {
                'переход должен пройти успешно': makeCase({
                    id: 'bluemarket-2446',
                    issue: 'BLUEMARKET-3176',
                    environment: 'kadavr',
                    async test() {
                        this.returnGoodsButton
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Кнопка должна быть видимой');

                        // проверка новой вкладки возвратов
                        const tabIds = await this.browser.getTabIds();

                        await this.returnGoodsButton.click();

                        const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});

                        await this.allure.runStep(
                            'Переключаемся на новую вкладку, проверяем, что мы на форме оформления возвратов',
                            async () => {
                                await this.browser.switchTab(newTabId);
                                await this.browser.getUrl().should.eventually.to.be
                                    .link({
                                        pathname: '/my/returns/create',
                                        query: {
                                            orderId: String(this.params.orderId),
                                            type: 'refund',
                                        },
                                    }, {
                                        skipHostname: true,
                                        skipProtocol: true,
                                    });
                            }
                        );
                    },
                }),
            },
        }
    ),
});
