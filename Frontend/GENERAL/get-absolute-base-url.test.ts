import { createApphostContextStub } from './create-apphost-context-stub';
import { getAbsoluteBaseUrl } from './get-absolute-base-url';

describe('middlewares/get-absolute-base-url', () => {
    it('Должен получать URL для HTTP', () => {
        expect(getAbsoluteBaseUrl(createApphostContextStub({
            request: {
                proto: 'http',
                path: 'quasar',
                hostname: 'yandex.kz',
            },
        }))).toEqual('http://yandex.kz/quasar');
    });

    it('Должен получать URL для HTTPS', () => {
        expect(getAbsoluteBaseUrl(createApphostContextStub({
            request: {
                proto: 'https',
                path: 'quasar',
                hostname: 'yandex.by',
            },
        }))).toEqual('https://yandex.by/quasar');
    });

    it('Должен получать URL для стандартных портов', () => {
        expect(getAbsoluteBaseUrl(createApphostContextStub({
            request: {
                proto: 'http',
                path: 'quasar',
                hostname: 'yandex.ru',
                port: 80,
            },
        }))).toEqual('http://yandex.ru/quasar');
    });

    it('Должен получать URL для стандартных портов 2', () => {
        expect(getAbsoluteBaseUrl(createApphostContextStub({
            request: {
                proto: 'https',
                path: 'quasar',
                hostname: 'yandex.ru',
                port: 443,
            },
        }))).toEqual('https://yandex.ru/quasar');
    });

    it('Должен получать URL для нестандартных портов', () => {
        expect(getAbsoluteBaseUrl(createApphostContextStub({
            request: {
                proto: 'https',
                path: 'quasar',
                hostname: 'local.yandex.ru',
                port: '3443',
            },
        }))).toEqual('https://local.yandex.ru:3443/quasar');
    });
});
