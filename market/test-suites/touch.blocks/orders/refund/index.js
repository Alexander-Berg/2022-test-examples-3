// eslint-disable-next-line no-restricted-imports
import {get} from 'lodash/fp';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
import createOrder from './utilities/createOrder';


module.exports = makeSuite('Переход на страницу возврата', {
    feature: 'Заявление на возврат',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const order = await createOrder.call(this, {status: 'DELIVERED'});

                const orderId = get(['orders', '0', 'id'], order);

                this.params.orderId = orderId;

                if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                    return this.browser.yaOpenPage(this.params.pageId, {orderId});
                }
                return this.browser.yaOpenPage(this.params.pageId);
            },
        },
        {
            'Для заказа со статусом DELIVERED': {
                'переход на страницу заполнения заявления должен пройти успешно': makeCase({
                    id: 'bluemarket-2748',
                    issue: 'BLUEMARKET-6634',
                    environment: 'kadavr',
                    async test() {
                        await this.orderCard.returnButton
                            .isVisible()
                            .should.eventually.to.be
                            .equal(true, 'Кнопка "Вернуть заказ" должна быть видимой');

                        const nextPageLink = await this.browser.yaWaitForChangeUrl(
                            () => this.orderCard.clickReturnButton(), 20000
                        );
                        const linkParams = {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        };

                        await nextPageLink.should.be.link({
                            query: {
                                orderId: this.params.orderId,
                            },
                            pathname: '/my/returns/create',
                        }, linkParams);
                    },
                }),
            },
        }
    ),
});
