import type { StrmClickhouse, StrmProviderID } from '../../db/StrmClickhouse';
import { UserQuery } from './typings';
import { CHController } from './CHController';
import { DataProviderID } from '../../common/QueryTypings';
import { Logger } from '../logger';
import { Cache } from '../../db/Cache';
import { Clickhouse } from '../../db/Clickhouse';
import { FetchDataSuccess } from '../../common/serverResponseTypings';

const gogol = require('./test_data/gogol.json');
const strm = require('./test_data/strm.json');
const perflog = require('./test_data/perflog.json');
const csp = require('./test_data/csp.json');
const errorBooster = require('./test_data/errorBooster.json');
const drm = require('./test_data/drm.json');
const ottAcs = require('./test_data/ottAcs.json');
const ottApp = require('./test_data/ottApp.json');
const genMusic = require('./test_data/genMusic.json');
const testData: Record<DataProviderID, unknown> = {
    gogol,
    strm,
    perflog,
    csp,
    errorBooster,
    drm,
    ottAcs,
    ottApp,
    genMusic,
};

const fakeLogger = new Logger();

describe('CHController', () => {
    let cache: Cache;
    let strm: StrmClickhouse;
    let rum: Clickhouse<'csp' | 'errorBooster'>;
    let drm: Clickhouse<'drm'>;
    let ott: Clickhouse<'ottAcs' | 'ottApp'>;
    let genMusic: Clickhouse<'genMusic'>;
    let chController: CHController;
    let dbs: Record<DataProviderID, StrmClickhouse | Clickhouse<DataProviderID>>;

    beforeEach(() => {
        cache = createCacheMock();
        strm = createStrmClickhouseMock();
        rum = createRumClickhouseMock();
        drm = createDrmClickhouseMock();
        ott = createOttClickhouseMock();
        genMusic = createGenMusicClickhouseMock();
        chController = new CHController({
            cacheDatabase: cache,
            strmDatabase: strm,
            rumDatabase: rum,
            drmDatabase: drm,
            ottDatabase: ott,
            genMusicDatabase: genMusic,
        });
        dbs = {
            gogol: strm,
            strm: strm,
            perflog: strm,
            csp: rum,
            errorBooster: rum,
            drm: drm,
            ottAcs: ott,
            ottApp: ott,
            genMusic: genMusic,
        };
    });

    function requestData(provider: DataProviderID, force: boolean) {
        return new Promise<FetchDataSuccess>(resolve => {
            chController.getData(
                {
                    params: { vsid: 'vsid1' },
                    query: {
                        force,
                        provider,
                        tsFrom: '10',
                        tsTo: '1000',
                    },
                    logger: fakeLogger,
                },
                { send: (data: any) => resolve(data) } as any
            );
        });
    }

    function testGettingData(dataProviderId: DataProviderID) {
        return async() => {
            // проверяем работу кликхауса
            const firstResultFromClickhouse = await requestData(dataProviderId, false);
            expect(cache.getCacheData).toBeCalledTimes(1); // +1 вызов
            expect(cache.upsertCachedData).toBeCalledTimes(1); // +1 вызов
            expect(dbs[dataProviderId].getSessionReport).toBeCalledTimes(1); // +1 вызов

            // проверяем работу кеша
            const resultFromCache = await requestData(dataProviderId, false);
            expect(cache.getCacheData).toBeCalledTimes(2); // +1 вызов
            expect(cache.upsertCachedData).toBeCalledTimes(1); // +0
            expect(dbs[dataProviderId].getSessionReport).toBeCalledTimes(1); // +0

            // проверяем запрос из кликхауса при force==true
            const secondResultFromClickhouse = await requestData(dataProviderId, true);
            expect(cache.getCacheData).toBeCalledTimes(2); // +0
            expect(cache.upsertCachedData).toBeCalledTimes(2); // +1 вызов
            expect(dbs[dataProviderId].getSessionReport).toBeCalledTimes(2); // +1 вызов

            expect(firstResultFromClickhouse.fromCache).toBe(false);

            expect(JSON.stringify(resultFromCache.payload))
                .toEqual(JSON.stringify(firstResultFromClickhouse.payload));
            expect(resultFromCache.fromCache).toBe(true);

            expect(JSON.stringify(secondResultFromClickhouse.payload))
                .toEqual(JSON.stringify(firstResultFromClickhouse.payload));
            expect(secondResultFromClickhouse.fromCache).toBe(false);

            expect(firstResultFromClickhouse).toMatchSnapshot();
        };
    }

    it('get gogol data', testGettingData('gogol'));
    it('get strm data', testGettingData('strm'));
    it('get perflog data', testGettingData('perflog'));
    it('get csp data', testGettingData('csp'));
    it('get errorBooster data', testGettingData('errorBooster'));
    it('get drm data', testGettingData('drm'));
    it('get ott access logs data', testGettingData('ottAcs'));
    it('get ott app logs data', testGettingData('ottApp'));
    it('get generative music data', testGettingData('genMusic'));
});

function createCacheMock(): Cache {
    let caches: Record<string, unknown[] | undefined> = {};
    return {
        upsertCachedData: jest.fn().mockImplementation((data: unknown[], key: string) => {
            caches[key] = data.slice();
            return Promise.resolve();
        }),
        getCacheData: jest.fn().mockImplementation((key: string) => Promise.resolve(caches[key]?.slice())),
    };
}

function createDrmClickhouseMock(): Clickhouse<'drm'> {
    return {
        getSessionReport: jest.fn().mockImplementation(({ provider }: UserQuery<'drm'>) => {
            return testData[provider];
        }),
        createQuery: ({ provider }) => Promise.resolve({ query: `${provider} query`, cluster: 'rum' }),
        isProviderSupported: (id: DataProviderID): id is 'drm' => id === 'drm',
    };
}

function createRumClickhouseMock(): Clickhouse<'csp' | 'errorBooster'> {
    return {
        getSessionReport: jest.fn().mockImplementation(({ provider }: UserQuery<'csp' | 'errorBooster'>) => {
            return testData[provider];
        }),
        createQuery: ({ provider }) => Promise.resolve({ query: `${provider} query`, cluster: 'rum' }),
        isProviderSupported: (id: DataProviderID): id is 'csp' | 'errorBooster' => ['csp', 'errorBooster'].includes(id),
    };
}

function createOttClickhouseMock(): Clickhouse<'ottAcs' | 'ottApp'> {
    return {
        getSessionReport: jest.fn().mockImplementation(({ provider }: UserQuery<'ottAcs' | 'ottApp'>) => {
            return testData[provider];
        }),
        createQuery: ({ provider }) => Promise.resolve({ query: `${provider} query`, cluster: 'ott' }),
        isProviderSupported: (id: DataProviderID): id is 'ottAcs' | 'ottApp' => ['ottAcs', 'ottApp'].includes(id),
    };
}

function createGenMusicClickhouseMock(): Clickhouse<'genMusic'> {
    return {
        getSessionReport: jest.fn().mockImplementation(({ provider }: UserQuery<'genMusic'>) => {
            return testData[provider];
        }),
        createQuery: ({ provider }) => Promise.resolve({ query: `${provider} query`, cluster: 'genMusic' }),
        isProviderSupported: (id: DataProviderID): id is 'genMusic' => id === 'genMusic',
    };
}

function createStrmClickhouseMock(): StrmClickhouse {
    return {
        getSessionReport: jest.fn().mockImplementation(({ provider }: UserQuery<StrmProviderID>) => {
            return (testData[provider] as unknown[]).slice();
        }),
        createQuery: ({ provider }) => Promise.resolve({ query: `${provider} query`, cluster: 'strm' }),
        getMinMaxTimestamps: () => Promise.resolve({ minTS: 0, maxTS: 100 }),
        getOttsessions: () => Promise.resolve([]),
    };
}
