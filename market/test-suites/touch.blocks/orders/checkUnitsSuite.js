import {
    makeCase,
    makeSuite,
} from 'ginny';

// pageObject
import OrderItem from '@self/root/src/components/OrderItem/__pageObject';

// mock
import orders from './unitsMock';


module.exports = makeSuite('Единицы измерения', {
    environment: 'kadavr',
    params: {
        expectedPriceText: 'Ожидаемая цена товара (включая валюту и ед. изм.)',
        expectedCounterText: 'Ожидаемое количество товара в единицах измерения',
    },
    defaultParams: {
        expectedPriceText: '852,47 ₽/уп',
        expectedCounterText: '8 уп',
    },
    feature: 'Отображаются упаковки на старнице Мои заказы.',
    story: {
        async beforeEach() {
            await this.browser.setState('Checkouter.collections.order', orders);
            await this.setPageObjects({
                orderItem: () => this.createPageObject(OrderItem),
            });
            await this.browser.yaOpenPage(this.params.pageId);
        },
        'отображаются в цене и счетчике': makeCase({
            async test() {
                const priceText = this.orderItem.getPriceText();
                const {expectedPriceText} = this.params;
                await this.expect(priceText).to.be.equal(expectedPriceText, 'упаковки в цене');

                const counterText = this.orderItem.getCountText();
                const {expectedCounterText} = this.params;
                await this.expect(counterText).to.be.equal(expectedCounterText, 'упаковки в счетчике');
            },
        }),
    },
});
