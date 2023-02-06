import { act } from 'react-test-renderer';
import { renderHook } from 'neo/tests/renderHook';
import 'neo/tests/mocks/hooks/contexts/useDataSourceCtx';
import 'neo/tests/mocks/hooks/useCookies';

import { useCookieCounter } from '../useCookieCounter';

describe('useCookieCounter', () => {
  it('should be 0 for empty cookie', () => {
    const [count] = renderHook(() => useCookieCounter('new'))();

    expect(count).toBe(0);
  });

  it('should get value from cookie', () => {
    const [count] = renderHook(() => useCookieCounter('useCookieCounterTest'))();

    expect(count).toBe(12);
  });

  it('should setCounter, increment, decrement and reset value', () => {
    const getResult = renderHook(() => useCookieCounter('useCookieCounterTest'));

    act(() => {
      const setCounter = getResult()[1];
      setCounter(42);
    });

    expect(getResult()[0]).toBe(42);

    act(() => {
      const decrement = getResult()[3];
      decrement();
    });

    expect(getResult()[0]).toBe(41);

    act(() => {
      const reset = getResult()[4];
      reset();
    });

    expect(getResult()[0]).toBe(0);

    act(() => {
      const increment = getResult()[2];
      increment();
    });

    expect(getResult()[0]).toBe(1);
  });

  it('should be able to work with a few cookies', () => {
    const getResult1 = renderHook(() => useCookieCounter('name1'));
    const getResult2 = renderHook(() => useCookieCounter('name2'));
    const [, setCounter1] = getResult1();
    const [,, increment2] = getResult2();

    act(() => {
      setCounter1(13);
      increment2();
    });

    expect(getResult1()[0]).toBe(13);
    expect(getResult2()[0]).toBe(1);
  });

  it('should be able change cookie name', () => {
    const [value] = renderHook(() => useCookieCounter('useCookieCounterTest', {
      cookieName: 'otherCounterName',
    }))();

    expect(value).toBe(100500);
  });
});
