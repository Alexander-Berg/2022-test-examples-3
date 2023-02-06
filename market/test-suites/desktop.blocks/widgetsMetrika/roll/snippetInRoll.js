import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import Roll from '@self/root/src/components/RollWithTitle/__pageObject';
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import SnippetMetrikaSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/snippet';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const ROOT_ZONE = 'ROLL';

/** Проверяет метрики visible/navigate для Snippet */
module.exports = makeSuite('Snippet внутри Roll', {
    params: {
        selector: 'Селектор для поиска виджета Roll',
        payloadSchema: 'Схема для валидации целей метрик Snippet (js-schema)',
        goalNamePrefix: 'Префикс зоны',
        snippetIndex: 'Позиция сниппета - отсчёт от 1',
        customRootZone: 'Заданное название места',
    },
    defaultParams: {
        snippetIndex: 1,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                const {
                    selector,
                    goalNamePrefix,
                    payloadSchema,
                    customRootZone = '',
                } = this.params;

                /** докидываем зону */
                this.params.goalNamePrefix = buildGoalName(goalNamePrefix, customRootZone || ROOT_ZONE);

                this.params.payloadSchema = {
                    /** добавляем позицию сниппета в payloadSchema */
                    position: this.params.snippetIndex,
                    /** переданная схема */
                    ...payloadSchema,
                };

                /** скролим к селектору, иначе не сможем получить метрики для lazy */
                await this.browser.yaSlowlyScroll(selector);
            },
        },

        /** проверяем метрики сниппета */
        prepareSuite(SnippetMetrikaSuite, {
            pageObjects: {
                widget() {
                    return this.createPageObject(Roll, {
                        root: this.params.selector,
                    });
                },
                snippet() {
                    return this.createPageObject(Snippet, {
                        parent: this.widget,
                        root: Snippet.getSnippetByIndex(this.params.snippetIndex),
                    });
                },
            },
        })
    ),
});
