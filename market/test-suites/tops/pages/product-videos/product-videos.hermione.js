import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {randomString} from '@self/root/src/helpers/string';
import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import Base from '@self/platform/spec/page-objects/n-base';
import HighratedSimilarProductsSuite from '@self/platform/spec/hermione/test-suites/blocks/HighratedSimilarProducts';
import HighratedSimilarProducts from '@self/platform/spec/page-objects/widgets/content/HighratedSimilarProducts';

const category = {
    entity: 'category',
    id: 91491,
    name: 'Мобильные телефоны',
    fullName: 'Мобильные телефоны',
    slug: 'mobilnye-telefony',
    type: 'guru',
    isLeaf: true,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Видеообзоры" карточки модели.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('SEO-разметка страницы.', {
            story: createStories(
                seoTestConfigs.pageCanonical,
                ({routeConfig, testParams, description}) => prepareSuite(BaseLinkCanonicalSuite, {
                    hooks: {
                        beforeEach() {
                            if (description === 'Тип "Визуальная"') {
                                // eslint-disable-next-line market/ginny/no-skip
                                return this.skip(
                                    'MOBMARKET-10116: Скипаем падающие автотесты, ' +
                                    'тикет на починку MOBMARKET-9726'
                                );
                            }

                            return this.browser
                                .yaSimulateBot()
                                .yaOpenPage('touch:product-videos', routeConfig);
                        },
                    },
                    pageObjects: {
                        base() {
                            return this.createPageObject(Base);
                        },
                    },
                    params: testParams.videos,
                })
            ),
        }
        ),
        makeSuite('Товар с низким рейтингом.', {
            environment: 'kadavr',
            story: prepareSuite(HighratedSimilarProductsSuite, {
                pageObjects: {
                    highratedSimilarProducts() {
                        return this.createPageObject(HighratedSimilarProducts);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const productsCount = 3;
                        const productId = routes.product.phone.productId;
                        const lowRatedProduct = {...productWithPicture};
                        lowRatedProduct.collections.product[productId].preciseRating = 3;

                        const otherProducts = [];

                        for (let i = 0; i < productsCount; i++) {
                            otherProducts.push(createProduct({
                                showUid: `${randomString()}_${i}`,
                                slug: 'test-product',
                                categories: [category],
                                preciseRating: 5,
                            }));
                        }

                        const reportState = mergeState([
                            lowRatedProduct,
                            ...otherProducts,
                        ]);

                        await this.browser.setState('report', reportState);
                        await this.browser.yaOpenPage('touch:product', routes.product.phone);
                    },
                },
                params: {
                    snippetsCount: 3,
                },
            }),
        })
    ),
});
