import nock from 'nock';

import { IMagicLink } from './types/IMagicLink';
import { IUserData } from './types/IUserData';
import { Cache } from './utils/Cache';
import { version } from './utils/const';
import { MagicResolver } from './MagicResolver';
import { LoginExpander } from './LoginExpander';

function getMagicHostName() {
    const r = String(Math.random() * Math.random());

    return `magiclinks-${r.split('.').pop()}.yandex-team.ru`;
}

function getLoginHostName() {
    const r = String(Math.random() * Math.random());

    return `schi-${r.split('.').pop()}.yandex-team.ru`;
}

describe('MagicResolver', () => {
    it('Should be an instance of MagicResolver', () => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        expect(resolver).toBeInstanceOf(MagicResolver);
    });

    it('Should return magic link data', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://test1.com': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const link = await resolver.pull('https://test1.com') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(true);
        expect(typeof link.now).toBe('number');
    });

    it('Should use existing promise', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://test1.com': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const promises = [
            resolver.pull('https://test1.com'),
            resolver.pull('https://test1.com'),
        ];

        await Promise.all(promises);

        expect(promises[0]).toStrictEqual(promises[1]);
    });

    it('Should buffer request', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://test1.com': {
                        ttl: 61,
                        completed: true,
                        value: [],
                    },
                    'https://test2.com': {
                        ttl: 62,
                        completed: true,
                        value: [],
                    },
                },
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 10,
            maxBufferTime: 100,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const promises = [
            resolver.pull('https://test1.com'),
            resolver.pull('https://test2.com'),
        ];

        const links = await Promise.all(promises) as IMagicLink[];

        expect(links[0].ttl).toBe(61000);
        expect(links[0].completed).toBe(true);

        expect(links[1].ttl).toBe(62000);
        expect(links[1].completed).toBe(true);
    });

    it('Should support maxBulkSize', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        const responses = [
            {
                data: {
                    'https://test1.com': {
                        ttl: 61,
                        completed: true,
                        value: [],
                    },
                    'https://test2.com': {
                        ttl: 62,
                        completed: true,
                        value: [],
                    },
                },
            },
            {
                data: {
                    'https://test3.com': {
                        ttl: 63,
                        completed: true,
                        value: [],
                    },
                },
            },
        ];
        let count = 0;

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .times(2)
            .reply(200, () => responses[count++]);

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 2,
            maxBufferTime: 100,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const promises = [
            resolver.pull('https://test1.com'),
            resolver.pull('https://test2.com'),
            resolver.pull('https://test3.com'),
        ];

        const links = await Promise.all(promises) as IMagicLink[];

        expect(links[0].ttl).toBe(61000);
        expect(links[0].completed).toBe(true);

        expect(links[1].ttl).toBe(62000);
        expect(links[1].completed).toBe(true);

        expect(links[2].ttl).toBe(63000);
        expect(links[2].completed).toBe(true);
    });

    it('Should clear cache on server command', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                drop_client_cache: true,
                data: {
                    'https://test1.com': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const cache = new Cache<IMagicLink>({ storeName: magicHostName });

        cache.clear = jest.fn();

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache,
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const link = await resolver.pull('https://test1.com') as IMagicLink;

        expect(cache.clear).toHaveBeenCalled();

        expect(link.ttl).toStrictEqual(0);
    });

    it('Should support cache', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://test1.com': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        let link;

        link = await resolver.pull('https://test1.com') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(true);

        link = await resolver.pull('https://test1.com') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(true);
    });

    it('Should not cache incomplete links', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        const spy = jest.fn();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .times(2)
            .reply(200, () => {
                spy();
                return {
                    data: {
                        'https://test1.com': {
                            ttl: 60,
                            completed: false,
                            value: [],
                        },
                    },
                };
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        let link;

        link = await resolver.pull('https://test1.com') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(false);

        link = await resolver.pull('https://test1.com') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(false);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    it('Should little fix startrek comment links', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://st.yandex-team.ru/WIKI-123#foo': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const link = await resolver.pull('https://st.yandex-team.ru/WIKI-123#foo') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(true);
        expect(link.value).toEqual([
            {
                type: 'string',
                color: 'gray',
                value: '#foo',
            },
        ]);
    });

    it('Should resolve user names', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://st.yandex-team.ru/WIKI-123': {
                        ttl: 60,
                        completed: true,
                        value: [
                            {
                                type: 'string',
                                value: 'test',
                            },
                            {
                                type: 'user',
                                login: 'vas',
                            },
                        ],
                    },
                },
            });

        nock(`https://${loginHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/inflector', {
                login: 'vas',
                lang: 'foo',
            })
            .query({
                _$client: `@yandex-int/magiclinks@${version}`,
                _$origin: window.location.origin,
            })
            .reply(200, {
                name: 'Василий',
                lastName: 'Пупкин',
                isDismissed: false,
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                lang: 'foo',
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const link = await resolver.pull('https://st.yandex-team.ru/WIKI-123') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(true);
        expect(link.value).toEqual([
            {
                type: 'string',
                value: 'test',
            },
            {
                type: 'user',
                login: 'vas',
                value: {
                    name: 'Василий',
                    lastName: 'Пупкин',
                    isDismissed: false,
                },
            },
        ]);
    });

    it('Should fail on server errors', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(500, 'Backend unavailable');

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        try {
            await resolver.pull('https://test1.com');

            throw new Error('It does not throw!');
        } catch (error) {
            expect(error.message).toStrictEqual('Backend unavailable');
        }
    });

    it('Should not fail if no data', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {},
            });

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache: new Cache<IMagicLink>({ storeName: magicHostName }),
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        const link = await resolver.pull('https://test1.com');

        expect(link).toBeUndefined();
    });

    it('Should not fail queue on unexpected errors', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {},
            });

        const cache = new Cache<IMagicLink>({ storeName: magicHostName });
        let thrown = false;

        cache.get = async() => {
            if (thrown) {
                return undefined;
            }

            thrown = true;

            throw new Error('Unexpected error');
        };

        const resolver = new MagicResolver({
            magicEndpoint: {
                protocol: 'https:',
                hostname: magicHostName,
                pathname: '/magiclinks/v1/links/',
            },
            maxBulkSize: 1,
            maxBufferTime: 0,
            requestDelay: 0,
            cache,
            expander: new LoginExpander({
                loginEndpoint: {
                    protocol: 'https:',
                    hostname: loginHostName,
                    pathname: '/inflector',
                },
                cache: new Cache<IUserData>({ storeName: loginHostName }),
            }),
        });

        let link;

        try {
            await resolver.pull('https://test1.com');

            throw new Error('It does not throw!');
        } catch (error) {
            expect(error.message).toStrictEqual('Unexpected error');
        }

        link = await resolver.pull('https://test1.com');

        expect(link).toBeUndefined();
    });
});
