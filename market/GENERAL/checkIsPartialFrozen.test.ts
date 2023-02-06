import { YangTasksInfo } from 'src/java/definitions';
import { checkIsPartialFrozen } from './checkIsPartialFrozen';

describe('checkIsPartialFrozen', () => {
  it('works true', () => {
    expect(checkIsPartialFrozen({ frozenOffersGroups: [{}] } as YangTasksInfo)).toEqual(true);
  });
  it('works false', () => {
    expect(checkIsPartialFrozen({} as YangTasksInfo)).toEqual(false);
  });
});
