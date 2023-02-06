import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';

import productMock from './product.mock';

const productState = createProduct(productMock, productMock.id);
const expectedUrlBase = '/product--smartfon-apple-iphone-7-256gb/14206682';

export default mergeSuites(
    makeSuite('Страница карточки модели', {
        story: createStories(
            {
                main: {
                    description: 'Описание',
                    url: 'market:product',
                    expectedUrl: expectedUrlBase,
                },
                spec: {
                    description: 'Характеристики',
                    url: 'market:product-spec',
                    expectedUrl: `${expectedUrlBase}/spec`,
                },
                offers: {
                    description: 'Цены',
                    url: 'market:product-offers',
                    expectedUrl: `${expectedUrlBase}/offers`,
                },
                reviews: {
                    description: 'Отзывы',
                    url: 'market:product-reviews',
                    expectedUrl: `${expectedUrlBase}/reviews`,
                },
                /*
                // Прячем до лучших времён см.MARKETFRONT-76293
                articles: {
                    description: 'Обзоры',
                    url: 'market:product-articles',
                    expectedUrl: `${expectedUrlBase}/articles`,
                },
                */
            },
            ({url, expectedUrl}) => prepareSuite(CanonicalUrlSuite, {
                pageObjects: {
                    canonicalUrl() {
                        return this.createPageObject(CanonicalUrl);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', productState);
                        return this.browser.yaOpenPage(url, {
                            productId: productMock.id,
                            slug: productMock.slug,
                        });
                    },
                },
                params: {expectedUrl},
            })
        ),
    })
);
