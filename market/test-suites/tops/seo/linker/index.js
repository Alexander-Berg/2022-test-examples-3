import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct, createRecipe, createFilter, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import LinkerIsVisibleSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/isVisible';
import LinkerFiltersSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/filters';
import LinkerCorrectSequenceSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/correctSequence';
import LinkerCountsSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/counts';
import LinkerSeoSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/seo';
import LinkerMoreSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/more';
import LinkerUrlParamsSuite from '@self/platform/spec/hermione/test-suites/blocks/Linker/urlParams';
import ProductRecipesListChoiceItemSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-product-recipes-list/choice-item';
// page-objects
import Linker from '@self/platform/spec/page-objects/components/Linker';
import PopularRecipes
    from '@self/root/src/widgets/content/PopularRecipes/components/PopularRecipes/__pageObject/index.desktop';
import ProductRecipes from '@self/platform/spec/page-objects/n-w-product-recipes';
import ProductRecipesList from '@self/platform/spec/page-objects/n-w-product-recipes-list';
import Title from '@self/root/src/widgets/content/search/Title/components/Title/__pageObject';

import recipesMock from './recipes.mock';
import productMock from './product.mock';

const productState = createProduct(productMock, productMock.id);
const {id: filterId, ...otherFilterParams} = recipesMock[0].filters[0];
const filterState = createFilter(otherFilterParams, filterId);

export default mergeSuites(
    makeSuite('Страница карточки модели', {
        environment: 'kadavr',
        story: mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        linker: () => this.createPageObject(Linker),
                    });

                    const recipesState = recipesMock
                        .slice(0, 15)
                        .map(({id, entity, ...other}) => createRecipe(other, id));
                    const state = mergeState([productState, ...recipesState, filterState]);
                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('market:product', {
                        productId: productMock.id,
                        slug: productMock.slug,
                    });
                },
            },
            prepareSuite(LinkerIsVisibleSuite),
            prepareSuite(LinkerCorrectSequenceSuite, {
                params: {
                    recipes: recipesMock.slice(0, 15).map(({name}) => name),
                },
            }),
            prepareSuite(LinkerCountsSuite, {
                params: {
                    counts: 15,
                },
            }),
            prepareSuite(LinkerFiltersSuite),
            prepareSuite(LinkerSeoSuite, {
                pageObjects: {
                    title() {
                        return this.createPageObject(Title);
                    },
                },
            })
        ),
    }),
    makeSuite('Страница выдачи', {
        environment: 'kadavr',
        story: mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        linker: () => this.createPageObject(PopularRecipes, {
                            root: PopularRecipes.rootModeSearch,
                        }),
                    });

                    const recipesState = recipesMock
                        .map(({id, entity, ...other}) => createRecipe(other, id));
                    const state = mergeState([productState, ...recipesState, filterState]);
                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('market:list', routes.list.phones);
                },
            },
            prepareSuite(LinkerIsVisibleSuite, {
                meta: {
                    id: 'marketfront-1756',
                    issue: 'MARKETVERSTKA-26382',
                },
            }),
            prepareSuite(LinkerMoreSuite),
            prepareSuite(LinkerUrlParamsSuite, {
                params: {
                    query: {
                        track: 'recipe_from_listing',
                    },
                },
            }),
            prepareSuite(LinkerSeoSuite, {
                meta: {
                    id: 'marketfront-1756',
                    issue: 'MARKETVERSTKA-26382',
                },
                pageObjects: {
                    title() {
                        return this.createPageObject(Title);
                    },
                },
            })
        ),
    }),
    makeSuite('Страница отзыва о товаре', {
        environment: 'kadavr',
        story: mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        productRecipesList: () => this.createPageObject(ProductRecipesList),
                        title: () => this.createPageObject(Title),
                    });

                    const recipesState = recipesMock
                        .slice(0, 1)
                        .map(({id, entity, ...other}) => createRecipe(other, id));
                    const state = mergeState([productState, ...recipesState, filterState]);
                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('market:product-spec', {
                        productId: productMock.id,
                        slug: productMock.slug,
                    });
                },
            },
            prepareSuite(ProductRecipesListChoiceItemSuite, {
                params: {
                    filterId,
                },
            })
        ),
    }),
    makeSuite('Страница характеристик КМ', {
        environment: 'kadavr',
        story: mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        linker: () => this.createPageObject(ProductRecipes),
                    });

                    const recipesState = recipesMock
                        .slice(0, 12)
                        .map(({id, entity, ...other}) => createRecipe(other, id));
                    const state = mergeState([productState, ...recipesState, filterState]);
                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('market:product-spec', {
                        productId: productMock.id,
                        slug: productMock.slug,
                    });
                },
            },
            prepareSuite(LinkerIsVisibleSuite, {
                meta: {
                    id: 'marketfront-2575',
                    issue: 'MARKETVERSTKA-26399',
                },
                params: {
                    visibleLinks: 5,
                },
                pageObjects: {
                    linker() {
                        return this.createPageObject(ProductRecipes, {
                            root: ProductRecipesList.root,
                        });
                    },
                },
            })
        ),
    })
);
