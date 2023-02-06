import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductArticleList} productArticleList
 */
export default makeSuite('Список обозоров', {
    story: {
        'По умолчанию': {
            'отображается.': makeCase({
                id: 'm-touch-2962',
                issue: 'MOBMARKET-13174',
                test() {
                    return this.productArticleList.isVisible()
                        .should.eventually.to.be.equal(true, 'Виджет отображается');
                },
            }),
            'первая статья': {
                'содержит ссылку на сторонний сайт.': makeCase({
                    id: 'm-touch-2964',
                    issue: 'MOBMARKET-13175',
                    async test() {
                        const url = await this.productArticleList.firstProductArticleLink.getAttribute('href');
                        return this.expect(url).to.not.contain('yandex.ru');
                    },
                }),
            },
        },
    },
});
