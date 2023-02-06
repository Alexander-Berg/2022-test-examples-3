import tokenHelper from '../../app/token';
import tokenArg from '../../app/token-arg';

jest.mock('../../app/secrets');
jest.mock('@ps-int/ufo-server-side-commons/helpers/bindings-node14');

describe('token', () => {
    describe('getTokenFromRequest', () => {
        it('не должен извлечь токен при отсутствии аргумента', () => {
            const req = {
                query: {}
            };
            expect(tokenHelper.getTokenFromRequest(req)).toBeFalsy();
        });
        it('не должен расшифровать невалидный токен', () => {
            const req = {
                query: {
                    [tokenArg]: 'bullshit'
                }
            };
            expect(tokenHelper.getTokenFromRequest(req)).toBeFalsy();
        });
        it('должен извлечь и расшифровать валидный токен', () => {
            const token = tokenHelper.generate({
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    date: '123',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                }
            });
            const req = {
                query: {
                    [tokenArg]: token.val
                }
            };
            expect(tokenHelper.getTokenFromRequest(req)).toEqual(token);
        });
    });
    describe('generate', () => {
        it('должен взять `url` из `query`, если не используется старый токен', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    url: '/disk/other-file'
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.url).toBe(input.query.url);
        });
        it('должен взять `url` из старого токена, если он используется', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    url: '/disk/other-file'
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.url).toBe(input.token.url);
        });
        it('должен взять `url` из `query`, если используется старый токен, в котором `url` нет', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input, true);
            expect(token.url).toBe(input.query.url);
        });
        it('не должен брать `noiframe` из старого токена, если он не используется', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    noiframe: true
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.noiframe).toBe(false);
        });
        it('должен взять `noiframe` из старого токена, если он используется', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    noiframe: true
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.noiframe).toBe(true);
        });
        it('должен установить `noiframe` для протокола СЕРПа, если не используется старый токен', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-serp:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    noiframe: false
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.noiframe).toBe(true);
        });
        it('должен установить `noiframe` для `http`-протокола, если не используется старый токен', () => {
            const input = {
                parsedUrl: {
                    protocol: 'https:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: 'http://example.com/some-file',
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    noiframe: false
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.noiframe).toBe(true);
        });
        it('должен установить `noiframe`, если имеется `query`-параметр и не используется старый токен', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file',
                    noiframe: true
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    noiframe: false
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.noiframe).toBe(true);
        });
        it('должен установить дату из `query`, если не используется старый токен', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    date: '123',
                    name: 'some-file',
                    noiframe: true
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    date: '124'
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.date).toBe(input.query.date);
        });
        it('должен установить дату из старого токена, если он используется', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    date: '123',
                    name: 'some-file',
                    noiframe: true
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    date: '124'
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.date).toBe(input.token.date);
        });
        it('должен установить дату из `query`-параметра, если используется старый токен, в котором нет даты', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    date: '123',
                    name: 'some-file',
                    noiframe: true
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input, true);
            expect(token.date).toBe(input.query.date);
        });
        it('должен взять заголовок из старого токена, если он используется', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file',
                    noiframe: true
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    title: 'other-file'
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.title).toBe('other-file');
        });
        it('должен взять заголовок из `query`, если не используется старый токен, а в `query` есть параметр `name`', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file',
                    name: 'some-file',
                    noiframe: true
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    name: 'other-file'
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.title).toBe('some-file');
        });
        it('должен взять заголовок из `query`-параметра `url`, если не используется старый токен, и в `query` нет параметра `name`', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    url: '/disk/some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    name: 'other-file'
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.title).toBe('some-file');
        });
        it('должен использовать заголовок по умолчанию, если `query` не содержит нужных параметров, и не используетя старый токен', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {},
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    name: 'other-file'
                }
            };
            const token = tokenHelper.generate(input);
            expect(token.title).toBe('[Untitled]');
        });
        it('не должен выставить `embed`, если нет ни `query`-параметра, ни свойства в старом токене', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input);
            expect(token.embed).toBeUndefined();
        });
        it('должен выставить `embed`, если есть `query`-параметр', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file',
                    embed: '1'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input);
            expect(token.embed).toBe('1');
        });
        it('должен выставить `embed`, если используется старый токен, и в нем есть свойство', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    embed: '1'
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.embed).toBe('1');
        });
        it('должен выставить `embed` из `query`, даже если используется старый токен, в котором есть это свойство', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file',
                    embed: '1'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    embed: '2'
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.embed).toBe('1');
        });
        it('не должен выставить `archive-path`, если нет `query`-параметра, либо свойства в старом токене', () => {
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input);
            expect(token['archive-path']).toBeUndefined();
        });
        it('не должен выставить `archive-path`, если не используется старый токен, имеющий это свойство, а также нет `query`-параметра', () => {
            const archivePath = '/path/to/archive.zip';
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    'archive-path': archivePath
                }
            };
            const token = tokenHelper.generate(input);
            expect(token['archive-path']).toBeUndefined();
        });
        it('должен выставить `archive-path`, если используется старый токен, и в нем есть свойство', () => {
            const archivePath = '/path/to/archive.zip';
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    'archive-path': archivePath
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token['archive-path']).toBe(archivePath);
        });
        it('должен выставить `archive-path` из `query`, если используется старый токен без свойства', () => {
            const archivePath = '/path/to/archive.zip';
            const input = {
                parsedUrl: {
                    protocol: 'ya-disk:'
                },
                originalUrl: 'https://docviewer.ru/*=some-token',
                query: {
                    name: 'some-file',
                    'archive-path': archivePath
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input, true);
            expect(token['archive-path']).toBe(archivePath);
        });
        it('должен выставить СЕРП-параметры для `http`-протокола, если не используется старый токен', () => {
            const query = 'a=a&b=b';
            const input = {
                parsedUrl: {
                    protocol: 'https:'
                },
                originalUrl: `https://docviewer.ru/?${query}`,
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {}
            };
            const token = tokenHelper.generate(input);
            expect(token.serpParams).toEqual(query);
        });
        it('должен выставить СЕРП-параметры из старого токена, если он используется', () => {
            const query = 'a=a&b=b';
            const input = {
                parsedUrl: {
                    protocol: 'https:'
                },
                originalUrl: 'https://docviewer.ru/?c=d',
                query: {
                    name: 'some-file'
                },
                cookies: {
                    yandexuid: 'myuid'
                },
                user: {
                    id: '123'
                },
                token: {
                    serpParams: query
                }
            };
            const token = tokenHelper.generate(input, true);
            expect(token.serpParams).toEqual(query);
        });
    });
    describe('getRedirectForToken', () => {
        it('должен сгенерировать редирект без сохранения `query`', () => {
            const token = {
                val: 'token-val',
                uid: '123'
            };
            const req = {
                query: {
                    a: 'a'
                }
            };
            const redirect = tokenHelper.getRedirectForToken(token, req);
            expect(redirect).toEqual({ pathname: '/view/123/', query: { [tokenArg]: 'token-val' } });
        });
        it('должен сгенерировать редирект с сохранением `query`', () => {
            const token = {
                val: 'token-val',
                uid: '123'
            };
            const req = {
                query: {
                    [tokenArg]: 'other-token-val',
                    a: 'a'
                }
            };
            const redirect = tokenHelper.getRedirectForToken(token, req, { saveQuery: true });
            expect(redirect).toEqual({ pathname: '/view/123/', query: { [tokenArg]: 'token-val', a: 'a' } });
        });
        it('должен сгенерировать редирект с `lang`, если `lang` валиден', () => {
            const token = {
                val: 'token-val',
                uid: '123'
            };
            const req = {
                query: {
                    lang: 'ru'
                }
            };
            const redirect = tokenHelper.getRedirectForToken(token, req, { saveLang: true });
            expect(redirect).toEqual({ pathname: '/view/123/', query: { [tokenArg]: 'token-val', lang: 'ru' } });
        });
        it('должен сгенерировать редирект без `lang`, если `lang` невалиден', () => {
            const token = {
                val: 'token-val',
                uid: '123'
            };
            const req = {
                query: {
                    lang: 'it'
                }
            };
            const redirect = tokenHelper.getRedirectForToken(token, req, { saveLang: true });
            expect(redirect).toEqual({ pathname: '/view/123/', query: { [tokenArg]: 'token-val' } });
        });
    });
});
