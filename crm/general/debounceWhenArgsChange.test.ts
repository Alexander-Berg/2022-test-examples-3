import { debounceWhenArgsChange } from './debounceWhenArgsChange';

describe('debounceWhenArgsChange', () => {
  beforeAll(() => {
    jest.useFakeTimers('modern');
  });

  it('does not reset timer when args not changed', async () => {
    const fn = jest.fn();
    const debounced = debounceWhenArgsChange(fn, 1000);

    debounced('1.0');
    debounced('1.1');
    jest.advanceTimersByTime(500);
    expect(fn).not.toBeCalled();

    debounced('1.1');
    jest.advanceTimersByTime(500);
    expect(fn).toBeCalledTimes(1);
    expect(fn).toBeCalledWith('1.1');
  });

  it('reset timer when args changed', async () => {
    const fn = jest.fn();
    const debounced = debounceWhenArgsChange(fn, 1000);

    debounced('1.1');
    jest.advanceTimersByTime(500);
    expect(fn).not.toBeCalled();

    debounced('1.0');
    jest.advanceTimersByTime(500);
    expect(fn).not.toBeCalled();

    debounced('1.1');
    jest.advanceTimersByTime(500);
    expect(fn).not.toBeCalled();
  });
});
