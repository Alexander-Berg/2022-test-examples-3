import _ from 'lodash';
import sinon from 'sinon';
import { assert } from 'chai';

import { prepareTests, getSortPath } from '../../../src/plugins/tide-stats/utils';
import { COLUMN_TITLES, STATS_KEYS } from '../../../src/plugins/tide-stats/constants';
import { StatsConfig } from '../../../src/plugins/tide-stats/types';
import * as testpalmConstants from '../../../src/parsers/tide-testpalm-parser/constants';
import { TideConstants } from '../../../src';

const TOOL = 'some-tool';

describe('tide-stats', () => {
    let testCollectionStub;
    const constants = { testpalm: testpalmConstants };

    beforeEach(() => {
        testCollectionStub = {
            filterTests: sinon.stub().returnsThis(),
            groupTests: sinon.stub().returns({}),
        };
    });

    describe('utils', () => {
        describe('prepareTests', () => {
            it('should group tests', () => {
                prepareTests(testCollectionStub, {} as StatsConfig, constants as TideConstants);

                sinon.assert.calledOnce(testCollectionStub.groupTests);
            });

            it('should not filter tests without type option', () => {
                prepareTests(testCollectionStub, {} as StatsConfig, constants as TideConstants);

                sinon.assert.notCalled(testCollectionStub.filterTests);
            });

            it('should filter tests with type option', () => {
                prepareTests(
                    testCollectionStub,
                    { type: 'some-type' } as StatsConfig,
                    constants as TideConstants,
                );

                sinon.assert.calledOnce(testCollectionStub.filterTests);
            });
        });

        describe('getSortPath', () => {
            const expectedTool = TOOL;
            const expectedColumn = STATS_KEYS[COLUMN_TITLES.TESTS];
            const sortPath = `${expectedTool}.${expectedColumn}`;

            it('should return null, if there is no sort option', () => {
                const result = getSortPath({} as StatsConfig, constants as TideConstants);

                assert.equal(result, null);
            });

            it('should return string with specific value', () => {
                const result = getSortPath(
                    { sort: sortPath } as StatsConfig,
                    constants as TideConstants,
                );

                assert.equal(result, sortPath);
            });

            it('should return string with tool from tool option, if it is not in sort option and group option is not tool', () => {
                const result = getSortPath(
                    { sort: expectedColumn, tool: expectedTool } as StatsConfig,
                    constants as TideConstants,
                );

                assert.equal(result, [expectedTool, expectedColumn].join('.'));
            });

            it('should return string with default tool, if it is not in tool and sort options', () => {
                const result = getSortPath(
                    { sort: expectedColumn } as StatsConfig,
                    constants as TideConstants,
                );

                assert.equal(result, [constants.testpalm.TOOL, expectedColumn].join('.'));
            });

            it('should return string with default tool, if it is in tool option, but group option is tool', () => {
                const result = getSortPath(
                    { sort: expectedColumn, tool: expectedTool, group: 'tool' } as StatsConfig,
                    constants as TideConstants,
                );

                assert.equal(result, [constants.testpalm.TOOL, expectedColumn].join('.'));
            });
        });
    });

    afterEach(() => {
        sinon.reset();
    });
});
