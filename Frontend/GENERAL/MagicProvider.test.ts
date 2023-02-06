import nock from 'nock';

import { IMagicLink } from './types/IMagicLink';
import { delay } from './utils/time';
import { Cache } from './utils/Cache';
import { MagicProvider } from './MagicProvider';
import { MagicResolver } from './MagicResolver';
import { LoginExpander } from './LoginExpander';
import { IUserData } from './types/IUserData';

function getMagicHostName() {
    const r = String(Math.random() * Math.random());

    return `magiclinks-${r.split('.').pop()}.yandex-team.ru`;
}

function getLoginHostName() {
    const r = String(Math.random() * Math.random());

    return `schi-${r.split('.').pop()}.yandex-team.ru`;
}

describe('MagicProvider', () => {
    it('Should be an instance of MagicProviderPush', () => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: false,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        expect(provider).toBeInstanceOf(MagicProvider);
    });

    it('Should run push event after push', async() => {
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
                    'https://href.com/1': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: false,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn((url, link) => {
            delete link.now;
        });

        provider.addListener('push', handleProviderPush);

        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(1);
        expect(handleProviderPush).toHaveBeenCalledWith('https://href.com/1', {
            ttl: 60000,
            completed: true,
            value: [],
        });
    });

    it('Should not run data after push and drop', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .times(2)
            .reply(200, {
                data: {
                    'https://href.com/1': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: false,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn((url, link) => {
            delete link.now;
        });
        const handleProviderDrop = jest.fn();

        provider.addListener('push', handleProviderPush);
        provider.addListener('drop', handleProviderDrop);

        provider.push('https://href.com/1');
        provider.drop('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(0);
        expect(handleProviderDrop).toHaveBeenCalledTimes(0);

        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(1);
        expect(handleProviderPush).toHaveBeenCalledWith('https://href.com/1', {
            ttl: 60000,
            completed: true,
            value: [],
        });

        provider.drop('https://href.com/1');

        expect(handleProviderDrop).toHaveBeenCalledTimes(1);
        expect(handleProviderDrop).toHaveBeenCalledWith('https://href.com/1');
    });

    it('Should run data once on multiple equal pushes', async() => {
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
                    'https://href.com/1': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: false,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn();

        provider.addListener('push', handleProviderPush);

        provider.push('https://href.com/1');
        provider.push('https://href.com/1');
        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(1);
    });

    it('Should not run data for unsupported urls', async() => {
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

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: false,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn();

        provider.addListener('push', handleProviderPush);

        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(0);
    });

    it('Should retry incomplete data', async() => {
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
                    'https://href.com/1': {
                        ttl: 60,
                        completed: false,
                        value: [],
                    },
                },
            });

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://href.com/1': {
                        ttl: 60,
                        completed: true,
                        value: [],
                    },
                },
            });

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: false,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn((url, link) => {
            delete link.now;
        });

        provider.addListener('push', handleProviderPush);

        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(2);
        expect(handleProviderPush).toHaveBeenCalledWith('https://href.com/1', {
            ttl: 60000,
            completed: false,
            value: [],
        });
        expect(handleProviderPush).toHaveBeenCalledWith('https://href.com/1', {
            ttl: 60000,
            completed: true,
            value: [],
        });
    });

    it('Should not run data if link becomes incomplete after ttl retry', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        let requests = 0;
        const responses = [
            {
                data: {
                    'https://href.com/1': {
                        ttl: 60,
                        completed: false,
                        value: [],
                    },
                },
            },
            {
                data: {
                    'https://href.com/1': {
                        // ttl retry
                        ttl: 0,
                        completed: true,
                        value: [],
                    },
                },
            },
            {
                data: {
                    'https://href.com/1': {
                        ttl: 60,
                        completed: false,
                        value: [],
                    },
                },
            },
            {
                data: {
                    'https://href.com/1': {
                        ttl: 66,
                        completed: true,
                        value: [],
                    },
                },
            },
        ];

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .times(4)
            .reply(200, () => responses[requests++]);

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: true,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn();

        provider.addListener('push', handleProviderPush);

        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(3);

        const link = provider.peek('https://href.com/1') as IMagicLink;

        expect(link.ttl).toBe(66000);
        expect(link.completed).toBe(true);
    });

    it('Should retry server errors', async() => {
        const magicHostName = getMagicHostName();
        const loginHostName = getLoginHostName();

        let requests = 0;
        const responses = [
            [
                500,
                'Backend unavailable',
            ],
            [
                200,
                {
                    data: {
                        'https://href.com/1': {
                            ttl: 60,
                            completed: true,
                            value: [],
                        },
                    },
                },
            ],
        ];

        nock(`https://${magicHostName}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .times(2)
            .reply(() => responses[requests++]);

        const provider = new MagicProvider({
            retryTimeout: 0,
            watchTtl: true,
            resolver: new MagicResolver({
                magicEndpoint: {
                    protocol: 'https:',
                    hostname: magicHostName,
                    pathname: '/magiclinks/v1/links/',
                },
                maxBulkSize: 10,
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
            }),
        });

        const handleProviderPush = jest.fn();

        provider.addListener('push', handleProviderPush);

        provider.push('https://href.com/1');

        await delay(500);

        expect(handleProviderPush).toHaveBeenCalledTimes(1);

        const link = provider.peek('https://href.com/1') as IMagicLink;

        expect(link.ttl).toBe(60000);
        expect(link.completed).toBe(true);
    });
});
