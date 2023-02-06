describeBlock('suggest2-url', function(block) {
    let data;

    let glob;
    stubBlocks('suggest2-url__select');

    beforeEach(function() {
        glob = stubGlobal('RequestCtx');
        data = stubData('cgi', 'experiments');
        RequestCtx.GlobalContext.expFlags = stubData('experiments');
    });

    afterEach(() => {
        glob.restore();
    });

    it('should return url', function() {
        const url = 'url-from-logic.com';
        blocks['suggest2-url__select'].returns(url);
        assert.equal(block(data), url);
    });

    it('should return url from suggest-uri flag', function() {
        const flagUrl = 'url-from-flag.com';
        const defaultUrl = 'url-from-logic.com';

        RequestCtx.GlobalContext.expFlags['suggest-uri'] = flagUrl;

        blocks['suggest2-url__select'].returns(defaultUrl);
        assert.equal(block(data), flagUrl);
    });

    it('should return url includes direct params', function() {
        const directParams = 'direct=1&param=2';
        const defaultUrl = 'url-from-logic.com';

        RequestCtx.GlobalContext.expFlags['suggest_direct_params'] = directParams;

        blocks['suggest2-url__select'].returns(defaultUrl);
        assert.equal(block(data), `${defaultUrl}?${directParams}`);
    });

    it('should return url includes front params', function() {
        const frontParams = 'front=1&param=2';
        const defaultUrl = 'url-from-logic.com';

        RequestCtx.GlobalContext.expFlags['suggest_front_params'] = frontParams;

        blocks['suggest2-url__select'].returns(defaultUrl);
        assert.equal(block(data), `${defaultUrl}?${frontParams}`);
    });

    it('should return url from suggest-uri flag includes direct params', function() {
        const directParams = 'direct=1&param=2';
        const flagUrl = 'url-from-flag.com?test=1';
        const defaultUrl = 'url-from-logic.com';

        RequestCtx.GlobalContext.expFlags['suggest-uri'] = flagUrl;
        RequestCtx.GlobalContext.expFlags['suggest_direct_params'] = directParams;

        blocks['suggest2-url__select'].returns(defaultUrl);
        assert.equal(block(data), `${flagUrl}&${directParams}`);
    });

    it('should return url from suggest-uri flag includes front params', function() {
        const frontParams = 'front=1&param=2';
        const flagUrl = 'url-from-flag.com?test=1';
        const defaultUrl = 'url-from-logic.com';

        RequestCtx.GlobalContext.expFlags['suggest-uri'] = flagUrl;
        RequestCtx.GlobalContext.expFlags['suggest_front_params'] = frontParams;

        blocks['suggest2-url__select'].returns(defaultUrl);
        assert.equal(block(data), `${flagUrl}&${frontParams}`);
    });
});

describeBlock('suggest2-url__select', function(block) {
    var data;

    stubBlocks(
        'RequestCtx',
        'suggest2-url__domain'
    );

    beforeEach(function() {
        data = stubData('experiments', 'region');
        RequestCtx.GlobalContext.tld = 'ru';
    });

    it('should return common url by default', function() {
        blocks['suggest2-url__domain'].returns('//yandex.ru');
        assert.equal(block(data), '//yandex.ru/suggest/suggest-ya.cgi?');
    });
});

describeBlock('suggest2-url__domain', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('cgi');
    });

    stubBlocks(
        'RequestCtx'
    );

    it('should return default domain with right tld', function() {
        RequestCtx.GlobalContext.tld = 'by';
        assert.equal(block(data), '//yandex.by');
    });
});

describeBlock('suggest2-url__get-handler-path', function(block) {
    let data;

    stubBlocks('suggest2-url__domain');

    beforeEach(function() {
        blocks['suggest2-url__domain'].returns('//yandex.ru');
    });

    it('should return correct path for suggest', function() {
        assert.equal(block(data), '//yandex.ru/suggest');
    });
});

describeBlock('suggest2-url__params', function(block) {
    var data;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        RequestCtx.GlobalContext.tld = 'ru';

        data = stubData('experiments', 'region');
        data.reqdata.ruid = 'yandexuid';
    });

    it('suggest2-url parameters should be specific for desktop level', function() {
        assert.deepEqual(block(data), {
            srv: 'serp_ru_desktop',
            wiz: 'TrWth',
            yu: 'yandexuid',
            lr: 213,
            uil: 'ru',
            fact: 1,
            history: 1,
            nav_text: 1,
            icon: 1,
            hl: 1,
            v: 4,
            n: 10,
            mob: 0,
            portal: 1,
            platform: 'desktop',

            safeclick: 1,
            skip_clickdaemon_host: 1,

            // Оторвать после раскатки на бэкенде https://st.yandex-team.ru/SERP-69805
            show_experiment: ['222', '224'],

            rich_nav: 1,

            // Оторвать после раскатки на бэкенде https://st.yandex-team.ru/SERP-69805
            use_verified: 1,

            verified_nav: 1,
            rich_phone: 1,
            extend_fw: 1,

            use_favicon: 1,
            nav_favicon: 1,
            maybe_ads: 1,

            mt_wizard: 1
        });
    });
});
