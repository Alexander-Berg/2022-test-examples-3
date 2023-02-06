import { expect } from 'chai';
import { describe, it } from 'mocha';

import { insertScopeAfterGlobals } from '../../src/utils/insertScopeAfterGlobals';

describe('insertScopeAfterGlobals', () => {
    const tests = {
        'one global classname': [
            {
                selector: '.i-ua_skin_dark',
                globals: ['i-ua_skin_dark'],
                expected: '.i-ua_skin_dark !!',
            },
            {
                selector: 'body.i-ua_skin_dark.div .test',
                globals: ['i-ua_skin_dark'],
                expected: 'body.i-ua_skin_dark.div !! .test',
            },
            {
                selector: '.i-ua_skin_dark .test .testy',
                globals: ['test'],
                expected: '.i-ua_skin_dark .test !! .testy',
            },
        ],
        complicated: [
            {
                selector: '.test:before > .test2',
                globals: ['test'],
                expected: '.test:before !! > .test2',
            },
            {
                selector: '.test[class]',
                globals: ['test'],
                expected: '.test[class] !!',
            },
            {
                selector: '.test[class] .test[s] .tests',
                globals: ['test[class]'],
                expected: '.test[class] !! .test[s] .tests', // проверка экранирования
            },
            {
                selector: '.i-ua_skin_dark~.test',
                globals: ['i-ua_skin_dark'],
                expected: '.i-ua_skin_dark !!~.test',
            },
        ],
        'multiple classnames': [
            {
                selector: '.i-ua_skin_dark .test .test2',
                globals: ['i-ua_skin_dark', 'test'],
                expected: '.i-ua_skin_dark .test !! .test2',
            },
            {
                selector: '.i-ua_skin_dark .test .i-ua_skin_dark.div',
                globals: ['i-ua_skin_dark', 'test'],
                expected: '.i-ua_skin_dark .test .i-ua_skin_dark.div !!',
            },
        ],
        rejection: [
            {
                selector: '.i-ua_skin_dark',
                globals: ['.i_ua_skin_dark'], // нужно без точки
                expected: null,
            },
            {
                selector: '.test.i-ua_skin_dark',
                globals: ['test2'],
                expected: null,
            },
            {
                selector: '.test',
                globals: [],
                expected: null,
            },
            {
                selector: 'div',
                globals: ['div'],
                expected: null,
            },
            {
                selector: '.i-ua_skin_dark .test',
                globals: ['skin_dark', 'est'],
                expected: null,
            },
        ]
    };

    for (const groupTitle of Object.keys(tests)) {
        // @ts-ignore не понимает, что индекс может быть строкой
        const groupTests = tests[groupTitle];

        describe(groupTitle, () => {
            for (const test of groupTests) {
                const { selector, globals, expected } = test;

                const action = expected ? 'insert' : 'not insert';
                const title = `should ${action} scope for "${selector}" with "${globals.join(' ')}" global classnames`;

                it(title, () => {
                    const result = insertScopeAfterGlobals(selector, '!!', globals);

                    expect(result).equal(expected);
                });
            }
        });
    }
});
