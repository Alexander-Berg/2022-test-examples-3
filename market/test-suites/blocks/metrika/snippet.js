/**
 * Постановили, что выносим только метричный сьют сниппета
 * https://github.yandex-team.ru/market/marketplace/pull/2334#pullrequestreview-1187266
 */

import {
    mergeSuites,
    makeSuite,
    prepareSuite,
} from 'ginny';

import MetrikaVisibleSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/visible';
import MetrikaClickSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/navigate';
import Link from '@self/root/src/components/Link/__pageObject';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const ROOT_ZONE = 'SNIPPET';

/**
 * Тесты на метрики сниппета.
 * @param {PageObject.Snippet} snippet - тестируемый сниппет
 */
module.exports = makeSuite('Метрики для Snippet.', {
    params: {
        payloadSchema: 'Схема для проверки метрики',
        goalNamePrefix: 'Префикс зоны',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                const {goalNamePrefix} = this.params;

                /** докидываем зону сниппета */
                this.params.goalNamePrefix = buildGoalName(goalNamePrefix, ROOT_ZONE);
            },
        },

        /** проверяем видимость сниппета */
        prepareSuite(MetrikaVisibleSuite, {
            pageObjects: {
                testedElement() {
                    return this.snippet;
                },
            },
        }),

        /** проверяем клик по сниппету */
        prepareSuite(MetrikaClickSuite, {
            pageObjects: {
                testedElement() {
                    return this.createPageObject(Link, {parent: this.snippet});
                },
            },
        })
    ),
});
