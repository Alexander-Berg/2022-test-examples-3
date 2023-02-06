import { getLang, DEFAULT_LANG, LANGS } from '../lang';
import { changeUrl } from './shared';

describe('lang', () => {
  test('should use language from query parameter', () => {
    for (let lang of LANGS) {
      changeUrl(`localhost?lang=${lang}`);

      expect(getLang()).toBe(lang);
    }
  });

  test('should return default language on invalid query parameter', () => {
    changeUrl('localhost?lang=invalid_lang');

    expect(getLang()).toBe(DEFAULT_LANG);
  });

  test('should return default language on missing query parameter', () => {
    changeUrl('localhost');

    expect(getLang()).toBe(DEFAULT_LANG);
  });

  test('should show error on invalid query parameter', () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    changeUrl('localhost?lang=invalid_lang');
    getLang();

    expect(fn).toBeCalled();
  });

  test('should show error on missing query parameter', () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    changeUrl('localhost');
    getLang();

    expect(fn).toBeCalled();
  });
});
