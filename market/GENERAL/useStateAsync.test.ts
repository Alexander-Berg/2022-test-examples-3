import { renderHook, act } from '@testing-library/react-hooks';

import { useStateAsync } from './useStateAsync';

describe('hook useStateAsync', () => {
  it('test hook', async () => {
    const { result } = renderHook(() => useStateAsync<string>('0'));
    const fn = jest.fn();
    const fn2 = jest.fn();
    expect(result.current[0]).toEqual('0');
    await act(() => {
      result.current[1]('4');
    });
    await act(async () => {
      fn2(await result.current[1]('1', fn));
    });
    expect(result.current[0]).toEqual('1');
    expect(fn.mock.calls.length).toEqual(1);
    expect(fn2.mock.calls.length).toEqual(1);
    fn.mockReset();
    fn2.mockReset();
    await act(async () => {
      fn2(await result.current[1]('1', fn));
    });
    expect(result.current[0]).toEqual('1');
    expect(fn.mock.calls.length).toEqual(1);
    expect(fn2.mock.calls.length).toEqual(1);

    fn.mockReset();
    fn2.mockReset();
    await act(async () => {
      const setState = result.current[1];
      fn2(await setState(v => `${v}5`, fn));
    });
    expect(result.current[0]).toEqual('15');
    expect(fn.mock.calls.length).toEqual(1);
    expect(fn2.mock.calls.length).toEqual(1);
  });
});
