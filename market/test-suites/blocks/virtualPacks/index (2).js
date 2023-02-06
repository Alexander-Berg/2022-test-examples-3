import {makeCase, makeSuite} from 'ginny';

// constants
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds/pageIds';

// pageObjects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';

// mocks
import {state, route, minCount} from './mock';

export default makeSuite('Виртуальные спайки', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-80143',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton, {
                    parent: DefaultOffer.root,
                }),
                popupCartCounter: () => this.createPageObject(CounterCartButton, {
                    parent: CartPopup.root,
                }),
            });

            this.minCount = minCount;
            await this.browser.setState('report', state);
            return this.browser.yaOpenPage(
                PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                route
            );
        },
        'по умолчанию': makeCase({
            id: 'marketfront-5804',
            async test() {
                const actualText = this.cartButton.getText();
                const expectedText = `В корзину от ${this.minCount} товаров`;
                return this.expect(actualText).to.be.equal(
                    expectedText,
                    'Надпись в кнопке учитывает минимум'
                );
            },
        }),
        'при нажатии на кнопку': makeCase({
            id: 'marketfront-5805',
            async test() {
                await this.browser.yaWaitForPageReady();
                await this.cartButton.click();
                await this.browser.waitForVisible(CartPopup.root, 11000);
                await new Promise(f => setTimeout(f, 1000));

                const requests = await this.browser.yaGetKadavrLogByBackendMethod('Carter', 'addItem');
                const actualCount = requests.length ? requests[0].request.body.count : undefined;
                return this.expect(actualCount).to.be.equal(
                    this.minCount,
                    'В стейте нужное число товара'
                );
            },
        }),
        'при попытки увеличения количества в счетчике': makeCase({
            id: 'marketfront-5803',
            async test() {
                await this.browser.yaWaitForPageReady();
                await this.cartButton.click();
                await this.browser.waitForVisible(CartPopup.root, 10000);
                await this.popupCartCounter.increase.click();
                await this.popupCartCounter.waitUntilCounterChanged(this.minCount, this.minCount);
            },
        }),
    },
});
