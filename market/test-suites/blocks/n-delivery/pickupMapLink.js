import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery__text
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Ссылка на карту с самовывозом', {
    feature: 'Самовывоз',
    story: {
        'Ссылка на карту содержит обязательные параметры': makeCase({
            params: {
                productId: 'Идентификатор продукта',
                slug: 'Слаг продукта',
                fesh: 'Идентификатор магазина',
            },
            async test() {
                const {fesh, slug, productId} = this.params;
                const expectedHref = await this.browser.yaBuildURL('market:geo', {
                    fesh,
                    slug,
                    productId,
                });

                const actualHref = await this.delivery.getPickupMapLinkHref();

                return this.allure.runStep('Проверяем, что ссылка на карту правильная', () =>
                    this.expect(actualHref).to.be.link(
                        expectedHref,
                        {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    )
                );
            },
        }),
    },
});
