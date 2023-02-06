import { getCurrentScript } from '../current-script';

describe('getCurrentScript', () => {
  test('should return instance of current script where document.currentScript is supported', () => {
    const params = {
      id: 'current-script-id',
      src: 'current-script-src',
    };

    Object.defineProperty(document, 'currentScript', {
      configurable: true,
      get: () => params,
    });

    expect(getCurrentScript()).toEqual(params);
  });

  test('should return instance of current script where document.currentScript is not supported', () => {
    const currentScriptId = 'current-script-id';

    document.body.innerHTML = `
      <script src="smth.js"></script>
      <script src="https://captcha-api.yandex.ru/captcha.js?params" id="${currentScriptId}"></script>
      <script src="/other.js"></script>
    `;

    Object.defineProperty(document, 'currentScript', {
      configurable: true,
      get: () => null,
    });

    expect(getCurrentScript().id).toBe(currentScriptId);
  });

  test('should use first script when multiple srcipts match captcha reges', () => {
    global.console.warn = jest.fn();
    const currentScriptId = 'current-script-id';

    document.body.innerHTML = `
      <script src="https://captcha-api.yandex.ru/captcha.js?params" id="${currentScriptId}"></script>
      <script src="https://captcha-api.yandex.ru/captcha.js?params2" id="another-script-id"></script>
    `;

    expect(getCurrentScript().id).toBe(currentScriptId);
  });

  test('should warn user when multiple scripts match captcha regex', () => {
    global.console.warn = jest.fn();

    document.body.innerHTML = `
      <script src="https://captcha-api.yandex.ru/captcha.js?params" id="current-script-id"></script>
      <script src="https://captcha-api.yandex.ru/captcha.js?params2" id="another-script-id"></script>
    `;

    getCurrentScript();
    expect(global.console.warn).toHaveBeenCalled();
  });
});
