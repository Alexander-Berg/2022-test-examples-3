import { getQueryStringFromRequest } from './get-query-string-from-request';
import { createApphostContextStub } from './create-apphost-context-stub';

describe('middlewares/get-query-string-from-request', () => {
    it('Должна корректно выдергивать строку параметров', () => {
        expect(getQueryStringFromRequest(createApphostContextStub({
            request: {
                headers: {},
                uri: '/quasar/iot?abc=2',
            },
        }))).toEqual('abc=2');
    });

    it('Должна корректно отфильтровывать параметры apphost`а', () => {
        expect(getQueryStringFromRequest(createApphostContextStub({
            request: {
                headers: {},
                uri: '/quasar/iot?renderer_export=binary&no_bolver=1&json=request&some-test-flag=1',
            },
        }))).toEqual('json=request&some-test-flag=1');
    });

    it('Должна корректно обрабатывать параметры-массивы', () => {
        expect(getQueryStringFromRequest(createApphostContextStub({
            request: {
                headers: {},
                uri: '/quasar/iot/sockets/onboarding/dbe28b5a-06dc-45be-9ce2-5e33b84b2752?ids%5B0%5D=dbe28b5a-06dc-45be-9ce2-5e33b84b2751&ids%5B1%5D=dbe28b5a-06dc-45be-9ce2-5e33b84b2752&ids%5B2%5D=dbe28b5a-06dc-45be-9ce2-5e33b84b275b&lang=ru&model=unknown&names%5B0%5D=Mi%20Air%20Purifier%202S%20%2C%20%7B%7D%20%3A&names%5B1%5D=Mi%20Air%20Purifier%201S&names%5B2%5D=Mi%20Air%20Purifier%201S',
            }
        }))).toEqual('ids%5B0%5D=dbe28b5a-06dc-45be-9ce2-5e33b84b2751&ids%5B1%5D=dbe28b5a-06dc-45be-9ce2-5e33b84b2752&ids%5B2%5D=dbe28b5a-06dc-45be-9ce2-5e33b84b275b&lang=ru&model=unknown&names%5B0%5D=Mi%20Air%20Purifier%202S%20%2C%20%7B%7D%20%3A&names%5B1%5D=Mi%20Air%20Purifier%201S&names%5B2%5D=Mi%20Air%20Purifier%201S');
    });

    it('Должна корректно обрабатывать параметр srcrwr', () => {
        expect(getQueryStringFromRequest(createApphostContextStub({
            request: {
                headers: {},
                uri: '/quasar?srcrwr=QUASAR_HOST%3Atesting.quasar.yandex.ru&srcrwr=IOT_HOST%3Aiot-dev.quasar.yandex.ru',
            }
        }))).toEqual('srcrwr=QUASAR_HOST%3Atesting.quasar.yandex.ru&srcrwr=IOT_HOST%3Aiot-dev.quasar.yandex.ru');
    });

    it('Должна корректно обрабатывать стандартные параметры ПП', () => {
        expect(getQueryStringFromRequest(createApphostContextStub({
            request: {
                headers: {},
                uri: '/quasar/iot?app_id=ru.yandex.mobile&app_platform=iphone&app_version_name=24.00&dp=3.0&lang=ru-RU&model=iPhone10,6&os_version=13.3&size=1125,2436',
            }
        }))).toEqual('app_id=ru.yandex.mobile&app_platform=iphone&app_version_name=24.00&dp=3.0&lang=ru-RU&model=iPhone10%2C6&os_version=13.3&size=1125%2C2436');
    });
});
