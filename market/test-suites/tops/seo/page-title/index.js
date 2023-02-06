import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';

import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';

import productMock from './product.mock';

export default mergeSuites(
    makeSuite('Страница карточки модели', {
        story: createStories(
            seoTestConfigs.pageTitle,
            ({
                url,
                testParams,
                matchMode,
            }) => createStories(testParams,
                ({type, expectedTitle, additionalProductProps}) => prepareSuite(PageTitleSuite, {
                    hooks: {
                        async beforeEach() {
                            productMock.type = type;
                            const productState = createProduct(
                                Object.assign({}, productMock, additionalProductProps),
                                productMock.id
                            );
                            await this.browser.setState('report', productState);
                            return this.browser.yaOpenPage(url, {
                                productId: productMock.id,
                                slug: productMock.slug,
                            });
                        },
                    },
                    params: {expectedTitle, matchMode},
                }))
        ),
    })
);
