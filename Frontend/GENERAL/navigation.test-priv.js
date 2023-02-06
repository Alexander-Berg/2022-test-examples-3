describe('navigation', function() {
    var data, service;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        data = stubData('counters', 'experiments', 'cgi');

        data.wizplaces = {
            navigation: [{
                context: [
                    { full_url: 'url1' },
                    { full_url: 'url2' },
                    { full_url: null },
                    { full_url: 'url3' }
                ],
                types: { main: 'navigation_context' }
            }]
        };

        service = sinon.stub(RequestCtx.Service, 'service').returns({ preset: [], extra: [] });
    });

    afterEach(function() {
        service.restore();
    });

    describeBlock('navigation__items', function(block) {
        stubBlocks(['navigation__item', 'navigation__more', 'navigation__items-visible-count']);

        beforeEach(function() {
            blocks['navigation__item'].returnsArg(1);
            blocks['navigation__more'].returns([{ name: 1 }, { name: 2 }, { name: 3 }, { name: 4 }, { name: 5 }]);
            blocks['navigation__items-visible-count'].returns(3);
        });

        it('should call navigation__item for every item in the list', function() {
            block(data, []);
            assert.equal(blocks['navigation__item'].callCount, 5);
        });

        it('should call navigation__more to add items', function() {
            block(data, []);
            assert.calledOnce(blocks['navigation__more']);
        });

        it('should hide all items after visible count', function() {
            var items = block(data, []);
            assert.notOk(_.get(items[0], 'mix.elemMods.extra', null));
            assert.ok(items[3].mix.elemMods.extra, 'hidden');
        });
    });

    describeBlock('navigation__item', function(block) {
        every(
            [1, '1'],
            'should add selected if item is_current is 1',
            function(value) {
                var item = { is_current: value };
                assert.isTrue(block(data, item).selected);
            }
        );

        every(
            [0, '0', ''],
            'should not add selected, but should add counter: false property if item is_current is not set',
            function(value) {
                var item = { is_current: value };
                assert.isFalse(block(data, item).selected);
            }
        );

        it('should use full_url without making any changes to it', function() {
            var item = { full_url: 'http://full.url/path?params=present' };
            assert.strictEqual(block(data, item).url, item.full_url);
        });

        it('should add parent-reqid if serviceName is video', function() {
            var item = { name: 'video', full_url: 'https://yandex.ru/video/search?text=bmw' };
            data.reqdata.reqid = '1477315446819759-mmeta27-06';
            assert.include(block(data, item).url, '&parent-reqid=1477315446819759-mmeta27-06');
        });
    });

    describeBlock('navigation__more', function(block) {
        var items;

        beforeEach(function() {
            sinon.stub(blocks, 'navigation__item-prepare-full-url').returnsArg(1);
            sinon.stub(blocks, 'navigation__item-prepare').returnsArg(1);
            sinon.stub(blocks, 'navigation__services').returns({
                preset: ['www', 'video'],
                extra: ['maps', 'images', 'kitty']
            });

            items = [
                [{ name: 'service1' }, { name: 'service2' }],
                [{ name: 'maps' }, { name: 'service1' }, { name: 'service2' }],
                [{ name: 'maps' }, { name: 'images' }, { name: 'service1' }, { name: 'service2' }]
            ];
        });

        afterEach(function() {
            blocks['navigation__item-prepare-full-url'].restore();
            blocks['navigation__item-prepare'].restore();
            blocks['navigation__services'].restore();
        });

        it('should not add any elements if all extra services are already in list', function() {
            var result = _.chain(block(data, [{ name: 'maps' }, { name: 'images' }, { name: 'kitty' }]))
                .map('name')
                .value();

            assert.sameMembers(result, ['maps', 'images', 'kitty']);
        });

        it('should return all services if default list is empty', function() {
            var result;

            ['www', 'video', 'maps', 'images', 'kitty'].forEach(function(name) {
                blocks['navigation__item-prepare'].withArgs(data, name, true).returns({ name: name });
            });
            result = _.map(block(data, []), 'name');

            assert.sameMembers(result, ['www', 'video', 'maps', 'images', 'kitty']);
        });

        every(items, 'should return unique extra services which do not exist in list', function(list) {
            var result = _.map(block(data, list), 'service');

            assert.sameMembers(result, ['service1', 'service2']);
        });

        describe('for direct page', function() {
            beforeEach(function() {
                RequestCtx.GlobalContext.report = 'yabs';
                ['www', 'video', 'maps', 'images', 'kitty'].forEach(function(name) {
                    blocks['navigation__item-prepare'].withArgs(data, name, true).returns({ name: name });
                });
            });

            it('should insert direct item', function() {
                var result = _.map(block(data, []), 'name');

                assert.deepEqual(result, ['www', 'video', 'direct_page', 'maps', 'images', 'kitty']);
            });

            it('should set as a current only the direct item', function() {
                var result = _.find(block(data, []), { is_current: true });

                assert.deepEqual(result, { name: 'direct_page', is_current: true });
            });
        });
    });

    describeBlock('navigation__item-prepare', function(block) {
        beforeEach(function() {
            service.returns({
                root: 'root',
                search: '/path?text=',
                params: '&someparam=1'
            });
        });

        afterEach(function() {
            service.restore();
        });

        it('should concat full_url for service in right order', function() {
            var b = block(data, [{ name: 'www' }]);

            assert.equal(b.full_url, 'root/path?text=&someparam=1');
        });
    });
});

describe('navigation', function() {
    var data, service;

    stubBlocks(
        'RequestCtx',
        'navigation__context'
    );

    beforeEach(function() {
        data = stubData('counters', 'experiments', 'cgi', 'device');

        service = sinon.stub(RequestCtx.Service, 'service').returns({ preset: [], extra: [] });
        blocks['navigation__context'].returns([]);
    });

    afterEach(function() {
        service.restore();
    });

    describeBlock('navigation', function(block) {
        it('should not have "more-type" mod in Pumpkin mode', function() {
            data.isPumpkin = true; // в @yandex-lego/serp-header осталась привязка к data.isPumpkin
            RequestCtx.GlobalContext.isPumpkin = true;
            assert.notOk(block(data).mods['more-type']);
        });

        it('should have "type_horizontal" mod', function() {
            assert.equal(block(data).mods['type'], 'horizontal');
        });
    });

    describeBlock('navigation__items-visible-count', function(block) {
        it('should return 8 for all domains', function() {
            assert.equal(block(data), 8);
        });
    });

    describeBlock('navigation__services', function(block) {
        var result;

        describe('default list', function() {
            it('should contain correct items for com domain', function() {
                RequestCtx.GlobalContext.tld = 'com';

                result = block(data);

                assert.deepEqual(result.preset, ['www', 'images', 'video']);
            });

            it('should contain correct items for com.tr domain', function() {
                RequestCtx.GlobalContext.tld = 'com.tr';
                RequestCtx.GlobalContext.isComTr = true;

                result = block(data);

                assert.deepEqual(result.preset, ['www', 'images', 'video', 'maps']);
            });

            every(['ru', 'ua', 'kz', 'by', 'bla'], 'should contain correct items for other domains',
                function(tld) {
                    RequestCtx.GlobalContext.tld = tld;

                    result = block(data);

                    assert.deepEqual(result.preset, ['www', 'images', 'video', 'maps', 'market']);
                });

            every(['com', 'ru', 'com.tr', 'ua', 'kz', 'by', 'bla'],
                'should contain correct items in Pumpkin mode for any domain', function(tld) {
                    RequestCtx.GlobalContext.tld = tld;
                    RequestCtx.GlobalContext.isComTr = tld === 'com.tr';
                    RequestCtx.GlobalContext.isPumpkin = 1;

                    result = block(data);

                    assert.deepEqual(result.preset, ['www', 'images']);
                });

            it('should contain correct items for com.tr domain and "blogs" report', function() {
                RequestCtx.GlobalContext.tld = 'com.tr';
                RequestCtx.GlobalContext.report = 'blogs';

                result = block(data);

                assert.deepEqual(result.preset, ['www', 'images', 'video', 'maps']);
            });

            every(['ru'],
                'should contain correct items for other domains and "blogs" report', function(tld) {
                    RequestCtx.GlobalContext.tld = tld;
                    RequestCtx.GlobalContext.report = 'blogs';

                    result = block(data);

                    assert.deepEqual(result.preset, ['www', 'images', 'video', 'maps', 'market']);
                });
        });

        describe('list under More button', function() {
            it('should contain correct items for Turkey', function() {
                RequestCtx.GlobalContext.tld = 'com.tr';
                RequestCtx.GlobalContext.isComTr = true;

                result = block(data);

                assert.deepEqual(result.extra, ['translate', 'disk', 'mail', 'direct-page', 'all']);
            });

            it('should contain correct items for com domain', function() {
                RequestCtx.GlobalContext.tld = 'com';

                result = block(data);

                assert.deepEqual(result.extra, ['news', 'translate', 'disk', 'mail', 'direct-page']);
            });

            every(['ru', 'ua', 'kz', 'by'], 'should contain correct items for KUBR', function(tld) {
                RequestCtx.GlobalContext.tld = tld;

                result = block(data);

                assert.deepEqual(result.extra, [
                    'news', 'translate', 'music', 'disk', 'mail', 'direct-page', 'all'
                ]);
            });

            it('should contain correct items for other domain', function() {
                RequestCtx.GlobalContext.tld = 'bla';

                result = block(data);

                assert.deepEqual(result.extra, ['news', 'translate', 'music', 'disk', 'mail', 'direct-page', 'all']);
            });

            every(['com', 'ru', 'com.tr', 'ua', 'kz', 'by', 'bla'],
                'should contain no items in Pumpkin mode for any domain', function(tld) {
                    RequestCtx.GlobalContext.tld = tld;
                    RequestCtx.GlobalContext.isPumpkin = 1;

                    result = block(data);

                    assert.lengthOf(result.extra, 0);
                });
        });
    });
});
