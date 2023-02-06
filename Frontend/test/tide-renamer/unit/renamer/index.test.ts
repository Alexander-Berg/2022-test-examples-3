import _ from 'lodash';
import 'mocha';
import { expect } from 'chai';
import type { Test } from '../../../../src';
import { getAffectedTests } from '../../../../src/plugins/tide-renamer/renamer';

describe('tide-renamer / renamer / index', () => {
    describe('getTestsToRename', () => {
        const testFiles: { filePath: string; tests: Array<object> }[] = [
            { filePath: '/dir/a.hermione.js', tests: [] },
            { filePath: '/dir/a.testpalm.yml', tests: [] },
            { filePath: '/dir/b.hermione.e2e.js', tests: [] },
            { filePath: '/dir/b.testpalm.yml', tests: [] },
        ];
        const tests = [
            {
                tools: new Set(['hermione', 'testpalm']),
                titlePath: ['Саджест / Счетчики', 'Safeclick', 'Проверка safeclick'],
                fullTitle: (): string => 'Саджест / Счетчики Safeclick Проверка safeclick',
                files: { hermione: testFiles[0], testpalm: testFiles[1] },
            },
            {
                tools: new Set(['testpalm']),
                titlePath: ['Саджест / Счетчики', 'Part-2', 'Part-N-1'],
                fullTitle: (): string => 'Саджест / Счетчики Part-2 Part-N-1',
                files: { testpalm: testFiles[1] },
            },
            {
                tools: new Set(['hermione']),
                titlePath: [
                    'Саджест / Счетчики',
                    'Should be added because of test file',
                    'Some value',
                ],
                fullTitle: (): string =>
                    'Саджест / Счетчики Should be added because of test file Some value',
                files: { hermione: testFiles[0] },
            },
            {
                tools: new Set(['testpalm']),
                titlePath: ['Саджест / Счетчики', 'Part-3', 'Part-A-9'],
                fullTitle: (): string => 'Саджест / Счетчики Part-3 Part-A-9',
                files: { testpalm: testFiles[1] },
            },
            {
                tools: new Set(['hermione', 'testpalm']),
                titlePath: ['Саджест / Счетчики', 'no-match', 'no-match'],
                fullTitle: (): string => 'Саджест / Счетчики no-match no-match',
                files: { hermione: testFiles[2] },
            },
            {
                tools: new Set(['hermione', 'testpalm']),
                titlePath: ['Саджест / Счетчики', 'no-match-2', 'no-match-3'],
                fullTitle: (): string => 'Саджест / Счетчики no-match-2 no-match-3',
                files: { hermione: testFiles[2], testpalm: testFiles[3] },
            },
        ];
        testFiles[0].tests.push(...[tests[0], tests[2]]);
        testFiles[1].tests.push(...[tests[0], tests[1], tests[3]]);
        testFiles[2].tests.push(...[tests[4], tests[5]]);
        testFiles[3].tests.push(...[tests[5]]);

        const constants = { hermione: { TITLE_KEYS: ['feature', 'type', 'experiment'] } };

        [
            {
                name: 'should get tests to rename by updates array containing regexp objects',
                updates: [
                    {
                        from: { feature: /(Саджест)/, type: /(Счетчики)/ },
                        to: { feature: '$1', type: '$1' },
                    },
                    { from: /Part-(\d+)/, to: 'Shard-$1' },
                    { from: /Part-(\w+)-(\d+)/, to: 'Code-$1:$2' },
                ],
                expectedTests: [tests[0], tests[1], tests[2], tests[3]],
            },
            {
                name: 'should get tests to rename when update is an object',
                updates: {
                    from: /Саджест \/ Счетчики no-match(-\d+)? no-match(-\d+)?/,
                    to: 'Саджест / Счетчики match$1 no-match$2',
                },
                expectedTests: [tests[4], tests[5]],
            },
        ].forEach((test) =>
            it(test.name, () => {
                const actualTests = getAffectedTests(
                    test.updates,
                    tests as unknown as Test[],
                    constants,
                );

                expect(actualTests.length).equal(test.expectedTests.length);
                for (const expectedTest of test.expectedTests) {
                    expect(
                        _.findIndex(actualTests, (actualTest) =>
                            _.isEqual(expectedTest, actualTest),
                        ),
                    ).not.equal(-1);
                }
            }),
        );
    });
});
