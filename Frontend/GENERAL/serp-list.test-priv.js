describeBlock('serp-list', function(block) {
    var data, result, context;

    stubBlocks([
        'serp-adv',
        'serp-list__result',
        'serp-list__clear-state',
        'serp-list_page_direct',
        'wizards',
        'serp-item',
        'extra-content',
        'construct__context',
        'supply__turbo-make-slider'
    ]);

    beforeEach(function() {
        data = stubData('searchdata', 'cgi', 'experiments', 'i-log', 'navi');
        context = { reportData: data, expFlags: {} };

        data.wizplaces = {
            upper: 'upper',
            important: 'important'
        };

        data.device = { OSFamily: 'Android' };

        _.set(data, 'reqdata.user_time.to_iso', '2018-01-15T16:25:00.000Z');

        blocks['construct__context'].returns(context);
    });

    describe('without data', function() {
        var wizplaces;

        beforeEach(function() {
            blocks['serp-list__result'].returns('ololo');

            wizplaces = data.wizplaces;
            result = block(data);
        });

        it('should call "serp-adv" with "premium" param', function() {
            assert.calledWith(blocks['serp-adv'], context, 'premium');
        });

        it('should call "serp-adv" with "halfpremium" param"', function() {
            assert.calledWith(blocks['serp-adv'], context, 'halfpremium');
        });

        it('should call "serp-list__result"', function() {
            assert.isTrue(blocks['serp-list__result'].called);
        });

        it('should return result of "serp-list__result"', function() {
            assert.equal(result, 'ololo');
        });

        it('should call "wizards" at first with "data.wizplaces.important"', function() {
            assert.deepEqual(blocks['wizards'].firstCall.args, [context, wizplaces.important, 'important']);
        });

        it('should call "wizards" second time with "data.wizplaces.upper"', function() {
            assert.deepEqual(blocks['wizards'].secondCall.args, [context, wizplaces.upper, 'upper']);
        });
    });

    describe('with docs', function() {
        var doc;

        beforeEach(function() {
            doc = {
                num: '4',
                server_descr: 'TEST'
            };

            data.wizplaces = {};
            data.wizplaces[doc.num] = 'qwerty';

            data.searchdata.docs = [doc];
        });

        it('should call "serp-list__clear-state"', function() {
            block(data);

            assert.isTrue(blocks['serp-list__clear-state'].called);
        });

        it('should call blocks["wizards"] before blocks["serp-item"]', function() {
            block(data);

            var wzrdDoc = blocks['wizards'].withArgs(context, data.wizplaces[doc.num]);
            assert(wzrdDoc.calledBefore(blocks['serp-item']), '"wizards" called after "serp-item"');
        });

        it('should call "serp-item" with doc', function() {
            block(data);

            assert.calledWith(blocks['serp-item'], context, doc);
        });

        it('should ignore empty data sources', function() {
            data.searchdata.docs.push({}, undefined);

            block(data);

            const resultLength = blocks['serp-list__result'].getCall(0).args[1];

            assert.lengthOf(resultLength, 0);
        });
    });

    describe('with direct', function() {
        var doc;

        beforeEach(function() {
            doc = {
                num: '4',
                server_descr: 'TEST'
            };
            data.isDirectPage = true;

            data.wizplaces = {};
            data.wizplaces[doc.num] = 'qwerty';

            data.searchdata.docs = [doc];
        });

        it('should call "serp-list__clear-state"', function() {
            block(data);

            assert.isTrue(blocks['serp-list__clear-state'].called);
        });
    });

    describe('visibility counter', function() {
        beforeEach(function() {
            _.set(data, 'log.baobabTree.tree.id', 'abc');
            data.clckHost = '//clckHost';
            data.counter = sinon.stub();
            data.getClickPrefix = sinon.stub().returns('clickPrefix');
            blocks['serp-list__result'].returns({ block: 'serp-list', content: [] });
        });

        function assertSerpListHasVisibilityCounter() {
            result = block(data);
            const item = result.content[0];
            assert.deepEqual(
                _.omit(item, 'mix', 'content'),
                { block: 'serp-list', elem: 'plain-visible' }
            );
            assert.equal(
                // Значение event-id зависит от Math.random(), вырезаем его
                item.content.replace(/%2C%22event-id%22%3A%22\w+%22%2C/, '%2C'),
                'bk848484(https://clckHost/safeclick/clickPrefix/path=471.143.842/vars=-method=prerender,-baobab-event-json=%5B%7B%22event%22%3A%22tech%22%2C%22type%22%3A%22serp-page-open%22%2C%22id%22%3A%22abc%22%2C%22data%22%3A%7B%22method%22%3A%22prerender%22%7D%7D%5D/*//yandex.ru/)'
            );
        }

        it('should be in prerender for any browser', function() {
            data.isPrerender = true;
            RequestCtx.GlobalContext.isSearchApp = false;
            assertSerpListHasVisibilityCounter();
        });

        it('should be in prerender for Android SearchApp', function() {
            data.isPrerender = true;
            data.device = { OSFamily: 'Android' };
            RequestCtx.GlobalContext.isSearchApp = true;
            assertSerpListHasVisibilityCounter();
        });

        it('should be in prerender for iOS SearchApp', function() {
            data.isPrerender = true;
            data.device = { OSFamily: 'iOS' };
            RequestCtx.GlobalContext.isSearchApp = true;
            assertSerpListHasVisibilityCounter();
        });

        it('should be without prerender for SearchApp BroPP', function() {
            data.isPrerender = false;
            data.device = { OSFamily: 'Android' };
            RequestCtx.GlobalContext.isSearchApp = true;
            RequestCtx.GlobalContext.ua = 'YaBrowser BroPP';
            assertSerpListHasVisibilityCounter();
        });
    });
});

describeBlock('serp-list__plain-visible-result-url', function(block) {
    it('should return counters for native logs by default', function() {
        assert.equal(
            block('https://yandex.ru/clck/unit-test'),
            'bk848484(https://yandex.ru/clck/unit-test)',
            'Ссылка на счетчик обернута в неправильную последовательность'
        );
    });

    it('should return counters for prefetch in exp', () => {
        RequestCtx.GlobalContext.expFlags['suggest_prefetch_html'] = 1;

        assert.deepEqual(
            block('https://yandex.ru/clck/unit-test'),
            {
                block: 'html-prefetch-counter',
                content: 'https://yandex.ru/clck/unit-test'
            },
            'Счетчик установлен неправильно'
        );
    });

    it('should return counters for native logs in exp YaBro', () => {
        RequestCtx.GlobalContext.expFlags['suggest_prefetch_html'] = 1;
        RequestCtx.GlobalContext.isYandexSearchBrowser = true;

        assert.equal(
            block('https://yandex.ru/clck/unit-test'),
            'bk848484(https://yandex.ru/clck/unit-test)',
            'Ссылка на счетчик обернута в неправильную последовательность'
        );
    });

    it('should return counters for native logs in exp Searchapp', () => {
        RequestCtx.GlobalContext.expFlags['suggest_prefetch_html'] = 1;
        RequestCtx.GlobalContext.isSearchApp = true;

        assert.equal(
            block('https://yandex.ru/clck/unit-test'),
            'bk848484(https://yandex.ru/clck/unit-test)',
            'Ссылка на счетчик обернута в неправильную последовательность'
        );
    });
});
