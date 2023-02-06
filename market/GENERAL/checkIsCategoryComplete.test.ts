import { CategoryStatus } from 'src/java/definitions';
import { checkIsCategoryComplete } from 'src/pages/TaskStatuses/utils/checkIsCategoryComplete';

describe('checkIsCategoryComplete', () => {
  it('works true', () => {
    expect(checkIsCategoryComplete({ activeItems: 0 } as CategoryStatus)).toEqual(true);
  });
  it('works false', () => {
    expect(checkIsCategoryComplete({ activeItems: 1 } as CategoryStatus)).toEqual(false);
  });
});
