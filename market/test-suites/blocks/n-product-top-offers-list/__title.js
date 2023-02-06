import {makeSuite, makeCase} from 'ginny';
import Region from '@self/platform/spec/page-objects/region';

/**
 * Тесты на элемент n-product-top-offers-list__title
 * @param {PageObject.ProductTopOffersList} topOffersList
 * @param {PageObject.Region} region
 */
export default makeSuite('Заголовок региона.', {
    feature: 'Регион',
    params: {
        city: 'Регион для установки',
        title: 'Ожидаемый заголовок для выбранного региона',
    },
    story: {
        'При выборе региона': {
            'Есть необходимый заголовок': makeCase({
                issue: 'MARKETVERSTKA-31854',
                id: 'marketfront-2986',
                test() {
                    return this.region.openForm()
                        .then(() => this.browser.waitForVisible(Region.suggest))
                        .then(() => this.region.setNewRegion(this.params.city))
                        .then(() => this.browser.waitForVisible(Region.selectFormList))
                        .then(() => this.region.getSuggestItemByIndex(1).click())
                        .then(() => this.region.applyNewRegionFromButton())
                        .then(() => this.topOffersList.title.isVisible()
                            .should.eventually.be.equal(true, 'Заголовок присутствует')
                        )
                        .then(() => this.topOffersList.title.getText()
                            .should.eventually.equal(this.params.title, 'Заголовок содержит нужный текст')
                        );
                },
            }),

            'При выборе региона Санкт-Петербург': {
                'Отсутствует регион в заголовоке': makeCase({
                    issue: 'MARKETVERSTKA-31905',
                    id: 'marketfront-2998',
                    test() {
                        return this.region.openForm()
                            .then(() => this.browser.waitForVisible(Region.suggest))
                            .then(() => this.region.setNewRegion('Санкт-Петербург'))
                            .then(() => this.browser.waitForVisible(Region.selectFormList))
                            .then(() => this.region.getSuggestItemByIndex(1).click())
                            .then(() => this.region.applyNewRegionFromButton())
                            .then(() => this.topOffersList.title.getText())
                            .should.eventually.be.equal('Предложения магазинов', 'Заголовок не содержит регион');
                    },
                }),
            },
        },
    },
});
