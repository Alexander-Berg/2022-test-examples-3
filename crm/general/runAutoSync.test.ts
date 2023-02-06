import { observable, runInAction } from 'mobx';
import { runAutoSync } from './runAutoSync';
import { UserStatusImpl } from './UserStatusImpl';

jest.useFakeTimers('modern');

const createMockUserStatusStore = () => ({
  statuses: observable.map(),
  syncStatuses: jest.fn(),
  getUserStatusById: jest.fn(),
  removeUserStatusById: jest.fn(),
});

const SYNC_DELAY = 15 * 1000;

class Interval {
  private readonly timer: number;

  constructor(handler, delay, ...args) {
    this.timer = window.setInterval(handler, delay, ...args);
  }

  destroy() {
    window.clearInterval(this.timer);
  }
}

describe('runAutoSync', () => {
  it('does not run syncStatuses on init with empty statuses', () => {
    const mockUserStatusStore = createMockUserStatusStore();

    runAutoSync(mockUserStatusStore, SYNC_DELAY, Interval);

    jest.advanceTimersByTime(10000);

    expect(mockUserStatusStore.syncStatuses).toBeCalledTimes(0);
  });

  it('runs syncStatuses on init with statuses', () => {
    const mockUserStatusStore = createMockUserStatusStore();
    runAutoSync(mockUserStatusStore, SYNC_DELAY, Interval);

    runInAction(() => {
      mockUserStatusStore.statuses.set(100, new UserStatusImpl(mockUserStatusStore, 100));
    });

    jest.advanceTimersByTime(100);

    expect(mockUserStatusStore.syncStatuses).toBeCalledTimes(1);
  });

  describe('sync by timer', () => {
    const mockUserStatusStore = createMockUserStatusStore();

    runAutoSync(mockUserStatusStore, SYNC_DELAY, Interval);

    it('runs timer on not empty map', () => {
      runInAction(() => {
        mockUserStatusStore.statuses.set(100, new UserStatusImpl(mockUserStatusStore, 100));
      });

      jest.advanceTimersByTime(16000);

      expect(mockUserStatusStore.syncStatuses).toBeCalledTimes(2);
    });

    it('removes timer on empty map', () => {
      runInAction(() => {
        mockUserStatusStore.statuses.delete(100);
      });

      jest.advanceTimersByTime(16000);

      expect(mockUserStatusStore.syncStatuses).toBeCalledTimes(2);
    });
  });

  it('disposes resources', () => {
    const mockUserStatusStore = createMockUserStatusStore();

    const dispose = runAutoSync(mockUserStatusStore, SYNC_DELAY, Interval);

    runInAction(() => {
      mockUserStatusStore.statuses.set(100, new UserStatusImpl(mockUserStatusStore, 100));
    });

    jest.advanceTimersByTime(16000);

    dispose();

    runInAction(() => {
      mockUserStatusStore.statuses.set(101, new UserStatusImpl(mockUserStatusStore, 101));
    });

    jest.advanceTimersByTime(16000);

    expect(mockUserStatusStore.syncStatuses).toBeCalledTimes(2);
  });
});
