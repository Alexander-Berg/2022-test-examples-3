/* global experimentarium, RequestCtx */

describeBlock('serp-request', block => {
    const dataConfig = { staticHost: 'http://static-host.ya.ru/web4/' };
    const serpRes = { js: { foo: 'bar' } };
    let data;
    let res;

    stubBlocks(
        'i-global__params',
        'i-console',
        'serp',
        'serp-request__answer',
        'serp-request__handlers',
        'serp-request__ajax-response-assets',
        'assets-tests',
        'RequestCtx'
    );

    beforeEach(() => {
        data = stubData();

        data.ajaxData = {};
        data.config = dataConfig;

        data.expFlags = data.expFlags || {};

        blocks['serp'].returns(serpRes);
    });

    it('should not try to update metadata, if no request object is present', () => {
        data.ajaxData = null;

        block(data);

        assert.notCalled(data.bundles.setMetadata);
        assert.notCalled(data.assets.setMetadata);
    });

    it('should correctly update metadata, if request object is present', () => {
        data.ajaxData = {
            bmt: 'testBmt',
            amt: 'testAmt'
        };

        block(data);

        assert.calledOnce(data.bundles.setMetadata);
        assert.calledWith(data.bundles.setMetadata, 'testBmt');

        assert.calledOnce(data.assets.setMetadata);
        assert.calledWith(data.assets.setMetadata, 'testAmt');
    });

    it('should send console logs in the internal network', () => {
        blocks['i-console'].returns('logs');
        data.isYandexNet = true;

        res = block(data);

        assert.propertyVal(res, 'logs', 'logs');
    });

    it('should not send console logs in the external network', () => {
        blocks['i-console'].returns('logs');
        RequestCtx.GlobalContext.isYandexNet = false;

        res = block(data);

        assert.notOk(res.logs, 'Logs sends in external network');
    });

    describe('common params', () => {
        beforeEach(() => {
            res = block(data);
        });

        it('should contain a cnt', () => {
            assert.propertyVal(res, 'cnt', 'pageview_candidate');
        });

        it('should contain a static-host', () => {
            assert.propertyVal(res, 'static-host', dataConfig.staticHost);
        });

        it('should call serp with false second argument', () => {
            assert.calledWith(blocks['serp'], data, false);
        });
    });

    describe('common params with dontUpdateGlobal', () => {
        beforeEach(() => {
            data.ajaxData.dug = 1; // dontUpdateGlobal
            res = block(data);
        });

        it('should contain a cnt', () => {
            assert.propertyVal(res, 'cnt', 'pageview_candidate');
        });

        it('should contain a static-host', () => {
            assert.propertyVal(res, 'static-host', dataConfig.staticHost);
        });

        it('should call serp with true second argument', () => {
            assert.calledWith(blocks['serp'], data, true);
        });
    });

    describe('serp params', () => {
        beforeEach(() => {
            res = block(data);
        });

        it('should be equal serp.js property', () => {
            assert.nestedProperty(res, 'serp.params');
            assert.deepEqual(res.serp.params, serpRes.js);
        });
    });

    describe('answer', () => {
        it('should contain only allowed handlers', () => {
            const allowedBlocks = ['test1', 'test2'];
            blocks['serp-request__handlers'].returns(allowedBlocks);
            blocks['serp-request__answer'].returns('answer');

            data.ajaxData = { test1: '', test2: '', test3: '' };

            res = block(data);

            const resKeys = Object.keys(res);
            _.each(allowedBlocks, function(name) {
                assert.include(resKeys, name);
            });
            assert.notInclude(resKeys, 'test3');
        });

        it('should place main at the first place', () => {
            const allowedBlocks = ['test1', 'main', 'test2'];
            blocks['serp-request__handlers'].returns(allowedBlocks);
            blocks['serp-request__answer'].returns('answer');

            data.ajaxData = { test1: '', test2: '', main: '', test3: '' };

            res = block(data);

            const resKeys = Object.keys(res).filter(key => _.includes(allowedBlocks, key));

            assert.propertyVal(resKeys, 0, 'main');
        });
    });
});

describeBlock('serp-request__answer', block => {
    const data = stubData();
    let res;

    after(() => {
        blocks['serp-request__answer-test'] = null;
    });

    it('should return an empty object when block is not a function', () => {
        blocks['serp-request__answer-test'] = {};

        res = block(data, 'serp-request__answer-test');

        assert.isObject(res);
        assert.ok(_.isEmpty(res), 'Response is not empty');
    });

    it('should pass a block param when it defined', () => {
        blocks['serp-request__answer-test'] = sinon.spy();

        block(data, 'serp-request__answer-test', 'param');

        assert.calledWith(blocks['serp-request__answer-test'], data, 'param');
    });

    it('should pass a block params list when it defined', () => {
        blocks['serp-request__answer-test'] = sinon.spy();

        block(data, 'serp-request__answer-test', ['param1', 'param2']);

        assert.calledWith(blocks['serp-request__answer-test'], data, 'param1', 'param2');
    });

    it('should pass only data when no params', () => {
        blocks['serp-request__answer-test'] = sinon.spy();

        block(data, 'serp-request__answer-test');

        assert.calledWith(blocks['serp-request__answer-test'], data);
    });

    it('should render block HTML', () => {
        blocks['serp-request__answer-test'] = _.constant({ block: 'test', content: 'Test' });

        res = block(data, 'serp-request__answer-test', {}, { global: 'params' });

        assert.match(res.html, /<div class=.*?test.*?>.*?Test.*?<\/div>/);
    });

    it('should handle a params field', () => {
        blocks['serp-request__answer-test'] = _.constant({ js: 'params' });

        res = block(data, 'serp-request__answer-test');

        assert.propertyVal(res, 'params', 'params');
    });

    it('should handle a scripts field', () => {
        blocks['serp-request__answer-test'] = _.constant({ scripts: ['jquery.js', 'lodash.js'] });

        res = block(data, 'serp-request__answer-test');

        assert.deepEqual(res.scripts, ['jquery.js', 'lodash.js']);
    });
});

describeBlock('serp-request__ajax-response-assets', block => {
    stubBlocks(
        'i-global__params',
        'b-page__get-css-only-experiment_postsearch',
        'RequestCtx'
    );

    beforeEach(() => {
        experimentarium._setFlags({ test_flag: 1 });
        experimentarium.addConfig('test_flag', { bundleAutoPush: true });
    });

    afterEach(() => {
        experimentarium.addConfig('test_flag', undefined);
    });

    it('should return assets in correct format', () => {
        const data = stubData();
        const excludedPopularBundle = null;
        const emptyBundle = '';

        blocks['i-global__params'].returns({ params: { nonce: '1234' } });
        blocks['b-page__get-css-only-experiment_postsearch'].returns(['cssOnlyExpsCSS;']);

        data.bundles.getBundlesCSS.returns(['bundleCSS;']);
        data.bundles.getBundlesDeferrableCSS.returns(['bundleDeferrableCSS;']);
        data.assets.getCss.returns(['assetCSS;']);
        data.assets.getCssSrc.returns([{ block: 'b-page', elem: 'css', url: 'assetCSSURL' }]);
        data.bundles.getBundlesJS.returns(['bundleJS;', excludedPopularBundle, emptyBundle]);
        data.assets.getJs.returns(['assetJS;']);
        data.assets.getJsSrc.returns([{ block: 'b-page', elem: 'js', url: 'assetJSURL' }]);
        data.assets.getExternal.returns([{ id: 'react-with-dom', src: '//react.min.js', counterId: 1 }]);

        assert.deepEqual(block(data), {
            assets: [
                {
                    type: 'style',
                    content: 'bundleCSS;bundleDeferrableCSS;assetCSS;cssOnlyExpsCSS;'
                },
                {
                    type: 'style',
                    url: 'assetCSSURL'
                },
                {
                    type: 'script',
                    attrs: {
                        id: 'react-with-dom',
                        src: '//react.min.js',
                        'data-rcid': 1
                    }
                },
                {
                    type: 'script',
                    content: 'bundleJS;assetJS;'
                },
                {
                    type: 'script',
                    url: 'assetJSURL'
                }
            ]
        });
    });
});
