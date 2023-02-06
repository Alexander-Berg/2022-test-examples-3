import { act, renderHook } from '@testing-library/react-hooks';

import { useSmartCaptchaLoader, callbacks } from '../useSmartCaptchaLoader';

import '../../typings/global.d';

jest.mock('@yandex-int/external-captcha/sources/utils/load-script');

describe('useSmartCaptchaLoader', () => {
  beforeEach(() => {
    // @ts-ignore
    window.smartCaptcha = undefined;
    callbacks.splice(0, callbacks.length);
  });

  test('should return undefined initially', () => {
    const { result } = renderHook(() => useSmartCaptchaLoader());

    expect(result.current).toBeUndefined();
  });

  test('should return smartCaptcha after calling `window.__onSmartCaptchaReady`', () => {
    const { result } = renderHook(() => useSmartCaptchaLoader());

    expect(result.current).toBeUndefined();

    // @ts-expect-error
    window.smartCaptcha = 'mock';

    act(() => window.__onSmartCaptchaReady());

    expect(result.current).toBe(window.smartCaptcha);
  });

  test('should return smartCaptcha when it is already set', () => {
    // @ts-expect-error
    window.smartCaptcha = 'mock';

    const { result } = renderHook(() => useSmartCaptchaLoader());

    expect(result.current).toBe(window.smartCaptcha);
  });

  test('should return smartCaptcha to all hooks', () => {
    const { result: result1 } = renderHook(() => useSmartCaptchaLoader());
    const { result: result2 } = renderHook(() => useSmartCaptchaLoader());

    const results = [result1, result2];

    results.forEach((result) => expect(result.current).toBeUndefined());

    // @ts-expect-error
    window.smartCaptcha = 'mock';

    act(() => window.__onSmartCaptchaReady());

    results.forEach((result) => expect(result.current).toBe(window.smartCaptcha));
  });
});
