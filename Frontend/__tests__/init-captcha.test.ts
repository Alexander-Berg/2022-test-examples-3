import { CAPTCHA_REGEX } from '../constants';
import { initCaptcha } from '../init-captcha';

import '../../typings/global.d';

describe('init captcha', () => {
  test('should set window.smartCaptcha._test to false when ?test is not present', () => {
    const mockScript = document.createElement('script');
    mockScript.src = CAPTCHA_REGEX;

    jest.spyOn(document, 'currentScript', 'get').mockReturnValue(mockScript);

    initCaptcha();

    expect(window.smartCaptcha._test).toBe('false');
  });

  test('should set window.smartCaptcha._test to ?test=false value', () => {
    const mockScript = document.createElement('script');
    mockScript.src = CAPTCHA_REGEX + '?test=false';

    jest.spyOn(document, 'currentScript', 'get').mockReturnValue(mockScript);

    initCaptcha();

    expect(window.smartCaptcha._test).toBe('false');
  });

  test('should set window.smartCaptcha._test to ?test=true value', () => {
    const mockScript = document.createElement('script');
    mockScript.src = CAPTCHA_REGEX + '?test=true';

    jest.spyOn(document, 'currentScript', 'get').mockReturnValue(mockScript);

    initCaptcha();

    expect(window.smartCaptcha._test).toBe('true');
  });

  test('should set window.smartCaptcha._origin to http://captcha-api.yandex.ru', () => {
    initCaptcha();

    expect(window.smartCaptcha._origin).toBe('https://captcha-api.yandex.ru');
  });
});
