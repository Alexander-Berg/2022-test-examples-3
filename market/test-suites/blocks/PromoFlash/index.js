import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {
    prepareKadavrReportStateAdditionalOffers,
} from '@self/project/src/spec/hermione/fixtures/promo/flash';

export default makeSuite('Флэш акция', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-50855',
    story: mergeSuites({
        'По умолчанию акция отображается': makeCase({
            async test() {
                await this.timerFlashSale.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Показываем Flash таймер'
                    );
            },
        }),
        'При наведении у тултипа ожидаемое поведение': makeCase({
            async test() {
                const {state} = prepareKadavrReportStateAdditionalOffers();
                await this.browser.setState('report', state);

                await this.timerFlashSale.onHover();
                await this.promoFlashDescription.waitForVisible();
                await this.promoFlashDescription.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'При наведении на таймер показывам промо блок с флэш акцией'
                    );

                await this.promoFlashDescription.waitForSnippetVisible();

                await this.browser.yaWaitKadavrLogByBackendMethod(
                    'Carter',
                    'addItem',
                    () => this.promoFlashDescription.clickAddToCart()
                ).then(({request}) => request.body.count)
                    .should.eventually.to.be.equal(
                        1,
                        'Товар добавляется в корзину'
                    );
            },
        }),
    }),
});
