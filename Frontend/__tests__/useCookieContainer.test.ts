import { act } from 'react-test-renderer';
import { renderHook } from 'neo/tests/renderHook';
import { ECOOKIE_TYPE } from 'neo/types/cookies';
import 'neo/tests/mocks/hooks/contexts/useDataSourceCtx';
import 'neo/tests/mocks/hooks/useCookies';

import { useCookieContainer } from '../useCookieContainer';

describe('useCookieContainer', () => {
  it('should get value from cookie', () => {
    const [value] = renderHook(() => useCookieContainer('useCookieContainerTest', ECOOKIE_TYPE.TECH))();

    expect(value).toBe('someValue');
  });

  it('should set and remove value', () => {
    const getResult = renderHook(() => useCookieContainer('new', ECOOKIE_TYPE.TECH));
    const [value, set, remove] = getResult();

    expect(value).toBe(undefined);

    act(() => {
      set('newValue');
    });

    expect(getResult()[0]).toBe('newValue');

    act(() => {
      remove();
    });

    expect(getResult()[0]).toBe(undefined);
  });

  it('should be able to work with a few cookies', () => {
    const getResult1 = renderHook(() => useCookieContainer('name1', ECOOKIE_TYPE.TECH));
    const getResult2 = renderHook(() => useCookieContainer('name2', ECOOKIE_TYPE.TECH));
    const [, set1] = getResult1();
    const [, set2] = getResult2();

    act(() => {
      set1('value1');
      set2('value2');
    });

    expect(getResult1()[0]).toBe('value1');
    expect(getResult2()[0]).toBe('value2');
  });

  it('should be able change cookie name', () => {
    const [value] = renderHook(() => useCookieContainer('useCookieContainerTest', ECOOKIE_TYPE.TECH, {
      cookieName: 'otherContainerName',
    }))();

    expect(value).toBe('otherValue');
  });
});
