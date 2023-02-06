import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-top-offer
 * @param {PageObject.ProductTopOffer} topOffer
 */
export default makeSuite('Сниппет оффера из ТОП6 без способов оплаты.', {
    feature: 'Сниппет.',
    story: {
        'Заглушка способов оплаты': {
            'по умолчанию': {
                'должна присутствовать': makeCase({
                    id: 'marketfront-3756',
                    issue: 'MARKETFRONT-4720',
                    async test() {
                        return this.topOffer.getDetailsPaymentStub()
                            .should.eventually.to.include(
                                'Варианты оплаты уточняйте в магазине',
                                'Содержит правильный текст'
                            );
                    },
                }),
            },
        },
    },
});
