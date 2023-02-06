import {makeCase, makeSuite} from 'ginny';
import ProductSnippet from '@self/platform/spec/page-objects/ProductSnippet';

/**
 * @param {PageObject.ProductSnippet} productSnippet
 * @param {PageObject.QuestionCard} questionCard
 */
export default makeSuite('Сниппет продукта.', {
    params: {
        productId: 'id продукта текущего вопроса',
        slug: 'Slug продукта',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                productSnippet: () => this.createPageObject(ProductSnippet, {
                    parent: ProductSnippet.TitleCard,
                }),
            });
        },

        'по умолчанию': {
            'содержит ссылку на страницу вопросов продукта': makeCase({
                id: 'm-touch-2237',
                issue: 'MOBMARKET-9084',
                feature: 'Хлебные крошки',
                async test() {
                    const expectedLink = await this.browser.yaBuildURL(
                        'touch:product-questions',
                        {
                            productId: this.params.productId,
                            slug: this.params.slug,
                        });
                    const actualLink = await this.productSnippet.snippetLinkHref;
                    await this.expect(actualLink, 'Ссылка корректная')
                        .to.be.link({pathname: expectedLink}, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
