import { AdaptiveInterval } from './AdaptiveInterval';

jest.useFakeTimers();

describe('AdaptiveInterval', () => {
  const callbackMock = jest.fn();
  let adaptiveInterval: AdaptiveInterval;
  beforeEach(() => {
    callbackMock.mockClear();
  });

  it('runs no interval on init', () => {
    adaptiveInterval = new AdaptiveInterval(callbackMock, 100);
    expect(callbackMock).not.toBeCalled();
  });

  describe('normal interval', () => {
    it('runs no interval for callback with zero timeout', () => {
      adaptiveInterval = new AdaptiveInterval(callbackMock, 0);
      adaptiveInterval.setNormalInterval();
      jest.advanceTimersByTime(200);
      adaptiveInterval.clearInterval();
      expect(callbackMock).not.toBeCalled();
    });

    it('runs call callback in interval', () => {
      adaptiveInterval = new AdaptiveInterval(callbackMock, 100);
      adaptiveInterval.setNormalInterval();
      jest.advanceTimersByTime(200);
      adaptiveInterval.clearInterval();
      expect(callbackMock).toBeCalledTimes(2);
    });

    it('supports stop interval', () => {
      adaptiveInterval = new AdaptiveInterval(callbackMock, 100);
      adaptiveInterval.setNormalInterval();
      jest.advanceTimersByTime(200);
      adaptiveInterval.clearInterval();
      jest.advanceTimersByTime(200);
      expect(callbackMock).toBeCalledTimes(2);
    });
  });

  describe('slow interval', () => {
    it('runs no interval for callback with zero timeout', () => {
      adaptiveInterval = new AdaptiveInterval(callbackMock, 0);
      adaptiveInterval.setSlowInterval();
      jest.advanceTimersByTime(200);
      adaptiveInterval.clearInterval();
      expect(callbackMock).not.toBeCalled();
    });

    it('runs call callback in interval', () => {
      adaptiveInterval = new AdaptiveInterval(callbackMock, 100);
      adaptiveInterval.setSlowInterval();
      jest.advanceTimersByTime(200);
      adaptiveInterval.clearInterval();
      expect(callbackMock).toBeCalledTimes(1);
    });

    it('supports stop interval', () => {
      adaptiveInterval = new AdaptiveInterval(callbackMock, 100);
      adaptiveInterval.setSlowInterval();
      jest.advanceTimersByTime(200);
      adaptiveInterval.clearInterval();
      jest.advanceTimersByTime(200);
      expect(callbackMock).toBeCalledTimes(1);
    });
  });

  it('supports options.timeoutNormal', () => {
    adaptiveInterval = new AdaptiveInterval(callbackMock, { timeoutNormal: 100 });
    adaptiveInterval.setNormalInterval();
    jest.advanceTimersByTime(200);
    adaptiveInterval.clearInterval();
    expect(callbackMock).toBeCalledTimes(2);
  });

  it('supports options.timeoutSlow', () => {
    adaptiveInterval = new AdaptiveInterval(callbackMock, { timeoutNormal: 100, timeoutSlow: 400 });
    adaptiveInterval.setSlowInterval();
    jest.advanceTimersByTime(400);
    adaptiveInterval.clearInterval();
    expect(callbackMock).toBeCalledTimes(1);
  });

  it('passes args to callback', () => {
    adaptiveInterval = new AdaptiveInterval(callbackMock, 100, 'a');
    adaptiveInterval.setNormalInterval();
    jest.advanceTimersByTime(100);
    adaptiveInterval.clearInterval();
    expect(callbackMock).toBeCalledWith('a');
  });

  describe('AdaptiveInterval.formatOptions', () => {
    it('formats number', () => {
      expect(AdaptiveInterval.formatOptions(100)).toStrictEqual({ timeoutNormal: 100 });
    });

    it('passes object', () => {
      expect(AdaptiveInterval.formatOptions({ timeoutNormal: 100 })).toStrictEqual({
        timeoutNormal: 100,
      });
    });
  });
});
