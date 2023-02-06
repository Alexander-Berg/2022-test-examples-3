import {makeCase, makeSuite, mergeSuites} from 'ginny';
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.touch';
import GenericBundleDescription from '@self/project/src/components/GenericBundleDescription/__pageObject';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';

export default makeSuite('Подарок', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-29987',
    params: {
        withPromoBlock: 'Проверить показ промо блока с подарком',
    },
    defaultParams: {
        withPromoBlock: false,
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                header: () => this.createPageObject(Header),
                cartEntryPoint: () => this.createPageObject(CartEntryPoint, {
                    parent: this.header,
                }),
                genericBundleDescription: () => this.createPageObject(GenericBundleDescription),
            });
        },
        'По умолчанию акция отображается': makeCase({
            async test() {
                if (this.params.withPromoBlock) {
                    await this.genericBundleDescription.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Промо блок с подаркам отображается'
                        );
                } else {
                    await this.dealsSticker.giftIcon.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Подарок должен отображаться'
                        );
                }
            },
        }),
        'Добавляем товар с подарком в корзину': makeCase({
            async test() {
                await this.browser.yaWaitForPageReady();
                const cartButtonSelector = await this.cartButton.getSelector();
                await this.browser.scroll(cartButtonSelector);
                await this.cartButton.click();

                await this.browser.waitForVisible(CartEntryPoint.counter, 20000);

                const counterText = await this.cartEntryPoint.getCounterText();

                return this.expect(counterText)
                    .to.be.equal('2', 'Каунтер равен 2');
            },
        }),
    }),
});
