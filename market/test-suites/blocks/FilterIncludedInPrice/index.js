import {makeSuite, makeCase} from 'ginny';

import {productOffersWithIIPF} from '@self/platform/spec/hermione/fixtures/productOffers/productOffersWithIIPF';

/**
 * Тесты на блок FilterIncludedInPrice.
 * @param {PageObject.FilterIncludedInPrice} filterIncludedInPrice
 * @param {PageObject.Delivery} snippetDelivery
 */
export default makeSuite('Фильтр "С учетом доставки"', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'фильтр присутствует': makeCase({
                id: 'marketfront-3810',
                issue: 'MARKETFRONT-5485',
                test() {
                    return this.filterIncludedInPrice.isVisible()
                        .should.eventually.to.be.equal(true, 'Фильтр присутствует на странице');
                },
            }),
        },
        'При выборе "С учётом самовывоза"': {
            'текст самовывоза в сниппете содержит "включён в цену"': makeCase({
                id: 'marketfront-3809',
                issue: 'MARKETFRONT-5485',
                async test() {
                    const {reportState} = productOffersWithIIPF(true);
                    await this.browser.setState('report', reportState);

                    await this.filterIncludedInPrice.clickIncludePickupRadio();

                    // обновление фильтров, не знаю на что завязаться еще
                    // блок, на котором ожидается результат, не пропадает и waitForVisible не заюзать тут
                    await this.browser.yaDelay(6000);

                    const pickupText = await this.snippetDelivery.getPickupText();

                    await this.expect(pickupText).to.contain(
                        'включён в цену',
                        'На сниппете присутствует текст о включенном в стоимсоть самовывозе'
                    );
                },
            }),
        },
    },
});
