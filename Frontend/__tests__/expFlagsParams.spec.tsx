import { getHost } from '../url';
import { restoreDom, clearNamespace } from '../../__tests__/tests-lib';

describe('Турбо-оверлей', () => {
    describe('Утилиты', () => {
        describe('Работа с экспериментальными флагами', () => {
            beforeEach(() => {
                restoreDom();
                jest.resetModules();
            });
            afterEach(restoreDom);
            afterAll(clearNamespace);

            it('Получает турбо-урл из dom', () => {
                // подготавливаем обвязку для получения урла
                const script = document.createElement('script');
                script.setAttribute('turbo-url', 'https://yandex.ru');
                script.setAttribute('id', 'overlay-script');
                document.body.appendChild(script);

                const { initExpFlags } = require('../expFlagsParams');

                let { turboUrl } = initExpFlags();
                expect(getHost(turboUrl)).toEqual('yandex.ru');

                script.setAttribute('turbo-url', 'https://renderer-turbo-dev.tunneler-ci.yandex.ru/turbo');

                ({ turboUrl } = initExpFlags());
                expect(getHost(turboUrl)).toEqual('renderer-turbo-dev.tunneler-ci.yandex.ru');

                script.removeAttribute('turbo-url');

                ({ turboUrl } = initExpFlags());
                expect(getHost(turboUrl)).toBeNull();
            });

            it('Получает время фоллбэка из dom', () => {
                // подготавливаем обвязку для получения урла
                const script = document.createElement('script');

                script.setAttribute('turbo-fallback-timeout', '100');
                script.setAttribute('id', 'overlay-script');
                document.body.appendChild(script);

                const { initExpFlags } = require('../expFlagsParams');

                let { turboFallbackTimeout } = initExpFlags();
                expect(turboFallbackTimeout).toBe(100);

                script.setAttribute('turbo-fallback-timeout', '100000');

                ({ turboFallbackTimeout } = initExpFlags());
                expect(turboFallbackTimeout).toBe(100000);

                script.removeAttribute('turbo-fallback-timeout');

                ({ turboFallbackTimeout } = initExpFlags());
                expect(turboFallbackTimeout).toBe(5000);
            });
        });
    });
});
