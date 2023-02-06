import { renderHook } from '@testing-library/react-hooks';
import { act } from 'react-test-renderer';

import { useIsTriple } from './useIsTriple';

describe('useIsTriple::', () => {
  it('inits', () => {
    const view = renderHook(() => useIsTriple());

    expect(view.result.current.isTriple).toEqual(false);
  });
  it('toggle', () => {
    const view = renderHook(() => useIsTriple());

    expect(view.result.current.isTriple).toEqual(false);

    act(() => {
      view.result.current.toggleTriple();
    });

    expect(view.result.current.isTriple).toEqual(true);

    act(() => {
      view.result.current.toggleTriple();
    });

    expect(view.result.current.isTriple).toEqual(false);
  });
  it('load from localstorage', () => {
    window.localStorage.setItem('TEST_USE_TRIPLE_KEY_3', 'true');
    const view = renderHook(() => useIsTriple('TEST_USE_TRIPLE_KEY_3'));

    expect(view.result.current.isTriple).toEqual(true);

    act(() => {
      view.result.current.toggleTriple();
    });

    expect(window.localStorage.getItem('TEST_USE_TRIPLE_KEY_3')).toEqual('false');

    const utils = renderHook(() => useIsTriple('TEST_USE_TRIPLE_KEY_3'));

    expect(utils.result.current.isTriple).toEqual(false);
  });
});
