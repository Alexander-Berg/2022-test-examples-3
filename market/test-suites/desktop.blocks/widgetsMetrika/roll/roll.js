import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import Roll from '@self/root/src/components/RollWithTitle/__pageObject';
import MetrikaVisibleSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/visible';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const ROOT_ZONE = 'ROLL';

/** Проверяет метрику visible для Roll */
module.exports = makeSuite('Roll', {
    params: {
        selector: 'Селектор для поиска виджета',
        payloadSchema: 'Схема для валидации целей метрик Roll (js-schema)',
        goalNamePrefix: 'Префикс зоны',
        customRootZone: 'Заданное название места',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                const {
                    goalNamePrefix,
                    selector,
                    customRootZone = '',
                } = this.params;

                /** докидываем зону */
                this.params.goalNamePrefix = buildGoalName(goalNamePrefix, customRootZone || ROOT_ZONE);

                /** скролим к селектору, иначе не сможем получить метрики для lazy виджета */
                await this.browser.yaSlowlyScroll(selector);
            },
        },

        /** проверяем видимость виджета */
        prepareSuite(MetrikaVisibleSuite, {
            pageObjects: {
                testedElement() {
                    return this.createPageObject(Roll, {
                        root: this.params.selector,
                    });
                },
            },
        })
    ),
});
