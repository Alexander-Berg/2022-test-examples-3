import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок MapFilters.
 * @param {PageObject.MapFilters}
 */
export default makeSuite('Блок фильтров на карте.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                feature: 'Карта',
                id: 'm-touch-3086',
                issue: 'MARKETFRONT-5441',
                async test() {
                    await this.mapFilters
                        .isExisting()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что отображается блок фильтров'
                        );
                },
            }),
        },
        'Фильтр “Магазины”': {
            'работоспособен': makeCase({
                feature: 'Карта',
                id: 'm-touch-3086',
                issue: 'MARKETFRONT-5441',
                async test() {
                    // TODO: унести в страничные сьюты, страницы touch:product-geo больше нет
                    const expectedUrl = await this.browser.yaBuildURL('touch:product-geo', {
                        ...this.params.route,
                        'offer-shipping': 'store',
                    });

                    await this.mapFilters.clickStoreButton();
                    const changedUrl = await this.browser.yaParseUrl();

                    return this.expect(changedUrl)
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'Фильтр “Пункты самовывоза”': {
            'работоспособен': makeCase({
                feature: 'Карта',
                id: 'm-touch-3087',
                issue: 'MARKETFRONT-5441',
                async test() {
                    // TODO: унести в страничные сьюты, страницы touch:product-geo больше нет
                    const expectedUrl = await this.browser.yaBuildURL('touch:product-geo', {
                        ...this.params.route,
                        'offer-shipping': 'pickup',
                    });

                    await this.mapFilters.clickPickupsButton();
                    const changedUrl = await this.browser.yaParseUrl();

                    return this.expect(changedUrl)
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
