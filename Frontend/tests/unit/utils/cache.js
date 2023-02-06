const Cache = require('../../../src/server/utils/cache');

describe('Класс Cache', () => {
    const CACHE_KEY = 'key';

    let sandbox, cache, cacheTTL;

    beforeEach(() => {
        sandbox = sinon.createSandbox();

        cacheTTL = 1000;
        cache = new Cache(cacheTTL);
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('.setData', () => {
        it('Должен корректно сохранять данные', () => {
            const data = { a: 'b', b: 3 };
            cache.setData(CACHE_KEY, data);

            assert.deepEqual(data, cache.store[CACHE_KEY].data);
        });

        it('Должен выставлять timestamp при сохранениии данных в кэш', () => {
            const now = Date.now();
            sandbox.stub(Date, 'now').returns(now);

            cache.setData(CACHE_KEY, {});

            assert.strictEqual(now, cache.store[CACHE_KEY].timestamp);
        });
    });

    describe('.getData', () => {
        it('Должен возвращать null, если данные в кэше устарели', () => {
            cache.setData(CACHE_KEY, {});

            const stubTime = Date.now() + cacheTTL;
            sandbox.stub(Date, 'now').returns(stubTime);

            assert.strictEqual(cache.getData(CACHE_KEY), null);
        });
    });
});
