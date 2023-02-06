import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';

import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';

import productMedicineMock from './mocks/product-medicine.mock';
import navigationTreeMedicineMock from './mocks/navigation-tree-medicine.mock';

export default makeSuite('Страница лекарственной карточки модели', {
    environment: 'kadavr',
    story: createStories(
        [seoTestConfigs.pageMedicine.spec],
        ({url, expectedTitle, titleMatchMode}) => mergeSuites(
            {
                async beforeEach() {
                    const question = createQuestion({
                        product: {
                            entity: 'product',
                            id: productMedicineMock.id,
                        },
                    });
                    const schema = {
                        modelQuestions: [question],
                        modelOpinions: [{
                            product: {id: 100500},
                            region: {id: 1},
                        }],
                    };
                    const productState = createProduct(productMedicineMock, productMedicineMock.id);
                    await this.browser.setState('Cataloger.tree', navigationTreeMedicineMock);
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productState);

                    return this.browser.yaOpenPage(url, {
                        productId: productMedicineMock.id,
                        slug: productMedicineMock.slug,
                    });
                },
            },
            prepareSuite(PageTitleSuite, {
                params: {
                    expectedTitle,
                    matchMode: titleMatchMode,
                },
                meta: {
                    id: 'm-touch-1037',
                    issue: 'MARKETFRONT-40708',
                },
            })
        )
    ),
});
