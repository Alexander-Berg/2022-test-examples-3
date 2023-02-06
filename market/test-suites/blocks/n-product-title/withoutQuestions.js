import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на точку входа на вопросы к товару в заголовке КМ
 * @property {PageObject.ProductTitle} this.productTitle
 */

export default makeSuite('Точка входа на вопросы к товару в заголовке КМ', {
    story: {
        'Если нет вопросов о товаре': {
            'ссылка отображается': makeCase({
                async test() {
                    await this.expect(
                        this.productTitle.isQaEntrypointVisible()
                    ).to.equal(true, 'Ссылка видна');
                },
            }),
        },
    },
});
