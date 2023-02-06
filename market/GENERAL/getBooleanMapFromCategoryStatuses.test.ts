import { CategoryStatus } from 'src/java/definitions';
import { getBooleanMapFromCategoryStatuses } from './getBooleanMapFromCategoryStatuses';

describe('getBooleanMapFromCategoryStatuses', () => {
  it('works', () => {
    expect(getBooleanMapFromCategoryStatuses([{ categoryId: 1 }, { categoryId: 2 }] as CategoryStatus[], true)).toEqual(
      {
        '1': true,
        '2': true,
      }
    );
  });
});
