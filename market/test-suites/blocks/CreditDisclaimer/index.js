import {makeSuite, makeCase} from 'ginny';

import CreditDisclaimer from '@self/platform/widgets/parts/CreditDisclaimer/__pageObject';

/**
 * Тесты на виджет CreditDisclaimer.
 * @property {PageObject.CreditDisclaimer} creditDisclaimer
 */
export default makeSuite('Юридический дисклеймер о покупке в кредит.', {
    feature: 'Кредиты',
    environment: 'kadavr',
    story: {
        'По умолчанию должен присутствовать': makeCase({
            async test() {
                this.setPageObjects({
                    creditDisclaimer: () => this.createPageObject(CreditDisclaimer),
                });

                await this.creditDisclaimer.isVisible()
                    .should.eventually.to.be.equal(true,
                        'Проверяем, что юридический дисклеймер о покупке в кредит присутствует.');
            },
        }),
    },
});
