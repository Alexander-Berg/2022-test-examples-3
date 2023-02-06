import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-top-offer
 * @param {PageObject.Button2} button
 * @param {PageObject.PaymentType} paymentType - Способы оплаты
 */
export default makeSuite('Сниппет оффера из ТОП6.', {
    feature: 'Топ 6',
    story: {
        'Кнопка "В магазин"': {
            'По умолчанию': {
                'содержит ссылку вида "market-click2-testing.yandex.ru"': makeCase({
                    id: 'marketfront-1965',
                    issue: 'MARKETVERSTKA-27219',
                    params: {
                        urlDomain: 'Домен ссылки, которая должна содержаться в кнопке',
                    },
                    test() {
                        return this.button.getHref()
                            .should.eventually.to.include(this.params.urlDomain, 'Содержит правильный URL');
                    },
                }),
                'содержит корректный путь': makeCase({
                    id: 'marketfront-4204',
                    issue: 'MARKETFRONT-25074',
                    async test() {
                        const url = await this.clickOutButton.getLinkUrl();
                        return this.expect(url.path)
                            .to.equal(this.params.url, 'Кликаут ссылка корректна');
                    },
                }),
            },
        },
        'Цена': {
            'Ссылка на цене': {
                'содержит корректный путь': makeCase({
                    id: 'marketfront-4204',
                    issue: 'MARKETFRONT-25074',
                    async test() {
                        const url = await this.topOfferSnippetCompact.priceClickoutLinkUrl();
                        return this.expect(url.path)
                            .to.equal(this.params.url, 'Кликаут ссылка корректна');
                    },
                }),
            },
        },
        'Название магазина': {
            'Ссылка в названии магазина': {
                'содержит корректный путь': makeCase({
                    id: 'marketfront-4204',
                    issue: 'MARKETFRONT-25074',
                    async test() {
                        const url = await this.topOfferSnippetCompact.shopNameClickoutLinkUrl();
                        return this.expect(url.path)
                            .to.equal(this.params.url, 'Кликаут ссылка корректна');
                    },
                }),
            },
        },
        'Блок Топ-6': {
            'Содержит не более 6 офферов': makeCase({
                id: 'marketfront-1966',
                issue: 'MARKETVERSTKA-27218',
                async test() {
                    const offersCount = await this.shopsTop6Info.getOffersCount();

                    return this.expect(offersCount > 0 && offersCount <= 6)
                        .to
                        .be
                        .equal(
                            true,
                            `На странице от 1 до 6 офферов (тут ${offersCount})`
                        );
                },
            }),
        },
    },
});
