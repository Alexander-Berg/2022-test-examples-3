/* globals RequestCtx */
describeBlock('adapter', block => {
    let context;

    stubBlocks([
        'adapter__get-type',
        'adapter__ts',
        'adapter__priv'
    ]);

    beforeEach(() => {
        context = { reportData: {} };
    });

    it('should do nothing for empty snippet', () => {
        block(context);

        assert.notCalled(blocks['adapter__ts']);
        assert.notCalled(blocks['adapter__priv']);
    });

    it('should do nothing for empty type', () => {
        block(context, {});

        assert.notCalled(blocks['adapter__ts']);
        assert.notCalled(blocks['adapter__priv']);
    });

    it('should do nothing if type is not a string', () => {
        blocks['adapter__get-type'].returns(['foo', 'bar']);

        block(context, {});

        assert.notCalled(blocks['adapter__ts']);
        assert.notCalled(blocks['adapter__priv']);
    });

    it('should call ts-runtime first', () => {
        blocks['adapter__get-type'].returns('foo');
        blocks['adapter__ts'].returns('dataset');

        block(context, {});

        assert.called(blocks['adapter__ts']);
        assert.notCalled(blocks['adapter__priv']);
    });

    it('should call priv-runtime second', () => {
        blocks['adapter__get-type'].returns('foo');

        block(context, {});

        assert.called(blocks['adapter__ts']);
        assert.called(blocks['adapter__priv']);
    });
});

describeBlock('adapter__ts', block => {
    let context;
    let snippet;
    let doc;
    let glob;

    stubBlocks([
        'adapter__get-webmaster-siteinfo',
        'adapter__set-feedback-form'
    ]);

    beforeEach(() => {
        context = { expFlags: {}, reportData: stubData('i-log') };
        snippet = {};
        doc = {};
        glob = stubGlobal('RequestCtx');
    });

    afterEach(() => {
        glob.restore();
    });

    it('should call runtime.select', () => {
        block(context, 'foo', snippet, doc);

        const options = RequestCtx.Taburet.adapters.select.lastCall.args[0];

        assert.equal(options.context, context);
        assert.equal(options.snippet, snippet);
        assert.equal(options.document, doc);
    });

    describe('progressive css', () => {
        beforeEach(() => {
            RequestCtx.Taburet.adapters.select.returns({
                meta: { type: 'foo', subtype: 'bar', rendered: true },
                data: { baz: true, type: 'is-it-buggy?' },
                __dangerousPrivCompatibility: true
            });
        });

        it('should push base and experimental', () => {
            block(context, 'foo', snippet, doc);

            const assets = RequestCtx.Taburet.adapters.getAssets.lastCall.returnValue;

            assert.called(assets.provideBaseCSS);
            assert.called(assets.provideExperimentalCSS);
            assert.called(assets.clearCSS);
        });
    });

    it('should process taburet result', () => {
        RequestCtx.Taburet.adapters.select.returns({
            meta: { type: 'foo', subtype: 'bar' },
            data: { baz: true, type: 'is-it-buggy?' },
            __dangerousPrivCompatibility: true
        });

        assert.deepEqual(
            block(context, snippet, doc),
            {
                type: 'foo',
                subtype: 'bar',
                baz: true
            }
        );
    });

    it('should call method which add host and webmaster info', function() {
        RequestCtx.Taburet.adapters.select.returns({
            meta: {},
            data: {},
            __dangerousPrivCompatibility: true
        });
        blocks['adapter__set-host-and-webmaster-info'] = sinon.stub();
        block(context, 'foo', snippet, doc);

        assert.calledOnce(blocks['adapter__set-host-and-webmaster-info']);
    });
});

describeBlock('adapter__priv', block => {
    let context;
    let snippet;
    let doc;

    stubBlocks([
        'adapter__set-counters',
        'adapter__get-subtype-handler',
        'adapter__get-webmaster-siteinfo'
    ]);

    beforeEach(() => {
        context = { expFlags: {}, reportData: {} };
        snippet = {};
        doc = {};

        blocks['adapter-foo'] = sinon.stub();
        blocks['adapter-foo_type_bar'] = sinon.stub();
        blocks['adapter-foo__ajax'] = sinon.stub();
    });

    it('should call adapter type', () => {
        blocks['adapter__get-subtype-handler'].returns(blocks['adapter-foo_type_bar']);

        block(context, 'foo', snippet, doc);

        assert.notCalled(blocks['adapter-foo']);
        assert.calledWith(blocks['adapter-foo_type_bar'], context, snippet, doc);
    });

    it('should call adapter itself', () => {
        block(context, 'foo', snippet, doc);

        assert.calledWith(blocks['adapter-foo'], context, snippet, doc);
        assert.notCalled(blocks['adapter-foo_type_bar']);
    });

    describe('ajax', () => {
        it('should retrieve ajax-elem', () => {
            block(context, 'foo', snippet, doc, { isAjax: true });

            assert.calledWith(blocks['adapter__get-subtype-handler'], context, snippet, 'ajax');
        });

        it('should call adapter ajax-elem with other arguments', () => {
            blocks['adapter__get-subtype-handler'].returns(blocks['adapter-foo__ajax']);

            block(context, 'foo', snippet, doc, true);

            assert.notCalled(blocks['adapter-foo']);
            assert.calledWith(blocks['adapter-foo__ajax'], context, context.reportData, snippet);
        });
    });

    it('should call method which add host and webmaster info', function() {
        blocks['adapter-foo'].returns({});
        blocks['adapter__set-host-and-webmaster-info'] = sinon.stub();
        block(context, 'foo', snippet, doc);

        assert.calledOnce(blocks['adapter__set-host-and-webmaster-info']);
    });
});

describeBlock('adapter__get-type', function(block) {
    let context;
    let constructData;

    beforeEach(function() {
        context = {};
        constructData = {
            type: 'type-from-type-field'
        };
    });

    describe('precedence of props to search for adapter type', function() {
        it('should return type from "adapter" prop', function() {
            constructData.adapter = 'type-from-adapter-field';
            assert.equal(block(context, constructData), 'type-from-adapter-field');
        });

        it('should return type from "type" prop', function() {
            assert.equal(block(context, constructData), 'type-from-type-field');
        });

        it('should return "undefined" if no appropriate props privided', function() {
            assert.equal(block(context, undefined), undefined);
            assert.equal(block(context, {}), undefined);
        });
    });

    it('should replace all chars that are not letters with dashes', function() {
        constructData.adapter = 'qwe_q)(*Ówe_QWeыва';
        assert.equal(block(context, constructData), 'qwe-q-we-QWe-');
    });

    it('should return undefined if no type is detected', function() {
        assert.isUndefined(block(context, {}));
    });
});

describeBlock('adapter__set-host-and-webmaster-info', function(block) {
    let adapterData;
    let glob;

    beforeEach(function() {
        adapterData = {};
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext = { tld: 'ru' };
    });

    afterEach(function() {
        glob.restore();
    });

    stubBlocks('adapter__host');

    it('should not add host and webmaster info if host is not exists', function() {
        block(null, adapterData);

        assert.isUndefined(adapterData.host);
        assert.isUndefined(adapterData.webmasterSiteinfo);
    });

    it('should add host and webmaster info if host is exists', function() {
        const host = 'https://example.ru';

        blocks['adapter__host'].returns(host);

        block(null, adapterData);

        assert.strictEqual(adapterData.host, host);
        assert.strictEqual(adapterData.webmasterSiteinfo, `https://webmaster.yandex.ru/siteinfo/?site=${host}`);
    });
});

describeBlock('adapter__get-webmaster-siteinfo', function(block) {
    stubBlocks('adapter__host');
    let glob;

    beforeEach(() => {
        glob = stubGlobal('RequestCtx');
    });

    afterEach(() => {
        glob.restore();
    });

    it('should not return webmaster info for com.tr and com tlds', function() {
        ['com', 'com.tr'].forEach(tld => {
            const host = `https://example.${tld}`;
            RequestCtx.GlobalContext = { tld };

            const webmasterSiteinfo = block(null, host);

            assert.isUndefined(webmasterSiteinfo);
        });
    });

    it('should return webmaster info for куубр tlds', function() {
        ['ru', 'by', 'kz', 'ua', 'uz'].forEach(tld => {
            const host = `https://example.${tld}`;
            RequestCtx.GlobalContext = { tld };

            const webmasterSiteinfo = block(null, host);

            assert.strictEqual(webmasterSiteinfo, `https://webmaster.yandex.${tld}/siteinfo/?site=${host}`);
        });
    });
});

describeBlock('adapter__compose-subtype-string', function(block) {
    let context;
    let rawData;

    let TYPE = 'subtype-test';
    let SUBTYPE = 'sub';
    let ELEM = 'el';

    let ADAPTER = 'adapter-subtype-test';
    let ADAPTER_WITH_SUBTYPE = 'adapter-subtype-test_type_sub';
    let ADAPTER_ELEM_WITH_SUBTYPE = 'adapter-subtype-test__el_type_sub';

    beforeEach(function() {
        context = { expFlags: {} };
        rawData = { type: TYPE, subtype: SUBTYPE };
    });

    it('should return base adapter if subtype not available', function() {
        delete rawData.subtype;
        assert.equal(block(context, rawData), ADAPTER);
    });

    it('should return adapter with subtype if subtype available', function() {
        assert.equal(block(context, rawData), ADAPTER_WITH_SUBTYPE);
    });

    it('should return adapter elem with subtype if subtype available and elem name is provided', function() {
        assert.equal(block(context, rawData, ELEM), ADAPTER_ELEM_WITH_SUBTYPE);
    });
});
