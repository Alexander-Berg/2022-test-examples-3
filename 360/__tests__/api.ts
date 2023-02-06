import getApi from '../src/api';

const suggestFakeResponse = {
    domain_status: 'available',
    suggested_domains: [{
        login: 'login1',
        name: 'test1.ru'
    }, {
        login: 'login2',
        name: 'test2.ru'
    }]
};

const fetchMockFn = (url: string, params: RequestInit) => {
    const bodyParsed = JSON.parse(String(params.body));

    const getResponse = (modelResult: Record<string, unknown>) => Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
            uid: bodyParsed._uid,
            models: [{ data: modelResult }]
        })
    });

    if (
        !url.includes('_m=get-ckey/v1') &&
        (!bodyParsed._ckey || bodyParsed._ckey === 'old-ckey')
    ) {
        return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({
                uid: bodyParsed._uid,
                models: [{
                    error: 'ckey',
                    ...(
                        // Тут не самый очевидный момент - почему-то шаренные модели `web-api` сразу возвращают новый
                        // ckey в поле `renewCkey`, а обычные модели - нет. И для обычных приходится идти дополнительно
                        // в ручку `renewCkey`. Здесь проэмулировал это поведение.
                        url.includes('/shared/') ? {
                            renewCkey: 'new-ckey'
                        } : null
                    )
                }]
            })
        });
    }

    if (url.includes('_m=get-ckey/v1')) {
        return getResponse({
            ckey: 'new-ckey'
        });
    }

    if (url.includes('_m=get-domenator-domains-suggest/v1')) {
        return getResponse(suggestFakeResponse);
    }

    if (url.includes('_m=domenator-register-domain/v1')) {
        return getResponse({
            result: 'success'
        });
    }

    if (url.includes('_m=get-domenator-domain-status/v1')) {
        return getResponse({
            domain: bodyParsed.models[0].params.domain || 'test.ru',
            login: 'my-login',
            status: 'registered'
        });
    }

    if (
        url.includes('_m=domenator-cancel-subscription/v1') ||
        url.includes('_m=domenator-change-login/v1')
    ) {
        return getResponse({});
    }

    return Promise.reject();
};

describe('api', () => {
    const originalFetch = global.fetch;
    const fetchMock = jest.fn(fetchMockFn);

    const onSkUpdateFn = jest.fn();
    const DEFAULT_API_PARAMS = {
        modelsUrl: 'https://web-api.ya.ru/models/',
        sk: 'new-ckey',
        uid: '123',
        onSkUpdate: onSkUpdateFn,
        extraHeaders: {
            'Authorization': 'OAuth 42face'
        }
    };

    beforeEach(() => {
        onSkUpdateFn.mockClear();
        fetchMock.mockClear();
        // @ts-ignore
        global.fetch = fetchMock;
    });
    afterEach(() => {
        global.fetch = originalFetch;
    });

    describe('getSuggestList', () => {
        it('should call `get-domenator-domains-suggest/v1` model with proper parameters', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.getSuggestList();

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock).toBeCalledWith(
                'https://web-api.ya.ru/models/?_m=get-domenator-domains-suggest/v1',
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'OAuth 42face'
                    },
                    credentials: 'include',
                    body: JSON.stringify({
                        models: [{
                            name: 'get-domenator-domains-suggest/v1',
                            params: {
                                tld: 'ru'
                            },
                        }],
                        _ckey: 'new-ckey',
                        _uid: '123',
                        _connection_id: undefined,
                    }),
                }
            );

            expect(result).toEqual(suggestFakeResponse);
        });

        it('can be called with string or with object', async () => {
            const api = getApi(DEFAULT_API_PARAMS);

            const result1 = await api.getSuggestList('login1@test1.ru');
            expect(result1).toEqual(suggestFakeResponse);
            expect(fetchMock).toBeCalledTimes(1);
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models[0].params).toEqual({
                tld: 'ru',
                login: 'login1',
                domain_base: 'test1'
            });

            const result2 = await api.getSuggestList({
                tld: 'ru',
                domainBase: 'test2',
                login: 'login2'
            });
            expect(result2).toEqual(suggestFakeResponse);
            expect(fetchMock).toBeCalledTimes(2);
            expect(JSON.parse(String(fetchMock.mock.calls[1][1].body)).models[0].params).toEqual({
                tld: 'ru',
                login: 'login2',
                domain_base: 'test2'
            });
        });

        it('call with limit', async () => {
            const api = getApi(DEFAULT_API_PARAMS);

            const result = await api.getSuggestList(undefined, 1);
            expect(result).toEqual(suggestFakeResponse);
            expect(fetchMock).toBeCalledTimes(1);
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models[0].params).toEqual({
                tld: 'ru',
                limit: '1'
            });
        });

        const testApiWithSkUpdate = async (isShared: boolean, ckey?: string) => {
            const modelsUrl = `https://web-api.ya.ru/${isShared ? 'shared' : 'models'}/`;
            const api = getApi({
                ...DEFAULT_API_PARAMS,
                modelsUrl,
                // @ts-ignore
                sk: ckey
            });
            const result = await api.getSuggestList();
            expect(result).toEqual(suggestFakeResponse);
            expect(fetchMock).toBeCalledTimes(isShared ? 2 : 3);

            expect(fetchMock.mock.calls[0][0]).toEqual(modelsUrl + '?_m=get-domenator-domains-suggest/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body))._ckey).toEqual(ckey);
            if (!isShared) {
                expect(fetchMock.mock.calls[1][0]).toEqual('https://web-api.ya.ru/models/?_m=get-ckey/v1');
            }
            const lastCall = isShared ? fetchMock.mock.calls[1] : fetchMock.mock.calls[2];
            expect(lastCall[0]).toEqual(modelsUrl + '?_m=get-domenator-domains-suggest/v1');
            expect(JSON.parse(String(lastCall[1].body))._ckey).toEqual('new-ckey');

            expect(onSkUpdateFn).toBeCalledWith('new-ckey');
        };

        it('should update SK & call api with new SK if SK is not valid', async () => {
            await testApiWithSkUpdate(false, 'old-ckey');
        });

        it('[shared model] should update SK & call api with new SK if SK is not valid', async () => {
            await testApiWithSkUpdate(true, 'old-ckey');
        });

        it('should update SK & call api with new SK if there is no SK at all', async () => {
            await testApiWithSkUpdate(false);
        });

        it('[shared model] should update SK & call api with new SK if there is no SK at all', async () => {
            await testApiWithSkUpdate(true);
        });

        it('should not update SK if it was updated by updateSk method', async () => {
            const api = getApi({
                ...DEFAULT_API_PARAMS,
                sk: 'old-ckey'
            });
            api.updateSk('new-key');
            const result = await api.getSuggestList();
            expect(result).toEqual(suggestFakeResponse);
            expect(fetchMock).toBeCalledTimes(1);

            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=get-domenator-domains-suggest/v1');
            expect(onSkUpdateFn).not.toBeCalled();
        });

        it('should use new params after update', async () => {
            let api = getApi(DEFAULT_API_PARAMS);
            api = getApi({
                modelsUrl: 'https://new-web-api.ya.ru/models/',
                sk: 'new-ckey-2',
                uid: '456'
            });
            const result = await api.getSuggestList();
            expect(result).toEqual(suggestFakeResponse);
            expect(fetchMock).toBeCalledTimes(1);

            expect(fetchMock.mock.calls[0][0]).toEqual('https://new-web-api.ya.ru/models/?_m=get-domenator-domains-suggest/v1');
            const requestBody = JSON.parse(String(fetchMock.mock.calls[0][1].body));
            expect(requestBody._uid).toEqual('456');
            expect(requestBody._ckey).toEqual('new-ckey-2');
        });
    });

    describe('registerDomain', () => {
        it('should call `domenator-register-domain/v1` model with proper parameters', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.registerDomain('test3.ru', 'login3');

            expect(result).toEqual({ result: 'success' });

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=domenator-register-domain/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models).toEqual([{
                name: 'domenator-register-domain/v1',
                params: {
                    domain: 'test3.ru',
                    login: 'login3'
                }
            }]);
        });
    });

    describe('getDomainStatus', () => {
        it('should call `get-domenator-domain-status/v1` model', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.getDomainStatus();

            expect(result).toEqual({
                domain: 'test.ru',
                login: 'my-login',
                status: 'registered'
            });

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=get-domenator-domain-status/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models).toEqual([{
                name: 'get-domenator-domain-status/v1',
                params: {}
            }]);
        });

        it('should call `get-domenator-domain-status/v1` model with specified domain', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.getDomainStatus('my-awesome-domain.ru');

            expect(result).toEqual({
                domain: 'my-awesome-domain.ru',
                login: 'my-login',
                status: 'registered'
            });

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=get-domenator-domain-status/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models).toEqual([{
                name: 'get-domenator-domain-status/v1',
                params: {
                    domain: 'my-awesome-domain.ru'
                }
            }]);
        });
    });

    describe('cancelSubscription', () => {
        it('should call `domenator-cancel-subscription/v1` model', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.cancelSubscription();

            expect(result).toEqual({});

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=domenator-cancel-subscription/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models).toEqual([{
                name: 'domenator-cancel-subscription/v1',
                params: {}
            }]);
        });

        it('should call `get-domenator-domain-status/v1` model with specified domain', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.cancelSubscription('my-awesome-domain-2.ru');

            expect(result).toEqual({});

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=domenator-cancel-subscription/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models).toEqual([{
                name: 'domenator-cancel-subscription/v1',
                params: {
                    domain: 'my-awesome-domain-2.ru'
                }
            }]);
        });
    });

    describe('changeLogin', () => {
        it('should call `domenator-change-login/v1` model with specified login', async () => {
            const api = getApi(DEFAULT_API_PARAMS);
            const result = await api.changeLogin('new-login');

            expect(result).toEqual({});

            expect(fetchMock).toBeCalledTimes(1);
            expect(fetchMock.mock.calls[0][0]).toEqual('https://web-api.ya.ru/models/?_m=domenator-change-login/v1');
            expect(JSON.parse(String(fetchMock.mock.calls[0][1].body)).models).toEqual([{
                name: 'domenator-change-login/v1',
                params: {
                    login: 'new-login'
                }
            }]);
        });
    });
});
