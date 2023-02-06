import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок n-deals-sticker
 *
 * @param {PageObject.DealsSticker} dealsSticker
 */
export default makeSuite('Стикер акции.', {
    feature: 'Стикер акции.',
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                async test() {
                    const isDealsStickerExists = await this.dealsSticker.isVisible();
                    return this.expect(isDealsStickerExists).to.equal(true, 'Стикер акции присутствует');
                },
            }),
        },
    },
});
