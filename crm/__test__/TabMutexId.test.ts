import { TabMutexId } from '../TabMutexId';

describe('TabMutexId', () => {
  describe('.constructor', () => {
    it('returns correct id', () => {
      const tabMutexId = new TabMutexId('id');
      expect(tabMutexId.id).toBe(TabMutexId.PREFIX + 'id');
    });
  });

  describe('.toString', () => {
    it('returns correct id', () => {
      const tabMutexId = new TabMutexId('id');
      expect(tabMutexId.toString()).toBe(TabMutexId.PREFIX + 'id');
    });
  });
});
