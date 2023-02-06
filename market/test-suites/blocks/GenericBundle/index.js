import {makeCase, makeSuite, mergeSuites} from 'ginny';
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.desktop';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import GenericBundleDescription from '@self/project/src/components/GenericBundleDescription/__pageObject';

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
                cartButton: () => this.createPageObject(CartButton),
                cartEntryPoint: () => this.createPageObject(CartEntryPoint),
                genericBundleDescription: () => this.createPageObject(GenericBundleDescription),
            });
        },
        'По умолчанию акция отображается': makeCase({
            async test() {
                if (this.params.withPromoBlock) {
                    await this.genericBundleDescription.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Промо блок с подаркам отоброжается'
                        );
                } else {
                    await this.promoBadge.isVisible()
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
                await this.cartButton.click();

                await this.cartEntryPoint.waitForCounterVisible();
                const counterText = await this.cartEntryPoint.getCounterText();

                return this.expect(counterText)
                    .to.be.equal('2', 'Каунтер равен 2');
            },
        }),
    }),
});
