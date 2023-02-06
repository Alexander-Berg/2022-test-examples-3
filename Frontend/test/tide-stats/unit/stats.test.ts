import _ from 'lodash';
import { assert } from 'chai';

import Stats from '../../../src/plugins/tide-stats/stats';
import { ROW_TITLES } from '../../../src/plugins/tide-stats/constants';

const TOOL = 'some-tool';

describe('tide-stats', () => {
    describe('stats', () => {
        let tests;
        let stats;

        beforeEach(() => {
            tests = {
                'group-2': { [TOOL]: { tests: [{}, {}] } },
                'group-0': { [TOOL]: { tests: [] } },
                'group-1': { [TOOL]: { tests: [{}] } },
            };
        });

        describe('sort', () => {
            it('should not sort test stats, if there is no sort path argument', () => {
                stats = new Stats(tests);

                stats.sort();

                assert.deepEqual(stats._groups, ['group-2', 'group-0', 'group-1']);
            });

            it('should sort test stats', () => {
                stats = new Stats(tests);

                stats.sort(`${TOOL}.tests`);

                assert.deepEqual(stats._groups, ['group-2', 'group-1', 'group-0']);
            });

            it('should leave total group at the end of test stats', () => {
                tests[ROW_TITLES.TOTAL] = { [TOOL]: { tests: [{}, {}, {}] } };
                stats = new Stats(tests);

                stats.sort(`${TOOL}.tests`);

                assert.deepEqual(stats._groups, [
                    'group-2',
                    'group-1',
                    'group-0',
                    ROW_TITLES.TOTAL,
                ]);
            });
        });

        describe('take', () => {
            it('should not shorten test stats, if there is no max count argument', () => {
                stats = new Stats(tests);

                stats.take();

                assert.lengthOf(stats._groups, 3);
            });

            it('should shorten test stats to defined size', () => {
                stats = new Stats(tests);

                stats.take(2);

                assert.lengthOf(stats._groups, 2);
            });

            it('should leave total group in test stats', () => {
                tests[ROW_TITLES.TOTAL] = { [TOOL]: { tests: [{}, {}, {}] } };
                stats = new Stats(tests);

                stats.take(2);

                assert.lengthOf(stats._groups, 3);
            });
        });

        describe('getResult', () => {
            it('should return test stats with numeric data', () => {
                stats = new Stats(tests);

                const result = stats.getResult();

                assert.deepEqual(result, {
                    'group-2': { [TOOL]: { Tests: 2 } },
                    'group-0': { [TOOL]: { Tests: 0 } },
                    'group-1': { [TOOL]: { Tests: 1 } },
                });
            });

            it('should return test stats without undefined keys', () => {
                tests.undefined = {};
                stats = new Stats(tests);

                const result = stats.getResult();

                assert.doesNotHaveAnyKeys(result, ['undefined']);
            });
        });
    });
});
