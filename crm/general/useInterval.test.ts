import { renderHook } from '@testing-library/react-hooks/native';
import { useInterval } from './useInterval';

describe('useInterval', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('calls callback function', async () => {
    const callback = jest.fn();

    const timeout = 500;

    renderHook(() => useInterval({ callback, timeout, isActive: true }));
    expect(callback).toHaveBeenCalledTimes(0);

    jest.advanceTimersByTime(timeout);

    expect(callback).toBeCalledTimes(1);
  });

  it('does nothing when inactive callback function', async () => {
    const timeout = 500;
    const callback = jest.fn();

    renderHook(() => useInterval({ callback, timeout, isActive: false }));

    jest.advanceTimersByTime(timeout);

    expect(callback).toHaveBeenCalledTimes(0);
  });
});
