import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * Тесты на элементы выдачи product-offers
 * @param {PageObject.Button} clickoutButton
 * @param {PageObject.PaymentType} paymentType - Способы оплаты
 */
export default makeSuite('Элемент списка офферов', {
    story: mergeSuites(
        {
            'Кнопка "В магазин"': {
                'По умолчанию': {
                    'содержит правильную ссылку, передаваемую из репорта': makeCase({
                        feature: 'Кнопка "В магазин"',
                        id: 'marketfront-1995',
                        issue: 'MARKETVERSTKA-27201',
                        params: {
                            urlDomain: 'Домен ссылки, которая должна содержаться в кнопке',
                        },
                        test() {
                            return this.clickoutButton.getHref()
                                .should.eventually.to.include(this.params.urlDomain, 'Содержит правильный текст');
                        },
                    }),
                },
            },
        },
        {
            'Блок способы оплаты': {
                'Содержит ожидаемый способ оплаты': makeCase({
                    id: 'marketfront-3375',
                    issue: 'MARKETVERSTKA-33916',
                    params: {
                        paymentTypeText: 'Текст способа оплаты, который должен содержаться в блоке',
                    },
                    test() {
                        return this.paymentType.getPaymentTypeText()
                            .should.eventually.to.include(
                                this.params.paymentTypeText,
                                'Оффер содержит ожидаемый текст'
                            );
                    },
                }),
            },
        },

        makeSuite('Кликаут.', {
            id: 'marketfront-4205',
            issue: 'MARKETFRONT-25074',
            story: {
                'Название товара': {
                    'Имеет корректную ссылку': makeCase({
                        async test() {
                            const url = await this.productOffer.getTitleLinkUrl();
                            return this.expect(url.path)
                                .to.equal(this.params.url, 'Кликаут ссылка корректна');
                        },
                    }),
                },
                'Цена': {
                    'Имеет корректную ссылку': makeCase({
                        async test() {
                            const url = await this.productOffer.getPriceLinkUrl();
                            return this.expect(url.path)
                                .to.equal(this.params.url, 'Кликаут ссылка корректна');
                        },
                    }),
                },
                'Название магазина': {
                    'Имеет корректную ссылку': makeCase({
                        async test() {
                            const url = await this.shopName.getShopLinkUrl();
                            return this.expect(url.path)
                                .to.equal(this.params.url, 'Кликаут ссылка корректна');
                        },
                    }),
                },
            },
        })
    ),
});
