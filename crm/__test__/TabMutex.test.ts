import { runPendingPromises } from 'utils/runPendingPromises';
import { TabMutex } from '../TabMutex';
import { TabMutexId } from '../TabMutexId';
import { createStorage } from './createStorage';

jest.useFakeTimers('modern');

describe('TabMutex', () => {
  const task = jest.fn();
  const taskId = 'id';
  const mutexId = new TabMutexId(taskId);
  const otherTabId = 'tab_id';

  beforeEach(() => {
    task.mockClear();
  });

  describe('when not run', () => {
    it('does not clean storage', () => {
      const storage = createStorage();
      const _tabMutex = new TabMutex({ id: 'id', task, storage });

      jest.advanceTimersByTime(TabMutex.DELAY_FOR_STORAGE_UPDATE_MS * 2);

      expect(storage.length).toBe(0);
      expect(task).not.toBeCalled();
    });
  });

  describe('before delay and other task already run', () => {
    it('does not catch lock', async () => {
      const storage = createStorage({ [mutexId.id]: otherTabId });
      const tabMutex = new TabMutex({ id: taskId, task, storage });
      tabMutex.run();

      jest.advanceTimersByTime(TabMutex.DELAY_FOR_STORAGE_UPDATE_MS * 2);
      await runPendingPromises();

      expect(storage[mutexId.id]).toBe(otherTabId);
      expect(task).not.toBeCalled();
    });
  });

  describe('after delay', () => {
    describe('when this mutex catch lock', () => {
      it('runs task', async () => {
        const storage = createStorage();
        const tabMutex = new TabMutex({ id: taskId, task, storage });
        tabMutex.run();

        jest.advanceTimersByTime(TabMutex.DELAY_FOR_STORAGE_UPDATE_MS * 2);
        await runPendingPromises();

        expect(storage[mutexId.id]).toBe(String(TabMutex.UNIQ_TAB_ID));
        expect(task).toBeCalledTimes(1);
      });
    });

    describe('when other mutex catch lock', () => {
      it('does not run task', async () => {
        const storage = createStorage();
        const tabMutex = new TabMutex({ id: taskId, task, storage });
        tabMutex.run();

        storage[mutexId.id] = otherTabId;
        jest.advanceTimersByTime(TabMutex.DELAY_FOR_STORAGE_UPDATE_MS * 2);
        await runPendingPromises();

        expect(storage[mutexId.id]).toBe(otherTabId);
        expect(task).not.toBeCalled();
      });
    });
  });
});
