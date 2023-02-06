describeBlock('adapter-adresa__url', function(block) {
    var data,
        snp,
        service;

    stubBlocks('adapter-adresa__single-url-params', 'adapter-adresa__multiple-url-params');

    beforeEach(function() {
        data = { log: { node: () => {} } };
        snp = { item: {} };
        service = sinon.stub(RequestCtx.Service, 'service').returns({
            root: '//maps.yandex.ru'
        });
    });

    afterEach(function() {
        service.restore();
    });

    it('for type "single" should call adapter-adresa__single-url-params once', function() {
        block(data, snp, 'single');

        assert.ok(blocks['adapter-adresa__single-url-params'].calledOnce);
    });

    it('for type "single" should not call adapter-adresa__multiple-url-params', function() {
        block(data, snp, 'single');

        assert.notOk(blocks['adapter-adresa__multiple-url-params'].called);
    });

    it('for type "multiple" should call adapter-adresa__multiple-url-params once', function() {
        block(data, snp, 'multiple');

        assert.ok(blocks['adapter-adresa__multiple-url-params'].calledOnce);
    });

    it('for type "multiple" should not call adapter-adresa__single-url-params', function() {
        block(data, snp, 'multiple');

        assert.notOk(blocks['adapter-adresa__single-url-params'].called);
    });
});

describeBlock('adapter-adresa__single-url-params', function(block) {
    var snp = { item: { company_id: 'some_id' } };

    it('should add oid if company_id is present', function() {
        var result = block(snp);

        assert.strictEqual(result.oid, snp.item.company_id);
    });

    it('should return undefined if company_id is not present', function() {
        var result = block({ item: {} });

        assert.isUndefined(result);
    });
});

describeBlock('adapter-adresa__multiple-url-params', function(block) {
    it('should return parameters if "item.url" is present', function() {
        var result = block({ item: { url: 'yandex.com' } });

        assert.ok(Object.keys(result).length);
    });

    it('should return undefined if "item.url" is not present', function() {
        var result = block({ item: {} });

        assert.isUndefined(result);
    });

    it('should return parameters if "item.url" is punycode', function() {
        var result = block({
            item: { url: 'xn--d1acpjx3f.com' }
        });

        assert.equal(result.text, 'яндекс.com');
    });

    it('should return parameters if "item.url" is unicode', function() {
        var result = block({
            item: { url: 'yandex.com' }
        });

        assert.equal(result.text, 'yandex.com');
    });
});

describeBlock('adapter-adresa__rating-url', function(block) {
    let context;
    let item;
    let service;

    beforeEach(function() {
        context = {
            pageUrl: RequestCtx.url('https://yandex.ru/search/'),
            tld: 'ru'
        };
        item = {
            is_online: false,
            seoname: 'pushkin',
            company_id: '123456'
        };
        service = sinon.stub(RequestCtx.Service, 'service').returns({
            root: 'https://yandex.ru/maps'
        });
    });

    afterEach(function() {
        service.restore();
    });

    it('should return url to Yandex.Maps', function() {
        const result = block(context, item);

        assert.equal(result[0], 'https://yandex.ru/maps/org/pushkin/123456/?reviews');
        assert.equal(result[1], 'maps');
    });

    it('should return url to Profile page', function() {
        item.is_online = true;

        const result = block(context, item);

        assert.equal(result[0], 'https://yandex.ru/profile/123456?intent=reviews');
        assert.equal(result[1], 'profile');
    });
});
