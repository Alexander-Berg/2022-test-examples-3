import { getCurrentTotal } from '../utils';
import { compareCoverages, getDecreasedText } from '../diff';
import { GOOD_TOTAL_COVERAGE, BAD_TOTAL_COVERAGE, GOOD_COVERAGE_PATH } from './data';
import { mapToDBItem } from '../storage';

describe('coverage checker utils', () => {
  test('save subproject in current coverage', async () => {
    const coverage = getCurrentTotal(GOOD_COVERAGE_PATH);
    const coverageDb = mapToDBItem(coverage, {
      reportPath: GOOD_COVERAGE_PATH,
      projectName: 'yang-react',
      subProject: 'common-logs',
      envBranchName: 'master',
    });
    expect(coverageDb.subproject).toBe('common-logs');
  });
  test('compareCoverages good coverage', () => {
    const result = compareCoverages(GOOD_TOTAL_COVERAGE, GOOD_TOTAL_COVERAGE);
    expect(result).toBe(undefined);
  });

  test('compareCoverages full low coverage', () => {
    const result = compareCoverages(GOOD_TOTAL_COVERAGE, BAD_TOTAL_COVERAGE);
    expect(result?.length).toBe(3);
  });

  test('compareCoverages good coverage with deviation', () => {
    const result = compareCoverages(
      GOOD_TOTAL_COVERAGE,
      {
        ...GOOD_TOTAL_COVERAGE,
        // понижаем покрытие
        branches: {
          ...GOOD_TOTAL_COVERAGE.branches,
          pct: GOOD_TOTAL_COVERAGE.branches.pct - 1,
        },
      },
      1
    );
    // процент понизился но из-за допустимого отклонения в 1% предупреждения не должно быть
    expect(result).toBe(undefined);
  });

  test('compareCoverages low coverage', () => {
    const lowCoveragePercent = GOOD_TOTAL_COVERAGE.branches.pct - 2;
    const result = compareCoverages(GOOD_TOTAL_COVERAGE, {
      ...GOOD_TOTAL_COVERAGE,
      // понижаем ппокрытие
      branches: {
        ...GOOD_TOTAL_COVERAGE.branches,
        pct: lowCoveragePercent,
      },
    });
    expect(result?.[0]).toBe(getDecreasedText('branches', GOOD_TOTAL_COVERAGE.branches.pct, lowCoveragePercent));
  });

  test('getCurrentCoverageReport', () => {
    const currentCoverage = getCurrentTotal('./src/test/files/good-coverage-report.json');
    expect(currentCoverage).toBeTruthy();
  });
});
