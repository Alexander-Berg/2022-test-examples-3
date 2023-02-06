import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на заголовок на карточке модели
 *
 * @param {PageObject.ProductOffersRegionHeader} productOffersRegionHeader
 * @param {PageObject.Region} region
 */
export default makeSuite('Заголовок региона.', {
    story: {
        'При выборе региона': {
            'заголовок содержит название этого региона': makeCase({
                feature: 'Регион.',
                id: 'marketfront-2981',
                issue: 'MARKETVERSTKA-31836',
                params: {
                    city: 'Регион для установки',
                    title: 'Ожидаемый заголовок для выбранного региона',
                },
                async test() {
                    await this.browser.yaScenario(this, 'region.changeTo', this.params.city, true);
                    await this.productOffersRegionHeader.title.isVisible()
                        .should.eventually.be.equal(true, 'Заголовок присутствует');
                    return this.productOffersRegionHeader.getHeaderText()
                        .should.eventually.equal(this.params.title, 'Заголовок содержит нужный текст');
                },
            }),

            'Отсутствует заголовок "Предложения магазинов"': makeCase({
                feature: 'Регион.',
                id: 'marketfront-2997',
                issue: 'MARKETVERSTKA-31904',
                async test() {
                    await this.browser.yaScenario(this, 'region.changeTo', 'Москва');
                    return this.productOffersRegionHeader.isExisting()
                        .should.eventually.be.equal(false, 'Заголовка нет');
                },
            }),
        },
    },
});
