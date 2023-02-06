describeBlock('i-global__counters', function(block) {
    var blockstat,
        data,
        counterFuncs = ['counter', 'counter_data', 'counter_data_no_blockstat'],
        counterToBlockstat = {
            counter: 'counterDataWithPath',
            counter_data: 'counterData',
            counter_data_no_blockstat: 'counterDataNoBlockstat'
        },
        wzrd = {};

    stubBlocks('RequestCtx');

    beforeEach(function() {
        blockstat = {
            clickPrefix: sinon.stub().returns('prefix'),
            clickHost: _.noop,
            clickHostParams: sinon.stub().returns(''),
            redirFrom: _.noop,
            redirPrefix: sinon.stub().returns({ referer: 'https://yandex.ru/search/?text=котики' }),
            counterData: (function(stub) {
                // Для sinon'а важно снисходящее количество аргументов, иначе не срабатывает требуемая логика
                // И никакого чейна, иначе не работает

                stub
                    .withArgs('web/wizard/tail', 'n1', 'v1', 'pos', 'p0', 'source', 'wizard')
                    .returns(['80.358.919', 'n1=v1,pos=p0,source=wizard']);

                stub
                    .withArgs('/path', 'n1', 'v1', 'n2', 'v2')
                    .returns(['1861', 'n1=v1,n2=v2']);

                stub
                    .withArgs('/path')
                    .returns(['1861']);

                return stub;
            })(sinon.stub()),
            counterDataWithPath: (function(stub) {
                // Для sinon'а важно снисходящее количество аргументов, иначе не срабатывает требуемая логика
                // И никакого чейна, иначе не работает

                stub
                    .withArgs('web/wizard/tail', 'n1', 'v1', 'pos', 'p0', 'source', 'wizard')
                    .returns({
                        path: '/web/wizard/tail',
                        result: ['80.358.919', 'n1=v1,pos=p0,source=wizard']
                    });

                stub
                    .withArgs('/path', 'n1', 'v1', 'n2', 'v2')
                    .returns({
                        path: '/path',
                        result: ['1861', 'n1=v1,n2=v2']
                    });

                stub
                    .withArgs('/path')
                    .returns({
                        path: '/path',
                        result: ['1861']
                    });

                return stub;
            })(sinon.stub()),
            counterDataNoBlockstat: sinon.stub().returns([0, 1])
        };

        data = _.merge(stubData('cgi', 'device', 'experiments'), {
            reqdata: {
                blockstat: blockstat,
                ruid: 'yandexuid'
            },
            clickPrefix: 'prefix',
            counterPrefix: 'web',
            redirPrefixCore: sinon.stub().returns({ counterPath: '/path', result: 'prefix' }),
            tld: ''
        });
    });

    describe('counters', function() {
        beforeEach(function() {
            block(data);
        });

        _.forEach(counterFuncs, function(fn) {
            var blockstatFn = counterToBlockstat[fn];

            describe(fn, function() {
                describe('with string pairs vars', function() {
                    it('should pass correct args to blockstat', function() {
                        data[fn]('/path', 'n1', 'v1', 'n2', 'v2');
                        assert.calledWithExactly(blockstat[blockstatFn], '/path', 'n1', 'v1', 'n2', 'v2');
                    });
                });
            });

            describe(fn, function() {
                describe('with object as base', function() {
                    it('should pass correct args to blockstat', function() {
                        data[fn]({
                            pos: 0,
                            types: { kind: 'wizard' },
                            counter_prefix: 'wizard'
                        }, 'tail', 'n1', 'v1');
                        assert.calledWithExactly(
                            blockstat[blockstatFn],
                            'web/wizard/tail',
                            'n1', 'v1',
                            'pos', 'p0',
                            'source', 'wizard'
                        );
                    });
                });
            });

            describe(fn, function() {
                describe('with object vars', function() {
                    it('should pass correct args to blockstat', function() {
                        data[fn]('/path', {
                            n1: 'v1',
                            n2: 'v2'
                        });
                        assert.calledWithExactly(
                            blockstat[blockstatFn],
                            '/path',
                            'n1', 'v1',
                            'n2', 'v2'
                        );
                    });
                });
            });

            describe(fn, function() {
                describe('with object as base (with tail) and object vars', function() {
                    it('should pass correct args to blockstat', function() {
                        data[fn]({
                            pos: 0,
                            types: { kind: 'wizard' },
                            counter_prefix: 'wizard'
                        },
                        'tail',
                        { n1: 'v1' });
                        assert.calledWithExactly(
                            blockstat[blockstatFn],
                            'web/wizard/tail',
                            'n1', 'v1',
                            'pos', 'p0',
                            'source', 'wizard'
                        );
                    });
                });
            });
        });

        describe('counter_w_url', function() {
            it('should return valid result with vars', function() {
                var expect = ['w', '1861', 'n1=v1,n2=v2', 'url'],
                    result = data.counter_w_url(wzrd, '/path', 'n1', 'v1', 'n2', 'v2', 'url');
                assert.deepEqual(result, expect);
            });

            it('should return valid result without vars', function() {
                var expect = ['w', '1861', 'url'],
                    result = data.counter_w_url(wzrd, '/path', 'url');
                assert.deepEqual(result, expect);
            });
        });
    });

    describe('isInternalUrl', function() {
        beforeEach(function() {
            block(data);
        });

        every(
            [
                'https://internet.yandex.ru',
                'https://internet.yandex.com.tr',
                'https://yastatic.net/butterfly',
                'https://yandex.com.tr/internet/',
                'https://yandex.ru/internet/'
            ],
            'should return true',
            function(url) {
                assert.equal(data.isInternalUrl(url), true);
            }
        );

        every(
            [
                'https://yandex.com.com.ru/internet/',
                'https://yandex.com.com.tr/internet/',
                'http://yandex.ru/clck?js=redir',
                'http://yandex.ru/clck/jsredir',
                'https://mayastatic.net',
                'https://fishyandex.ru/',
                'http://yandex.coma.tr',
                'http://yandex.vasya.ru',
                'https://www.petrolplus.ru/fuelindex'
            ],
            'should return false',
            function(url) {
                assert.equal(data.isInternalUrl(url), false);
            }
        );
    });

    describe('convertVarsToArray', function() {
        beforeEach(function() {
            block(data);
        });

        it('should convert plain object to flat string array of keys and values', function() {
            assert.deepEqual(
                data.convertVarsToArray({
                    str0: null,
                    str1: '1',
                    str2: 2,
                    str3: undefined,
                    str4: '4v',
                    str5: !true,
                    str7: '7',
                    str8: ''
                }),
                ['str0', null, 'str1', '1', 'str2', 2, 'str4', '4v', 'str5', false, 'str7', '7', 'str8', '']
            );
        });
    });

    describe('redirCounterUrl', function() {
        stubBlocks('Util');

        it('should call with isJsredir=2', function() {
            data.isForeign = false;

            block(data);
            data.redirCounterUrl('http://ya.ru/', {});
            assert.equal(Util.signUrl.args[0][5], 2);
        });
    });
});
