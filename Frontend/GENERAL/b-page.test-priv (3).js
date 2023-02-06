describeBlock('b-page__skin-mode', function(block) {
    let context;
    let glob;

    beforeEach(function() {
        context = { expFlags: {}, reportData: {} };
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext.expFlags = stubData('experiments');
    });

    afterEach(function() {
        glob.restore();
    });

    it('Форсированное задание темы', function() {
        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'dark';
        assert.equal(block(context), 'dark');

        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'light';
        assert.equal(block(context), 'light');

        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'system';
        assert.equal(block(context), 'system');
    });

    it('Задание темы из куки', function() {
        context.reqdata = {
            ycookie: {
                yp: {
                    skin: 's'
                }
            }
        };

        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'cookie';
        assert.equal(block(context), 'system');

        context.reqdata = {
            ycookie: {
                yp: {
                    skin: 'd'
                }
            }
        };

        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'cookie';
        assert.equal(block(context), 'dark');

        context.reqdata = {
            ycookie: {
                yp: {
                    skin: 'l'
                }
            }
        };

        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'cookie';
        assert.equal(block(context), 'light');
    });

    it('Пустая кука фоллбечится на значение "как в системе"', function() {
        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'cookie';
        assert.equal(block(context), 'system');
    });

    it('Невалидное значение куки фоллбечится на значение "как в системе"', function() {
        context.reqdata = {
            ycookie: {
                yp: {
                    skin: 'blah'
                }
            }
        };

        RequestCtx.GlobalContext.expFlags['dark_theme_desktop'] = 'cookie';
        assert.equal(block(context), 'system');
    });
});
