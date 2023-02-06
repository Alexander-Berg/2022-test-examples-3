import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import SearchProductSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchProduct';
import ProductReview from '@self/platform/spec/page-objects/ProductReview';

/**
 * @param {PageObject.SearchProduct} snippetContainer Внешний контейнер в который обёрнут сниппет
 * @param {PageObject.SearchProduct} snippet непосредственнос сам сниппет с контентом
 */
export default makeSuite('Сниппет хаба отзывов.', {
    params: {
        productId: 'ID продукта, который отображен в сниппете',
        slug: 'Slug продукта',
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    review: () => this.createPageObject(ProductReview, {
                        parent: this.snippetContainer,
                    }),
                });
            },
        },

        prepareSuite(SearchProductSuite)
    ),
});
