/* eslint-disable import/first */
jest.unmock('axios');
import { XmlHttpRequest } from '@yandex-int/messenger.utils';
import { RegistryTransport } from '../RegistryTransport';

const csrfUrl = 'https://yandex.ru';
const apiUrl = 'https://api.yandex.ru';

function nextTick() {
    return new Promise((resolve) => {
        process.nextTick(resolve);
    });
}

describe('registryApi', () => {
    let requests: XmlHttpRequest.RequestsIterator;
    let transport: RegistryTransport;
    test;
    beforeEach(() => {
        XmlHttpRequest.mock();

        transport = new RegistryTransport({
            apiUrl,
            applicationId: '',
            apiVersion: 1,
            passportUpdateUrl: '',
            csrfTokenEnabled: true,
            csrfTokenUrl: csrfUrl,
            requestConfigWrapper: (_counterId, requestConfig) => requestConfig,
        });
        requests = XmlHttpRequest.iterator();
        jest.useFakeTimers();
    });

    afterEach(() => {
        if (transport) {
            (transport as any).dropCsrfToken();
        }
        XmlHttpRequest.unmock();
        jest.useRealTimers();
    });

    describe('Retry', () => {
        it('Обрабатывается ошибка без статуса', async () => {
            const response = transport.request('test', undefined, { enableCSRF: false, retry: 2 });

            await nextTick();

            requests.errorNext(0);
            jest.runOnlyPendingTimers();
            await nextTick();
            requests.errorNext(0);
            jest.runOnlyPendingTimers();
            await nextTick();

            requests.errorNext(0);
            jest.runOnlyPendingTimers();
            await nextTick();

            try {
                await response;
            } catch (e) {
                expect(e).toBeDefined();
            }
        });
    });

    describe('csrf', () => {
        it('Перед выполнением запроса должен запросить токен (только первый раз)', async () => {
            const response = transport.request('test');

            // Сначала запрашиваем токен
            await nextTick();

            expect(requests.size()).toBe(1);
            expect(requests.get(0).open).toBeCalledWith('POST', csrfUrl, true);
            requests.resolveNext({ token: 'test token' });

            // Токен пришел, происходит сам запрос в api
            await nextTick();

            expect(requests.size()).toBe(2);
            expect(requests.get(1).open).toBeCalledWith('POST', apiUrl, true);

            requests.resolveNext({ data: 'ok' });

            expect(requests.get(1).requestHeaders['X-CSRF-TOKEN']).toBe('test token');

            await nextTick();

            expect(await response).toBe('ok');

            // Второй запрос уже должен быть без предзапороса токена
            const response2 = transport.request('test');

            await nextTick();

            expect(requests.size()).toBe(3);
            expect(requests.get(2).open).toBeCalledWith('POST', apiUrl, true);

            requests.resolveNext({ data: 'ok' });

            expect(requests.get(2).requestHeaders['X-CSRF-TOKEN']).toBe('test token');

            await nextTick();

            expect(await response2).toBe('ok');
        });

        it('Токен должен перезапроситься', async () => {
            const response = transport.request('test');

            // Сначала запрашиваем токен
            await nextTick();

            expect(requests.size()).toBe(1);
            expect(requests.get(0).open).toBeCalledWith('POST', csrfUrl, true);
            requests.resolveNext({ token: 'test token' });

            // Токен пришел, происходит сам запрос в api
            await nextTick();

            expect(requests.size()).toBe(2);
            expect(requests.get(1).open).toBeCalledWith('POST', apiUrl, true);
            // Возвращаем ошибку bad_csrf_token
            requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

            // Должен произойти еще один запрос за токеном
            await nextTick();

            expect(requests.size()).toBe(3);
            expect(requests.get(2).open).toBeCalledWith('POST', csrfUrl, true);

            requests.resolveNext({ token: 'test token 2' });

            // Токен пришел, происходит сам запрос в api
            await nextTick();

            expect(requests.size()).toBe(4);
            expect(requests.get(3).open).toBeCalledWith('POST', apiUrl, true);

            requests.resolveNext({ data: 'ok' });
            expect(requests.get(3).requestHeaders['X-CSRF-TOKEN']).toBe('test token 2');

            await nextTick();

            expect(await response).toBe('ok');
        });

        it('Токен может быть перезапрошен максимум 2 раза', async () => {
            const response = transport.request('test');

            // Сначала запрашиваем токен
            await nextTick();

            expect(requests.size()).toBe(1);
            expect(requests.get(0).open).toBeCalledWith('POST', csrfUrl, true);
            requests.resolveNext({ token: 'test token 0' });

            for (let i = 0; i < 4; i += 2) {
                await nextTick();

                expect(requests.size()).toBe(i + 2);
                expect(requests.get(i + 1).open).toBeCalledWith(
                    'POST',
                    apiUrl,
                    true,
                );
                // Возвращаем ошибку bad_csrf_token
                requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });
                expect(requests.get(i + 1).requestHeaders[ 'X-CSRF-TOKEN' ]).toBe(`test token ${i}`);

                // Должен произойти еще один запрос за токеном
                await nextTick();

                expect(requests.size()).toBe(i + 3);
                expect(requests.get(i + 2).open).toBeCalledWith('POST', csrfUrl, true);

                requests.resolveNext({ token: `test token ${i + 2}` });
            }

            // Токен пришел, происходит сам запрос в api
            await nextTick();

            expect(requests.size()).toBe(6);
            expect(requests.get(5).open).toBeCalledWith(
                'POST',
                apiUrl,
                true,
            );
            // Возвращаем ошибку bad_csrf_token
            requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

            return response.catch((error) => {
                expect(error.status).toBe(403);
            });
        });

        it('Счетчик перезапроса должен быть сброшен', async () => {
            const response = transport.request('test');

            // Сначала запрашиваем токен
            await nextTick();

            requests.resolveNext({ token: 'test token 0' });

            for (let i = 0; i < 4; i += 2) {
                await nextTick();

                requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

                await nextTick();

                requests.resolveNext({ token: `test token ${i + 2}` });
            }

            await nextTick();

            requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

            await nextTick();

            try {
                await response;
            } catch (e) {
                expect(e.status).toBe(403);
            }

            // Второй запрос. Флаг должен быть сброшен делаем следующий запрос
            const response2 = transport.request('test');

            await nextTick();

            // Токен должен быть перезапрошен
            requests.resolveNext({ token: 'test token 0' });

            await nextTick();

            // В ответ на запрос возвращаем ошибку bad_csrf_token
            requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

            await nextTick();

            // Возвращаем новый токен
            requests.resolveNext({ token: 'normal token' });

            await nextTick();

            // На перезапрос возвращаем нормальный ответ после этого счетчик перезапроса csrf_token
            // должен быть сброшен
            requests.resolveNext({ data: 'ok' });
            expect(requests.value().requestHeaders['X-CSRF-TOKEN']).toBe('normal token');

            await nextTick();

            expect(await response2).toBe('ok');

            // Сценарий третьего запроса такой же как первого
            const response3 = transport.request('test');

            // Сначала запрашиваем токен
            await nextTick();

            requests.resolveNext({ token: 'test token 0' });

            for (let i = 0; i < 4; i += 2) {
                await nextTick();

                requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

                await nextTick();

                requests.resolveNext({ token: `test token ${i + 2}` });
            }

            await nextTick();

            requests.errorNext(403, '', { response: { data: { code: 'bad_csrf_token' } } });

            await nextTick();

            try {
                await response3;
            } catch (e) {
                expect(e.status).toBe(403);
            }
        });
    });
});
