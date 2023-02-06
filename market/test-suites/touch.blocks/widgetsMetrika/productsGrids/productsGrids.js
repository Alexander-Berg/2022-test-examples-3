import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import ProductsGridsWidget from '@self/root/src/widgets/parts/ProductsGrids/components/ProductsGrids/__pageObject';
import MetrikaVisibleSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/visible';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const ROOT_ZONE = 'PRODUCTS-GRIDS';

/** Проверяет метрику visible для ProductsGrids */
module.exports = makeSuite('ProductsGrids', {
    params: {
        selector: 'Селектор для поиска виджета',
        payloadSchema: 'Схема для валидации целей метрик ProductsGrids (js-schema)',
        goalNamePrefix: 'Префикс зоны',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                const {goalNamePrefix, selector, payloadSchema} = this.params;

                /** докидываем зону */
                this.params.goalNamePrefix = buildGoalName(goalNamePrefix, ROOT_ZONE);

                this.params.payloadSchema = {
                    /** название виджета */
                    name: 'ProductsGrids',
                    /** переданная схема */
                    ...payloadSchema,
                };

                /** скролим к селектору, иначе не сможем получить метрики для lazy виджета */
                await this.browser.yaSlowlyScroll(selector);
            },
        },

        /** проверяем видимость виджета */
        prepareSuite(MetrikaVisibleSuite, {
            pageObjects: {
                testedElement() {
                    return this.createPageObject(ProductsGridsWidget, {
                        root: this.params.selector,
                    });
                },
            },
        })
    ),
});
