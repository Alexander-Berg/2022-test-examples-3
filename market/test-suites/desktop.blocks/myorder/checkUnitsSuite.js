import {
    makeCase,
    makeSuite,
} from 'ginny';

// pageObject
import MyOrderPrice from '@self/root/src/components/Orders/OrderItems/OrderPrice/__pageObject';

// mock
import orders from './unitsMock';


module.exports = makeSuite('Единицы измерения', {
    environment: 'kadavr',
    params: {
        expectedPriceText: 'Цена товара за единицу измерения и количество',
    },
    defaultParams: {
        expectedPriceText: '852,47 ₽/уп × 8',
    },
    feature: 'Отображаются упаковки на старнице Мои заказы.',
    story: {
        async beforeEach() {
            await this.browser.setState('Checkouter.collections.order', orders);
            await this.setPageObjects({
                orderPrice: () => this.createPageObject(MyOrderPrice),
            });
            await this.browser.yaOpenPage(this.params.pageId);
        },
        'отображаются в цене': makeCase({
            async test() {
                const priceText = this.orderPrice.getPriceText();
                const {expectedPriceText} = this.params;
                await this.expect(priceText).to.be.equal(expectedPriceText, 'упаковки в цене');
            },
        }),
    },
});
