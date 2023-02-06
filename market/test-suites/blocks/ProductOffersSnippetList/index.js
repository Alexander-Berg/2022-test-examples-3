import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на виджет ProductOffersStaticList.
 *
 * @property {PageObject.ProductOffersSnippetList} this.productOffersSnippetList
 */
export default makeSuite('Блок Топ 6 предложений', {
    story: {
        'По умолчанию': {
            'отображается на странице': makeCase({
                id: 'm-touch-2883',
                issue: 'MOBMARKET-12583',

                async test() {
                    return this.productOffersSnippetList
                        .isVisible()
                        .should.eventually.to.be.equal(true, 'Блок Топ 6 предложений должен отображаться');
                },
            }),
        },
    },
});
