import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import ScrollBoxWidget from '@self/root/src/components/ScrollBox/__pageObject';
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import SnippetMetrikaSuite from '@self/root/src/spec/hermione/test-suites/blocks/metrika/snippet';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const ROOT_ZONE = 'SCROLL-BOX';

/** Проверяет метрики visible/navigate для Snippet */
module.exports = makeSuite('Snippet внутри ScrollBox', {
    params: {
        selector: 'Селектор для поиска виджета ScrollBox',
        payloadSchema: 'Схема для валидации целей метрик Snippet (js-schema)',
        goalNamePrefix: 'Префикс зоны',
        snippetIndex: 'Позиция сниппета - отсчёт от 1',
    },
    defaultParams: {
        snippetIndex: 1,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                const {selector, goalNamePrefix, payloadSchema} = this.params;

                /** докидываем зону */
                this.params.goalNamePrefix = buildGoalName(goalNamePrefix, ROOT_ZONE);

                this.params.payloadSchema = {
                    /** название виджета */
                    name: 'ScrollBox',
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
                    return this.createPageObject(ScrollBoxWidget, {
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
