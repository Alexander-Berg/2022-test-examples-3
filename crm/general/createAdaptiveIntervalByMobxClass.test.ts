import { makeAutoObservable, runInAction } from 'mobx';
import { mocked } from 'ts-jest/utils';
import { AdaptiveInterval } from './AdaptiveInterval';
import { createAdaptiveIntervalByMobxClass } from './createAdaptiveIntervalByMobxClass';

jest.useFakeTimers('modern');
jest.mock('./AdaptiveInterval');

const AdaptiveIntervalMocked = mocked(AdaptiveInterval);

class MobxStore {
  useNormalInterval: boolean = true;

  constructor() {
    makeAutoObservable(this);
  }
}

const mobxStore = new MobxStore();
const AdaptiveIntervalByMobx = createAdaptiveIntervalByMobxClass({
  switcher: () => mobxStore.useNormalInterval,
});
const callbackMock = jest.fn();

describe('createAdaptiveIntervalByMobx', () => {
  beforeEach(() => {
    AdaptiveIntervalMocked.mockClear();
    callbackMock.mockClear();
  });

  it('creates AdaptiveInterval on init', () => {
    const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
    adaptiveIntervalByMobx.destroy();
    expect(AdaptiveIntervalMocked).toBeCalled();
  });

  it('creates normal interval on init', () => {
    mobxStore.useNormalInterval = true;
    const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
    jest.advanceTimersByTime(100);
    const adaptiveIntervalMocked = AdaptiveIntervalMocked.mock.instances[0];
    adaptiveIntervalByMobx.destroy();
    expect(adaptiveIntervalMocked.setNormalInterval).toBeCalled();
    expect(adaptiveIntervalMocked.setSlowInterval).not.toBeCalled();
  });

  it('creates slow interval on init', () => {
    mobxStore.useNormalInterval = false;
    const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
    jest.advanceTimersByTime(100);
    const adaptiveIntervalMocked = AdaptiveIntervalMocked.mock.instances[0];
    adaptiveIntervalByMobx.destroy();
    expect(adaptiveIntervalMocked.setNormalInterval).not.toBeCalled();
    expect(adaptiveIntervalMocked.setSlowInterval).toBeCalled();
  });

  it('switches interval on store change', () => {
    mobxStore.useNormalInterval = true;
    const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
    jest.advanceTimersByTime(100);
    runInAction(() => {
      mobxStore.useNormalInterval = false;
    });
    jest.advanceTimersByTime(100);
    const adaptiveIntervalMocked = AdaptiveIntervalMocked.mock.instances[0];
    adaptiveIntervalByMobx.destroy();
    expect(adaptiveIntervalMocked.setSlowInterval).toBeCalled();
  });

  it('supports debounce delay option', () => {
    mobxStore.useNormalInterval = false;
    const AdaptiveIntervalByMobx = createAdaptiveIntervalByMobxClass({
      switcher: () => mobxStore.useNormalInterval,
      debounceDelay: 100,
    });
    const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
    const adaptiveIntervalMocked = AdaptiveIntervalMocked.mock.instances[0];
    jest.advanceTimersByTime(100);
    runInAction(() => {
      mobxStore.useNormalInterval = true;
    });

    expect(adaptiveIntervalMocked.setNormalInterval).not.toBeCalled();
    jest.advanceTimersByTime(100);
    expect(adaptiveIntervalMocked.setNormalInterval).toBeCalled();

    adaptiveIntervalByMobx.destroy();
  });

  it('passes adaptive interval options', () => {
    const AdaptiveIntervalByMobx = createAdaptiveIntervalByMobxClass({
      switcher: () => mobxStore.useNormalInterval,
      adaptiveIntervalOptions: {
        timeoutNormal: 100,
        timeoutSlow: 200,
      },
    });
    AdaptiveIntervalMocked.formatOptions.mockImplementationOnce((timeoutNormal) => ({
      timeoutNormal: timeoutNormal as number,
    }));
    const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 300);
    adaptiveIntervalByMobx.destroy();

    expect(AdaptiveIntervalMocked).toBeCalledWith(callbackMock, {
      timeoutNormal: 300,
      timeoutSlow: 200,
    });
  });

  describe('.destroy', () => {
    it("doesn't switch interval on store change", () => {
      mobxStore.useNormalInterval = true;
      const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
      adaptiveIntervalByMobx.destroy();
      runInAction(() => {
        mobxStore.useNormalInterval = false;
      });
      const adaptiveIntervalMocked = AdaptiveIntervalMocked.mock.instances[0];
      expect(adaptiveIntervalMocked.setSlowInterval).not.toBeCalled();
    });

    it('stops interval', () => {
      const adaptiveIntervalByMobx = new AdaptiveIntervalByMobx(callbackMock, 100);
      adaptiveIntervalByMobx.destroy();
      const adaptiveIntervalMocked = AdaptiveIntervalMocked.mock.instances[0];
      expect(adaptiveIntervalMocked.clearInterval).toBeCalled();
    });
  });
});
