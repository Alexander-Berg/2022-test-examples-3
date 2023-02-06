import {makeCase, makeSuite} from 'ginny';

// constants
import {PAGE_IDS_DESKTOP} from '@self/root/src/constants/pageIds/pageIds';

// pageObjects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import AmountSelect from '@self/project/src/components/AmountSelect/__pageObject';

// mocks
import {minCount, state, path} from './mock';

export default makeSuite('Виртуальные спайки', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-80143',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton, {parent: DefaultOffer.root}),
                cartPopup: () => this.createPageObject(CartPopup),
                popupCartCounter: () => this.createPageObject(AmountSelect, {parent: this.cartPopup}),
            });

            await this.browser.setState('report', state);
            this.minCount = minCount;

            return this.browser.yaOpenPage(PAGE_IDS_DESKTOP.YANDEX_MARKET_PRODUCT, path);
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
                await this.browser.waitForVisible(CartPopup.root, 10000);

                const requests = await this.browser.yaGetKadavrLogByBackendMethod('Carter', 'addItem');
                const actualCount = requests.length ? requests[0].request.body.count : undefined;
                return this.expect(actualCount).to.be.equal(
                    minCount,
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
