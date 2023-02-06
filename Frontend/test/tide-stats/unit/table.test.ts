import _ from 'lodash';
import { assert } from 'chai';
import Table from 'cli-table3';

import { createTable } from '../../../src/plugins/tide-stats/table';
import { StatsConfig } from '../../../src/plugins/tide-stats/types';

const TOOL = 'some-tool';
const COLUMN = 'some-column';

describe('tide-stats', () => {
    describe('table', () => {
        const sortPath = `${TOOL}.${COLUMN}`;

        describe('createTable', () => {
            it('should return table instance', () => {
                const result = createTable({}, { sort: sortPath } as StatsConfig);

                assert.instanceOf(result, Table);
            });
        });
    });
});
