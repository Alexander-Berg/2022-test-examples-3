describeBlock('b-page', function(block) {
    var data;

    stubBlocks(
        'RequestCtx',
        'b-page__title',
        'b-page__head',
        'b-page__content-postsearch',
        'b-page__content-presearch',
        'b-page__mix',
        'serp-metrika'
    );

    beforeEach(function() {
        data = stubData('experiments', 'counters', 'device', 'i-log', 'cgi');

        RequestCtx.GlobalContext.zaloginPopupRegistry = { getById: () => ({}) };
        RequestCtx.GlobalContext.isLoggedIn = false;
        RequestCtx.Taburet = {
            adapters: {
                getAssets: () => ({
                    provideCssOnlyExperiments: () => '',
                    provideMainChunk: () => {},
                    provideReactWithDomAndPolyfills: () => {},
                    provideES6Polyfill: () => {},
                    providePresearchCSS: () => '',
                    setPresearchCSSExp: () => ''
                }),
                getGlobalCssExperiments: () => ''
            }
        };
        RequestCtx.GlobalContext.logViewRegistry = {
            getRegistry: () => {}
        };

        data.tld = '';
        data.config = {
            staticHost: '',
            staticVersion: 'mimimi'
        };
    });

    it('should return an object', function() {
        expect(block(data)).to.be.an('object');
    });

    it('should return undefined in share-mode in pre-search', function() {
        data.isPreSearch = true;
        RequestCtx.GlobalContext.isShareMode = true;

        expect(block(data)).to.equal(undefined);
    });

    it('should not call content in pre-search', function() {
        data.isPreSearch = true;

        block(data);

        expect(blocks['b-page__content-postsearch'].called).to.equal(false);
    });

    it('should call content in post-search', function() {
        data.isPreSearch = false;

        block(data);

        expect(blocks['b-page__content-postsearch'].called).to.equal(true);
    });
});

describeBlock('b-page__content-postsearch', function(block) {
    var data;

    stubBlocks([
        'b-page__content-postsearch-pre-delimiter',
        'b-page__content-postsearch-post-delimiter',
        'b-page__presearch-delimiter'
    ]);

    beforeEach(function() {
        data = stubData('experiments', 'counters');
        data.config = {
            staticVersion: 'mimimi'
        };
    });

    it('should call "data.counter" with corresponding args', function() {
        block(data);

        assert.calledWith(data.counter, '/search_props', 'staticVersion', 'mimimi');
    });

    it('should call pre- and post-delimiter methods in certain order', () => {
        block(data);

        assert.callOrder(
            blocks['b-page__content-postsearch-pre-delimiter'],
            blocks['b-page__presearch-delimiter'],
            blocks['b-page__content-postsearch-post-delimiter']
        );
    });
});

describeBlock('b-page__content-presearch', block => {
    stubBlocks([
        'b-page__content-presearch-pre-delimiter',
        'b-page__presearch-delimiter'
    ]);

    it('should call pre- and post-delimiter methods in certain order', () => {
        block();

        assert.callOrder(
            blocks['b-page__content-presearch-pre-delimiter'],
            blocks['b-page__presearch-delimiter']
        );
    });
});

describeBlock('b-page__content-postsearch-pre-delimiter', block => {
    stubBlocks('RequestCtx', 'b-page__content-presearch');

    it('should call b-page__content-presearch in share mode', () => {
        RequestCtx.GlobalContext.isShareMode = true;

        block({});
        assert.calledOnce(blocks['b-page__content-presearch']);
    });
});

describeBlock('b-page', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('experiments', 'counters', 'cgi', 'device');
        data.tld = 'ru';
        data.config = { staticHost: '' };
        data.log = { node: sinon.stub() };
        data.globalParams = {};

        RequestCtx.Taburet = {
            adapters: {
                getAssets: () => ({
                    provideCssOnlyExperiments: () => '',
                    provideMainChunk: () => {},
                    provideReactWithDomAndPolyfills: () => {},
                    provideES6Polyfill: () => {},
                    providePresearchCSS: () => '',
                    setPresearchCSSExp: () => ''
                }),
                getGlobalCssExperiments: () => ''
            }
        };
    });

    stubBlocks('b-page__title', 'b-page__head', 'b-page__content', 'serp-metrika');

    describe('font decoder', function() {
        it('should be enabled by default', function() {
            assert.isTrue(block(data).js.isFontDecoderEnabled);
        });
    });
});

// Из-за ошибки на тачпадах (отсутствует хендлер 'i-counters__redef'), пока положил здесь.
// TODO: перенести на уровень common
describeBlock('b-page__head', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('experiments', 'cgi', 'device');

        data.isPumpkin = false;
        data.isPreSearch = false;
        data.config = {};
    });

    stubBlocks([
        'RequestCtx',
        'i-counters__rc',
        'i-counters__w',
        'i-counters__sh',
        'b-page__title',
        'b-page__social',
        'b-page__favicon',
        'b-page__head-css-pumpkin',
        'b-page__head-css-production',
        'rum__init'
    ]);

    describe('w/o options', function() {
        it('should not call "b-page__head-css-pumpkin"', function() {
            block(data);

            expect(blocks['b-page__head-css-pumpkin'].called).to.equal(false);
        });
    });

    describe('in pumpkin mode', function() {
        it('should call "b-page__head-css-pumpkin"', function() {
            RequestCtx.GlobalContext.isPumpkin = true;
            block(data);

            expect(blocks['b-page__head-css-pumpkin'].called).to.equal(true);
        });
    });
});
