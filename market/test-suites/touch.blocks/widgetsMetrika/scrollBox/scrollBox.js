import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import ScrollBoxWidget from '@self/root/src/components/ScrollBox/__pageObject';
import MetrikaVisibleSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/visible';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const ROOT_ZONE = 'SCROLL-BOX';

/** Проверяет метрику visible для ScrollBox */
module.exports = makeSuite('ScrollBox', {
    params: {
        selector: 'Селектор для поиска виджета',
        payloadSchema: 'Схема для валидации целей метрик ScrollBox (js-schema)',
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
                    name: 'ScrollBox',
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
                    return this.createPageObject(ScrollBoxWidget, {
                        root: this.params.selector,
                    });
                },
            },
        })
    ),
});
