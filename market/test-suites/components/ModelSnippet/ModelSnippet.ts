'use strict';

import {makeSuite, importSuite, mergeSuites, PageObject} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

const ModelSnippet = PageObject.get('ModelSnippet');

/**
 * Тест на блок ModelSnippet.
 * @param {PageObject.ModelSnippet} snippet
 */
export default makeSuite('Сниппет модели.', {
    issue: 'VNDFRONT-1258',
    environment: 'kadavr',
    feature: 'Отзывы на модель',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.browser.yaWaitForPageObject(ModelSnippet);
            },
        },
        importSuite('Link', {
            meta: {
                id: 'vendor_auto-152',
                issue: 'VNDFRONT-1256',
                environment: 'kadavr',
                feature: 'Отзывы на модель',
            },
            suiteName: 'Ссылка "Отзывы на Маркете"',
            params: {
                caption: 'Отзывы на Маркете',
                external: true,
                target: '_blank',
                url: buildUrl('external:market-model-reviews', {modelId: '[0-9]+'}),
                comparison: {
                    mode: 'match',
                    skipProtocol: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.snippet, this.snippet.externalLink);
                },
            },
        }),
    ),
});
