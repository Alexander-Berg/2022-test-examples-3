import { TabMutexId } from '../TabMutexId';
import { TabMutexCleaner } from '../TabMutexCleaner';
import { createStorage } from './createStorage';

jest.useFakeTimers('modern');

describe('TabMutexCleaner', () => {
  describe('when not run', () => {
    it('does not clean storage', () => {
      const mutexId = new TabMutexId('test').id;
      const storage = createStorage({ [mutexId]: 'value' });
      const _tabMutexCleaner = new TabMutexCleaner(storage);

      jest.advanceTimersByTime(
        Math.max(TabMutexCleaner.MUTEX_LIFETIME_MS, TabMutexCleaner.LOCALSTORAGE_POLLING_DELAY_MS) *
          3,
      );

      expect(storage[mutexId]).toBe('value');
    });
  });

  describe('when run', () => {
    it('cleans storage', () => {
      const mutexId = new TabMutexId('test').id;
      const storage = createStorage({ [mutexId]: 'value' });

      const tabMutexCleaner = new TabMutexCleaner(storage);
      tabMutexCleaner.run();

      jest.advanceTimersByTime(
        Math.max(TabMutexCleaner.MUTEX_LIFETIME_MS, TabMutexCleaner.LOCALSTORAGE_POLLING_DELAY_MS) *
          3,
      );

      tabMutexCleaner.destroy();

      expect(storage[mutexId]).toBeUndefined();
    });
  });

  describe('after destroy', () => {
    it('does not clean storage', () => {
      const mutexId = new TabMutexId('test').id;
      const storage = createStorage({ [mutexId]: 'value' });
      const tabMutexCleaner = new TabMutexCleaner(storage);
      tabMutexCleaner.run();

      tabMutexCleaner.destroy();

      jest.advanceTimersByTime(
        Math.max(TabMutexCleaner.MUTEX_LIFETIME_MS, TabMutexCleaner.LOCALSTORAGE_POLLING_DELAY_MS) *
          3,
      );

      tabMutexCleaner.destroy();

      expect(storage[mutexId]).toBe('value');
    });
  });
});
