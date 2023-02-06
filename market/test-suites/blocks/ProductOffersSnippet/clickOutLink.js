import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок ProductOffersSnippet
 * @property {PageObject.ProductOffersSnippet} offerSnippet
 */
export default makeSuite('Кнопка перехода в магазин.', {
    params: {
        expectedHrefPath: 'Ожидаемая ссылка перехода в магазин',
    },
    story: {
        'По умолчанию': {
            'должна присутствовать': makeCase({
                async test() {
                    await this.offerSnippet.clickOutLink.isExisting().should.eventually.to.equal(
                        true, 'Кнопка "В магазин" должна присутствовать'
                    );

                    if (this.params.expectedHrefPath) {
                        const url = await this.offerSnippet.clickOutUrl;

                        await this.browser.allure.runStep('Читаем значение ссылки на кнопке "В магазин"',
                            () => this.expect(url).to.be.link(this.params.expectedHrefPath, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        );
                    }
                },
            }),
        },
    },
});
