import nock from 'nock';

import { IUserData } from './types/IUserData';
import { Cache } from './utils/Cache';
import { version } from './utils/const';
import { LoginExpander } from './LoginExpander';

describe('LoginExpander', () => {
    it('Should be an instance of LoginExpander', () => {
        const expander = new LoginExpander({
            cache: new Cache<IUserData>({ storeName: 'test' }),
        });

        expect(expander).toBeInstanceOf(LoginExpander);
    });

    it('Should expand login', async() => {
        const login = String(Math.random());

        nock('https://schi.yandex-team.ru')
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/inflector', {
                login,
                lang: 'foo',
            })
            .query({
                _$client: `@yandex-int/magiclinks@${version}`,
                _$origin: location.origin,
            })
            .reply(200, {
                name: 'Name',
                lastName: 'LastName',
                isDismissed: false,
            });

        const expander = new LoginExpander({
            lang: 'foo',
            loginEndpoint: {
                protocol: 'https:',
                hostname: 'schi.yandex-team.ru',
                pathname: '/inflector',
            },
            cache: new Cache<IUserData>({ storeName: 'test' }),
        });
        const user = await expander.expand(login);

        expect(user).toEqual({
            name: 'Name',
            lastName: 'LastName',
            isDismissed: false,
        });
    });

    it('Should use existing promise', async() => {
        const login = String(Math.random());

        nock('https://schi.yandex-team.ru')
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/inflector', {
                login,
                lang: 'foo',
            })
            .query({
                _$client: `@yandex-int/magiclinks@${version}`,
                _$origin: location.origin,
            })
            .reply(200, {
                name: 'Name',
                lastName: 'LastName',
                isDismissed: false,
            });

        const expander = new LoginExpander({
            lang: 'foo',
            loginEndpoint: {
                protocol: 'https:',
                hostname: 'schi.yandex-team.ru',
                pathname: '/inflector',
            },
            cache: new Cache<IUserData>({ storeName: 'test' }),
        });
        const users = await Promise.all([
            expander.expand(login),
            expander.expand(login),
        ]);

        expect(users).toEqual([
            {
                name: 'Name',
                lastName: 'LastName',
                isDismissed: false,
            },
            {
                name: 'Name',
                lastName: 'LastName',
                isDismissed: false,
            },
        ]);
    });

    it('Should use cache', async() => {
        const login = String(Math.random());

        nock('https://schi.yandex-team.ru')
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/inflector', {
                login,
                lang: 'foo',
            })
            .query({
                _$client: `@yandex-int/magiclinks@${version}`,
                _$origin: location.origin,
            })
            .reply(200, {
                name: 'Name',
                lastName: 'LastName',
                isDismissed: false,
            });

        const expander = new LoginExpander({
            lang: 'foo',
            loginEndpoint: {
                protocol: 'https:',
                hostname: 'schi.yandex-team.ru',
                pathname: '/inflector',
            },
            cache: new Cache<IUserData>({ storeName: 'test' }),
        });

        expect(await expander.expand(login)).toEqual({
            name: 'Name',
            lastName: 'LastName',
            isDismissed: false,
        });

        expect(await expander.expand(login)).toEqual({
            name: 'Name',
            lastName: 'LastName',
            isDismissed: false,
        });
    });

    it('Should return undefined on api error', async() => {
        const login = String(Math.random());

        nock('https://schi.yandex-team.ru')
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/inflector', {
                login,
                lang: 'foo',
            })
            .query({
                _$client: `@yandex-int/magiclinks@${version}`,
                _$origin: location.origin,
            })
            .reply(500, 'Backend unavailable');

        const expander = new LoginExpander({
            lang: 'foo',
            loginEndpoint: {
                protocol: 'https:',
                hostname: 'schi.yandex-team.ru',
                pathname: '/inflector',
            },
            cache: new Cache<IUserData>({ storeName: 'test' }),
        });

        expect(await expander.expand(login)).toBeUndefined();
    });
});
