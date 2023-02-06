import {makeCase, makeSuite} from 'ginny';

/**
 * @property {PageObject.ShopRating} shopRating
 */
export default makeSuite('Рейтинг Магазина.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен быть виден.': makeCase({
                async test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что рейтинг виден',
                        () => this.shopRating.isVisible()
                            .should.eventually.to.be.equal(true, 'Рейтинг виден')
                    );
                },
            }),
        },
    },
});
