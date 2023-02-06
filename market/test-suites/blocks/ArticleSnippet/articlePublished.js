import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ArticleSnippet} articleSnippet
 */
export default makeSuite('Сниппет опубликованной статьи.', {
    story: {
        'По умолчанию': {
            'ссылка на публикацию ведет на публикацию статьи': makeCase({
                issue: 'MARKETVERSTKA-31024',
                id: 'marketfront-2840',
                feature: 'Проверка ссылок',
                async test() {
                    const articleHref = await this.articleSnippet.getArticleHref();
                    const expectedHref = await this.browser.yaBuildURL(
                        'market:journal-article',
                        {type: 'brand', semanticId: 'kak-delat-dela'}
                    );
                    return this.expect(articleHref)
                        .to.be.link(expectedHref, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            })},
    },
});
