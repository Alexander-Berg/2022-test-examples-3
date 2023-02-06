import { GOOD_COVERAGE } from './data';
import { getCoverageQueryValue } from '../utils';
import { DB_COVERAGE_COLUMNS } from '../configs';

describe('storage utils', () => {
  test('equal columns and values', () => {
    const columns = DB_COVERAGE_COLUMNS.split(', ');
    const value = getCoverageQueryValue(GOOD_COVERAGE).split(', ');

    // проверка на то что порядок колонок и проставление значений одинаковый
    columns.forEach((el, i) => {
      const v = GOOD_COVERAGE[el];
      expect(`${v}`).toBe(value[i].replace(/'/gi, ''));
    });
  });
});
