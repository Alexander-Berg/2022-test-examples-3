import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок stickers_type_discount
 * @param {PageObject.Stickers} discountSticker
 */
export default makeSuite('Бейдж скидки.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                async test() {
                    const isDiscountBadgeExists = await this.discountSticker.isVisible();
                    return this.expect(isDiscountBadgeExists).to.equal(true, 'Бейдж скидки присутствует');
                },
            }),
        },
    },
});
