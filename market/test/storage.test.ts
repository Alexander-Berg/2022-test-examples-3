import { GOOD_COVERAGE } from './data';
import { COVERAGE_TABLE_NAME, DB_COVERAGE_COLUMNS, DB_NAMESPACE } from '../configs';
import { selectLastCoverageItem } from '../storage';

const dbOptionsMock = {
  tableName: COVERAGE_TABLE_NAME,
  tableNamespace: DB_NAMESPACE,
  columnsQuery: DB_COVERAGE_COLUMNS,
  dbClient: {
    query: () => {
      return Promise.resolve({
        rows: [GOOD_COVERAGE],
      });
    },
  },
};

describe('storage', () => {
  test('getActualCoverage', async () => {
    const actualCoverage = await selectLastCoverageItem(dbOptionsMock as any, 'ir-ui');
    expect(actualCoverage?.id).toBe(GOOD_COVERAGE.id);
  });
});
