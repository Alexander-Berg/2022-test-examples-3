import {makeSuite, makeCase} from 'ginny';

import SearchPager from '@self/platform/widgets/content/search/Pager/__pageObject';
import Preloadable from '@self/platform/spec/page-objects/preloadable';

import cookieGenerator from './helpers/cookieGenerator';

const additionalParams = {
    lr: 213,
    viewtype: 'list',
};

/**
 * Сценарии для интеграционных тестов
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Сценарии для интеграционных тестов', {
    issue: 'AUTOTESTMARKET-6384',
    feature: 'Сценарии для интеграционных тестов',
    story: {
        beforeEach() {
            this.setPageObjects({
                pager: () => this.createPageObject(SearchPager),
                preloadable: () => this.createPageObject(Preloadable),
            });

            return this.browser
                .yaOpenPage('market:index')
                .then(() => {
                    this.yandexuidCookie = cookieGenerator.generateYandexUidCookie();
                    return this.browser.yaSetCookie(this.yandexuidCookie);
                })
                .then(() => this.browser.yaSetCookie(cookieGenerator.generateBackendCookie()));
        },

        '1.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario1', additionalParams)
                    .then(() => addOutput.call(this, 'scenario1'));
            },
        }),

        '2.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario2', additionalParams)
                    .then(() => addOutput.call(this, 'scenario2'));
            },
        }),

        '3.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario3', additionalParams)
                    .then(() => addOutput.call(this, 'scenario3'));
            },
        }),

        '4.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario4', additionalParams)
                    .then(() => addOutput.call(this, 'scenario4'));
            },
        }),

        '5.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario5', additionalParams)
                    .then(() => addOutput.call(this, 'scenario5'));
            },
        }),

        '6.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario6', additionalParams)
                    .then(() => addOutput.call(this, 'scenario6'));
            },
        }),

        '7.': makeCase({
            test() {
                return this.browser
                    .yaScenario(this, 'search.scenario7', additionalParams)
                    .then(() => addOutput.call(this, 'scenario7'));
            },
        }),
    },
});

let addOutput = function (scenarioName) {
    return this.extendOutputObj({[this.yandexuidCookie.value]: scenarioName});
};
