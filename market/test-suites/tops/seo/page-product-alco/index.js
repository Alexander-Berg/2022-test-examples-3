import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';

import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';

import productAlcoMock from './mocks/product-alco.mock';
import navigationTreeAlcoMock from './mocks/navigation-tree-alco.mock';

export default makeSuite('Страница алкогольной карточки модели', {
    environment: 'kadavr',
    story: createStories(
        seoTestConfigs.pageAlco,
        ({url, expectedDescription, expectedTitle, titleMatchMode, descriptionMatchMode}) => mergeSuites(
            {
                async beforeEach() {
                    const question = createQuestion({
                        product: {
                            entity: 'product',
                            id: productAlcoMock.id,
                        },
                    });
                    const schema = {
                        modelQuestions: [question],
                        modelOpinions: [{
                            product: {id: 100500},
                            region: {id: 1},
                        }],
                    };
                    const productState = createProduct(productAlcoMock, productAlcoMock.id);
                    await this.browser.setState('Cataloger.tree', navigationTreeAlcoMock);
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', productState);

                    return this.browser.yaOpenPage(url, {
                        productId: productAlcoMock.id,
                        slug: productAlcoMock.slug,
                    });
                },
            },
            prepareSuite(PageTitleSuite, {
                params: {
                    expectedTitle,
                    matchMode: titleMatchMode,
                },
            }),
            prepareSuite(PageDescriptionSuite, {
                pageObjects: {
                    pageMeta() {
                        return this.createPageObject(PageMeta);
                    },
                },
                params: {
                    expectedDescription,
                    matchMode: descriptionMatchMode,
                },
            })
        )
    ),
});
