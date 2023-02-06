import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import promoMock from '@self/root/src/spec/hermione/kadavr-mock/loyalty/promos';

module.exports = makeSuite('Кнопка прикрепления купона.', {
    id: 'bluemarket-2798',
    issue: 'BLUEMARKET-6631',
    params: {
        text: 'Текст, который должен быть на кнопке прикрепления купона',
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    bonusButton: () => this.createPageObject(Button, {parent: this.bindBonus}),
                });
            },
        },
        makeSuite('По-умолчанию', {
            story: {
                'отображается корректно': makeCase({
                    async test() {
                        await this.bonusButton.isVisible()
                            .should.eventually.be.equal(true, 'Кнопка прикрепления купона должна быть видимой.');

                        return this.bonusButton.getButtonText()
                            .should.eventually.be.equal(
                                this.params.text,
                                `На кнопке прикрепления купона должен быть текст ${this.params.text}`
                            );
                    },
                }),
            },
        }),

        makeSuite('Акция закончилась.', {
            defaultParams: {
                promo: promoMock.expired,
            },
            story: {
                'Кнопка не отображается': makeCase({
                    test() {
                        return this.bonusButton.isExisting()
                            .should.eventually.be.equal(
                                false,
                                'Кнопка прикрепления купона не должна быть видимой.'
                            );
                    },
                }),
            },
        })
    ),
});
