import {prepareSuite, mergeSuites, makeSuite} from '@yandex-market/ginny';
import {createProduct, createRecipe, createFilter, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import LinkerIsVisibleSuite from '@self/platform/spec/hermione2/test-suites/blocks/Linker/isVisible';
// page-objects
import Linker from '@self/platform/spec/page-objects/components/Linker';

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
                    hermione.setPageObjects.call(this, {
                        linker: () => this.browser.createPageObject(Linker),
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
            prepareSuite(LinkerIsVisibleSuite)
        ),
    })
);
