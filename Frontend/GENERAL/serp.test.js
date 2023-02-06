describe('serp', function() {
    var requestTest, location, serpBlocks, serpInstance, sandbox;

    function createSerpInstance() {
        serpInstance = buildDomBlock('serp-test', {
            block: 'serp-test',
            mix: {
                block: 'serp',
                js: { uniqId: 'search', testing: true }
            },
            js: true
        });
    }

    before(function() {
        requestTest = BEM.blocks['serp-request-test'];
        location = BEM.blocks.location.getInstance();

        sinon.stub(BEM.blocks['i-ua'], 'canUseAjax').returns(true);
        sinon.stub(BEM.blocks['serp-request'], 'reloadPage');

        sinon.stub(location, 'change').callsFake(function() {
            this.trigger('change', {});
        });
    });

    after(function() {
        location.change.restore();
        BEM.blocks['i-ua'].canUseAjax.restore();
        BEM.blocks['serp-request'].reloadPage.restore();
    });

    beforeEach(function() {
        requestTest.stubGlobals();

        serpBlocks = BEM.blocks.serp.BLOCKS;
        BEM.blocks.serp.BLOCKS = ['serp-test'];

        sandbox = sinon.createSandbox();
    });

    afterEach(function() {
        if (serpInstance) {
            serpInstance.destruct();
            serpInstance = null;
        }

        BEM.blocks.serp.BLOCKS = serpBlocks;

        requestTest.resetState();
        requestTest.clearSerpRequest();

        sandbox.restore();
    });

    it('should request blocks data on location change', function(done) {
        requestTest.stubRequest(function() {
            var requestData = this.getRequestData();
            assert.includeMembers(Object.keys(requestData), ['serp', 'serp-test']);
            done();
        });

        createSerpInstance();
    });

    it('should get data from getAjaxData method', function(done) {
        requestTest.stubGetMethod(function(params) {
            var data = JSON.parse(params.ajax);
            assert.nestedPropertyVal(data, 'serp-test.foo', 'bar');
            done();
        });

        createSerpInstance();
    });

    it('should call ajaxPrepare back', function(done) {
        var prepareSpy = sinon.spy(),
            stubs = stubBlockPrototype('serp-test', {
                onAjaxPrepare: prepareSpy
            });
        stubs.init();
        requestTest.addStubToRestore(stubs);

        requestTest.stubGetMethod(function() {
            assert.calledOnce(prepareSpy);
            done();
        });

        createSerpInstance();
    });

    it('should call ajaxSuccess back', function(done) {
        var successSpy = sinon.spy(),
            stubs = stubBlockPrototype('serp-test', {
                onAjaxSuccess: successSpy
            });
        stubs.init();
        requestTest.addStubToRestore(stubs);

        requestTest.stubGetMethod(function(params, onSuccess) {
            var response = requestTest.createResponse({
                'serp-test': 'such data'
            });
            onSuccess(response);

            setTimeout(function() {
                assert.calledOnce(successSpy);
                done();
            }, 0);
        });

        createSerpInstance();
    });

    describe('events', function() {
        var startSpy, finishSpy;

        beforeEach(function() {
            startSpy = sinon.spy();
            finishSpy = sinon.spy();

            BEM.channel('ajax').on({
                successCallbacksStarted: startSpy,
                successCallbacksFinished: finishSpy
            });
        });

        afterEach(function() {
            BEM.channel('ajax').un({
                successCallbacksStarted: startSpy,
                successCallbacksFinished: finishSpy
            });
        });

        it('should trigger successCallbacksStarted and successCallbacksFinished once with one request', function(done) {
            // Более комплексные тесты – в Hermione, см. SERP-43948
            var response = requestTest.createResponse({ 'serp-test': 'such data' });

            requestTest.stubGetMethod(function(params, onSuccess) {
                onSuccess(response);

                setTimeout(function() {
                    assert.calledOnce(startSpy);
                    assert.calledOnce(finishSpy);
                    done();
                }, 0);
            });

            createSerpInstance();
        });
    });
});

/**
 * Тестовый блок
 */
BEM.DOM.decl('serp-test', {
    onSetMod: {
        'serp-bound': {
            yes: function() {
                BEM.blocks.location.getInstance().change({ url: 'http://ya.ru' });
            }
        }
    },

    getAjaxData: function() {
        return { foo: 'bar' };
    },

    onAjaxPrepare: $.noop,
    onAjaxSuccess: $.noop,
    onAjaxError: $.noop
});
