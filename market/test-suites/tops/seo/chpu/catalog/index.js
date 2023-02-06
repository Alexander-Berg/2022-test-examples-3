import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createProduct} from '@self/platform/spec/hermione/helpers/shopRating';
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import NavigationTreeChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/NavigationTree/chpu';
import ClarifyCategoryChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/ClarifyCategory/chpu';
import SnippetCardChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/snippet-card/chpu';
import SnippetCellChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/snippet-cell/chpu';
import PopularBrandsChpuCatalogSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-popular-brands/chpu-catalog';
// page-objects
import Navigation from '@self/platform/spec/page-objects/Navigation';
import NavigationDepartment from '@self/platform/spec/page-objects/NavigationDepartment';
import ClarifyCategory from '@self/platform/spec/page-objects/ClarifyCategory';
import SnippetCard from '@self/platform/spec/page-objects/snippet-card';
import SnippetCell from '@self/platform/spec/page-objects/snippet-cell';
import PopularBrands from '@self/platform/spec/page-objects/components/PopularBrands';

const CATALOG_ID = 54440;
const CATALOG_SLUG = 'elektronika';

async function createAndSetState(browser) {
    const productId = 1722193751;
    const productSlug = 'smartfon-samsung-galaxy-s8';

    const product = createProduct({slug: productSlug}, productId);

    await browser.setState('report', mergeState([
        {
            data: {
                search: {
                    // нужно чтобы отображался список
                    total: 1,
                },
            },
        },
        product,
    ]));

    await browser.setState('persBasket', {
        items: [
            {
                'type': 'MODEL',
                'id': productId,
                'displayName': 'Смартфон Samsung Galaxy S8 черный бриллиант',
                'modelId': productId,
                'hid': CATALOG_ID,
            },
        ],
    });
}

export default makeSuite('Департамент', {
    story: mergeSuites(
        makeSuite('Страница Каталога.', {
            environment: 'testing',
            story: prepareSuite(NavigationTreeChpuSuite, {
                pageObjects: {
                    navigation() {
                        return this.createPageObject(Navigation);
                    },
                    navigationDepartment() {
                        return this.createPageObject(NavigationDepartment, {parent: this.navigation});
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:catalog', {nid: CATALOG_ID, slug: CATALOG_SLUG});
                    },
                },
            }),
        }),
        makeSuite('Страница Поиска.', {
            environment: 'testing',
            story: prepareSuite(ClarifyCategoryChpuSuite, {
                pageObjects: {
                    clarifyCategory() {
                        return this.createPageObject(ClarifyCategory);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:search', routes.search.cats);
                    },
                },
            }),
        }),
        makeSuite('Страница Избранное.', {
            environment: 'kadavr',
            story: prepareSuite(SnippetCardChpuSuite, {
                pageObjects: {
                    snippetCard() {
                        return this.createPageObject(SnippetCard);
                    },
                    snippetCell() {
                        return this.createPageObject(SnippetCell);
                    },
                },
                hooks: {
                    async beforeEach() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETFRONT-2227 скипаем упавшие тесты для озеленения');

                        /* eslint-disable no-unreachable */
                        await this.browser.yaProfile('ugctest3');
                        await createAndSetState(this.browser);

                        return this.browser.yaOpenPage('market:wishlist');
                        /* eslint-enable no-unreachable */
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
            }),
        }),
        makeSuite('Страница КМ.', {
            environment: 'testing',
            story: prepareSuite(SnippetCellChpuSuite, {
                pageObjects: {
                    snippetCell() {
                        return this.createPageObject(SnippetCell, {
                            root: '[data-bem*="model_card_accessory_snippet_click"]',
                        });
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:product', routes.product.withColors);
                    },
                },
                params: {
                    title: 'С этим товаром часто покупают',
                },
            }),
        }),
        makeSuite('Страница Брендов.', {
            environment: 'testing',
            story: prepareSuite(PopularBrandsChpuCatalogSuite, {
                pageObjects: {
                    popularBrands() {
                        return this.createPageObject(PopularBrands);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:brands-list');
                    },
                },
            }),
        }),
        makeSuite('Страница брендов на букву А.', {
            environment: 'testing',
            story: prepareSuite(PopularBrandsChpuCatalogSuite, {
                pageObjects: {
                    popularBrands() {
                        return this.createPageObject(PopularBrands);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:brands-list', {char: 'a'});
                    },
                },
            }),
        })
    ),
});
