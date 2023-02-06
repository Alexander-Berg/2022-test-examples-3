describeBlock('main__content', function(block) {
    var data = stubData('cgi');

    stubBlocks([
        'main__carousel-block',
        'content',
        'inquire'
    ]);

    it('should not call main__carousel-block if isPumpkin is true', function() {
        data.isPumpkin = true;

        block(data);

        assert.notCalled(blocks['main__carousel-block']);
    });
});

describeBlock('main__ajax-response-content', function(block) {
    var data,
        sandbox;

    stubBlocks(['main__content', 'main__push-favicons']);

    beforeEach(function() {
        data = stubData('cgi', 'experiments');
        sandbox = sinon.createSandbox();
        blocks['main__content'].returns({
            content: 'TEST_MAIN_CONTENT'
        });
        blocks['main__push-favicons'].returns({});
    });

    afterEach(function() {
        sandbox.restore();
    });

    it('should return result of BEMHTML.apply of main__content block', function() {
        sandbox.stub(BEMHTML, 'apply').withArgs('TEST_MAIN_CONTENT').returns('TEST_APPLY');

        var result = block(data);

        assert.calledWith(BEMHTML.apply, 'TEST_MAIN_CONTENT');
        assert.deepEqual(result, { html: 'TEST_APPLY' });
    });
});

describeBlock('main', function(block) {
    var data;

    stubBlocks([
        'b-page__counter-open',
        'main__content',
        'b-page__spec-css',
        'main__serp-auth',
        'main__center',
        'advanced-search-loader',
        'advanced-search__is-present',
        'b-page__counter-open_ajax',
        'main__ajax',
        'main__distr-popup',
        'main__direct-abuse-modal'
    ]);

    beforeEach(function() {
        data = stubData('cgi', 'experiments', 'i-log', 'counters');
        data.promofooter = {};
        data.promofooter_mobile = {};
        data.device = {
            BrowserName: 'yabro'
        };

        // Стаб для уровня blocks-desktop
        blocks['voice-search'] && sinon.stub(blocks, 'voice-search');
    });

    afterEach(function() {
        // Стаб для уровня blocks-desktop
        blocks['voice-search'] && blocks['voice-search'].restore();
    });

    it('should return ajax response when data.ajax is true', function() {
        data.ajax = true;

        blocks['main__ajax'].returns({ mods: {}, content: ['TEST'] });

        assert.equal(block(data).content[0], 'TEST');
        assert.calledOnce(blocks['b-page__counter-open_ajax']);
    });
});
