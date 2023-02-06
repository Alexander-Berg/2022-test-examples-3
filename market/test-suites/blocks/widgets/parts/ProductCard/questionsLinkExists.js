import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ProductCard} productCard
 */
export default makeSuite('Ссылка «Вопросы о товаре», отображается', {
    story: {
        'По умолчанию': {
            'ссылка отображается':
                makeCase({
                    id: 'm-touch-2495',
                    issue: 'MOBMARKET-10413',
                    async test() {
                        await this.expect(this.productCardLinks.isQuestionsLinkVisible()).to.equal(true);
                    },
                }),
        },
    },
});
