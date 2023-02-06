describeBlock('b-page', function(block) {
    var data;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        data = stubData('counters');
    });

    it('should return undefined in share-mode in pre-search', function() {
        data.isPreSearch = true;
        RequestCtx.GlobalContext.isShareMode = true;

        expect(block(data)).to.equal(undefined);
    });
});

describeBlock('b-page__content-postsearch', function(block) {
    var data;
    let glob;

    stubBlocks(
        'RequestCtx',
        'b-page__spec-css',
        'b-page__header',
        'b-page__header-update-js',
        'b-page__burger-drawer',
        'adapter__ts',
        'serp__spin',
        'b-page__foot-js',
        'suggest2',
        'overlay',
        'construct__context',
        'mouse-tracking',
        'distr-promo-head',
        'main',
        'mini-suggest__inject-params',
        'serp-footer',
        'verified-tooltip',
        'user-id',
        'header3-actions',
        'self-promo-shortcut',
        'butterfly',
        'lazy-load'
    );

    beforeEach(function() {
        data = stubData('cgi', 'counters', 'experiments', 'region');
        data.globalParams = {};
        data.bbSid = {};
        data.config = {
            staticVersion: '0x000000'
        };
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext.blackbox = {
            activeAccount: {
                uid: '960657744',
                login: 'lshkoloberda',
                name: 'Лжевася',
                surname: 'Школоберда',
                avatar: '0/0-0',
                email: 'lshkoloberda@yandex.ru'
            },
            otherUserAccountsInfo: [],
            areOtherAccountsAvailable: false
        };
        RequestCtx.Taburet = {
            adapters: {
                getAssets: () => ({
                    provideCssOnlyExperiments: () => '',
                    provideMainChunk: () => {},
                    provideReactWithDomAndPolyfills: () => {},
                    provideES6Polyfill: () => {},
                    providePresearchCSS: () => '',
                    setPresearchCSSExp: () => {}
                }),
                getGlobalCssExperiments: () => ''
            }
        };
        RequestCtx.GlobalContext.logViewRegistry = {
            getRegistry: () => {}
        };
        RequestCtx.GlobalContext.zaloginPopupRegistry = {
            getById: () => ({})
        };
        data.reportData = { reqdata: {}, prefs: {} };
    });

    afterEach(() => {
        glob.restore();
    });

    it('should call "data.counter" with corresponded arg', function() {
        block(data);

        assert.ok(data.counter.calledWith('/search_props', 'staticVersion', '0x000000'));
    });
});

describeBlock('b-page__mods', function(block) {
    var data = stubData('experiments', 'i-log');

    stubBlocks(
        'b-page__content'
    );

    beforeEach(function() {
        data = stubData('counters');
        data.tld = 'ru';
    });

    it('should have appsearch mod for search application', function() {
        data.isAppSearchHeader = true;

        assert.nestedPropertyVal(block(data), 'appsearch', true);
    });
});

describeBlock('b-page__content', function(block) {
    var data;

    stubBlocks([
        'b-page__spec-css',
        'b-page__header',
        'b-page__header-update-js',
        'serp__spin',
        'b-page__foot-js',
        'suggest2',
        'overlay',
        'construct__context',
        'mouse-tracking',
        'distr-promo-head',
        'main',
        'serp-footer',
        'RequestCtx'
    ]);

    beforeEach(function() {
        data = stubData('cgi', 'counters', 'experiments', 'region');
        data.globalParams = {};
        data.bbSid = {};
        data.config = {
            staticVersion: '0x000000'
        };
        data.reportData = { reqdata: {}, prefs: {} };
        data.counterAttrs = function() { return {} };
        data.counter = function() { return {} };
        data.reqdata = { passport: {}, flags: {}, device_detect: { BrowserName: 'yabro' } };
    });
});

describeBlock('b-page__layout-postsearch-start', block => {
    stubBlocks([
        'b-page__presearch-delimiter',
        'b-page__layout-postsearch-start_post-delimiter'
    ]);

    it('should call pre- and post-delimiter methods in certain order', () => {
        block();

        assert.callOrder(
            blocks['b-page__presearch-delimiter'],
            blocks['b-page__layout-postsearch-start_post-delimiter']
        );
    });
});

describeBlock('b-page__layout-presearch-finish', block => {
    stubBlocks([
        'b-page__presearch-delimiter',
        'b-page__layout-presearch-finish_pre-delimiter'
    ]);

    it('should call pre- and post-delimiter methods in certain order', () => {
        block();

        assert.callOrder(
            blocks['b-page__layout-presearch-finish_pre-delimiter'],
            blocks['b-page__presearch-delimiter']
        );
    });
});

describeBlock('b-page__skin-mode', function(block) {
    let context;
    let glob;

    beforeEach(function() {
        context = { expFlags: {}, reportData: { reqdata: {} } };
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext.expFlags = stubData('experiments');
    });

    afterEach(function() {
        glob.restore();
    });

    it('Задание темы из куки', function() {
        context.reportData.reqdata = {
            ycookie: {
                yp: {
                    skin: 's'
                }
            }
        };

        assert.equal(block(context.reportData), 'system');

        context.reportData.reqdata = {
            ycookie: {
                yp: {
                    skin: 'd'
                }
            }
        };

        assert.equal(block(context.reportData), 'dark');

        context.reportData.reqdata = {
            ycookie: {
                yp: {
                    skin: 'l'
                }
            }
        };

        assert.equal(block(context.reportData), 'light');
    });

    it('Пустая кука фоллбечится на значение "как в системе"', function() {
        assert.equal(block(context.reportData), 'system');
    });

    it('Невалидное значение куки фоллбечится на значение "как в системе"', function() {
        context.reportData.reqdata = {
            ycookie: {
                yp: {
                    skin: 'blah'
                }
            }
        };

        assert.equal(block(context.reportData), 'system');
    });

    it('Проверка режима prod', function() {
        RequestCtx.GlobalContext.isSearchApp = true;

        context.reportData.reqdata = {
            ycookie: {
                yp: {
                    skin: 'd'
                }
            }
        };

        RequestCtx.GlobalContext.platform = 'ios';

        assert.equal(block(context.reportData), 'system'); // для iOS всегда системная тема

        RequestCtx.GlobalContext.platform = 'android';

        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '22.12' };

        assert.equal(block(context.reportData), 'light'); // для ПП Android ниже 22.1.3 всегда светлая тема

        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '22.13' };

        assert.equal(block(context.reportData), 'system'); // для ПП Android 22.1.3+ всегда системная тема
    });
});
