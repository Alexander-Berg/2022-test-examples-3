import {makeSuite, makeCase} from 'ginny';

/**
 * @property {PageObject.ProductCardHeader} productCardHeader
 */
export default makeSuite('Точка входа на вопросы к товару в заголовке КМ', {
    story: {
        'Если нет вопросов о товаре': {
            'ссылка не отображается':
                makeCase({
                    id: 'm-touch-2984',
                    issue: 'MOBMARKET-13320',
                    feature: 'Структура страницы',
                    async test() {
                        await this.expect(
                            this.productCardHeader.isQaEntrypointVisible()
                        ).to.equal(false, 'Ссылка не видна');
                    },
                }),
        },
    },
});
