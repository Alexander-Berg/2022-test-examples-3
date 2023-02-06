describe('fdb-uri', function() {
    const FdbURI = require('../../../src/shared/utils/fdb-uri');
    const URI = require('urijs');
    const host = 'http://localhost';
    const fdbHost = 'http://fdb.sbs.yandex-team.ru';
    const regions = [
        { id: 213, key: 'ru', name: 'Россия' },
        { id: 187, key: 'ua', name: 'Украина' },
    ];
    const fdbURI = new FdbURI(URI, host, regions, fdbHost);

    const query = 'котики';
    const system = 'yandex-web';
    const systemName = 'snippet_4';
    const domain = 'ua';
    const externalFlags = [
        '&waitall=da',
        '&timeout=1000000',
        '&spaces=1 + 1',
        '&srcparams=REALTY=rearr=nbtitle10',
        '&rearr=scheme_Local/Ugc/DryRun=1',
        '&rearr=scheme_Local%2FUgc%2FDryRun%3D1',
        // Такой вариант мы сейчас не поддерживаем, но надо:
        // '&smth_with_ampersands=true&&false',
        // По счастливой случайности это работает:
        '&smth_with_one_ampersand=true&false',
    ];
    const pluginsFlags = [
        '&sbs_plugin=base',
        '&sbs_plugin=body',
    ];
    const flags = externalFlags.join('') + pluginsFlags.join('');
    const beta = 'hamster.yandex';

    describe('direct url', function() {
        const uri = fdbURI.build({ query, system, systemName, domain, flags, beta });

        it('correct host', function() {
            assert.equal(URI.parse(uri).hostname, URI.parse(fdbHost).hostname);
        });

        it('correct path', function() {
            assert.equal(URI.parse(uri).path, '/search');
        });


        describe('correct query parts', function() {
            const parsedQuery = URI.parseQuery(URI.parse(uri).query);

            it('query', function() {
                assert.equal(parsedQuery.query, query);
            });

            it('domain', function() {
                assert.equal(parsedQuery.domain, domain);
            });

            it('system', function() {
                assert.equal(parsedQuery.system, system);
            });

            it('system-name', function() {
                assert.equal(parsedQuery['system-name'], systemName);
            });

            it('region', function() {
                const expectedRegionId = 187;
                assert.equal(parsedQuery.region, expectedRegionId);
            });

            it('external flags', function() {
                assert.equal(parsedQuery.exp_flags, externalFlags.join(''));
            });

            it('plugins', function() {
                assert.deepEqual(parsedQuery.plugins, ['base', 'body']);
            });
        });
    });

    describe('cache', function() {
        it('should be with cache', function() {
            const uri = fdbURI.build({ query, system, domain, flags, beta });
            const parsedQuery = URI.parseQuery(URI.parse(uri).query);

            assert(!('no_cache' in parsedQuery));
        });

        it('should be without cache', function() {
            const uri = fdbURI.build({ query, system, domain, flags, beta, noCache: true });
            const parsedQuery = URI.parseQuery(URI.parse(uri).query);

            assert('no_cache' in parsedQuery);
        });
    });

    describe('debug', function() {
        it('should be without debug', function() {
            const uri = fdbURI.build({ query, system, domain, flags, beta });
            const parsedQuery = URI.parseQuery(URI.parse(uri).query);

            assert(!('debug' in parsedQuery));
        });

        it('should be with debug', function() {
            const uri = fdbURI.build({ query, system, domain, flags, beta, debug: true });
            const parsedQuery = URI.parseQuery(URI.parse(uri).query);

            assert('debug' in parsedQuery);
        });
    });
});
