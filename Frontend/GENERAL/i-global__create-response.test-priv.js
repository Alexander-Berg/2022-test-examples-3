describe('i-global__create-response', () => {
    let sandbox;
    let data;
    let rrCtx = { setResponseHeader: sinon.stub(), findLastItem: sinon.stub() };

    stubBlocks(
        'profile__add-meta',
        'profile__start',
        'profile__stop'
    );

    beforeEach(() => {
        sandbox = sinon.createSandbox();

        data = stubData('experiments', 'cgi', 'device');
    });

    afterEach(() => {
        sandbox.restore();
    });

    describeBlock('i-global__create-response', block => {
        stubBlocks('i-global__create-response_pre', 'i-global__create-response_normal');

        beforeEach(() => {
            blocks['i-global__create-response_pre'].returns('pre');
            blocks['i-global__create-response_normal'].returns('normal');
        });

        it('should choose pre handler when path is pre', () => {
            data.entry = 'pre-search';
            data.reqdata.path = 'search/touch/pre';
            RequestCtx.GlobalContext.preHandler = true;

            const res = block(data, rrCtx);

            assert.equal(res, 'pre');
        });

        it('should choose pre handler when path is pre with slash', () => {
            data.entry = 'pre-search';
            data.reqdata.path = 'search/touch/pre/';
            RequestCtx.GlobalContext.preHandler = true;

            const res = block(data, rrCtx);

            assert.equal(res, 'pre');
        });

        it('should choose normal handler when path is not pre', () => {
            data.entry = 'pre-search';
            data.reqdata.path = 'search/touch';

            const res = block(data, rrCtx);

            assert.equal(res, 'normal');
        });
    });

    describeBlock('i-global__create-response_normal', block => {
        stubBlocks(
            'i-global__prepare-priv-context',
            'i-global__is-bemjson-dump-request',
            'i-global__create-response_bemjson-dump',
            'i-global__create-response_ajax',
            'i-global__create-response_html'
        );

        it('should return BEMJSON dump when it requested', () => {
            blocks['i-global__is-bemjson-dump-request'].returns(true);
            blocks['i-global__create-response_bemjson-dump'].returns('dump');

            blocks['i-global__prepare-priv-context'].returns(data);
            const res = block(data, rrCtx);

            assert.equal(res, 'dump');
        });

        it('should return AJAX response when it requested', () => {
            blocks['i-global__create-response_ajax'].returns('ajax');

            data.ajax = true;
            blocks['i-global__prepare-priv-context'].returns(data);

            const res = block(data, rrCtx);

            assert.equal(res, 'ajax');
        });

        it('should return HTML response when no extra conditions', () => {
            blocks['i-global__create-response_html'].returns('html');

            blocks['i-global__prepare-priv-context'].returns(data);
            const res = block(data, rrCtx);

            assert.equal(res, 'html');
        });
    });

    describeBlock('i-global__create-response_pre', block => {
        it('should compose i-global__create-response_normal results correctly', () => {
            sandbox.stub(blocks, 'i-global__create-response_normal').callsFake(data => `${data.entry};`);
            data.entry = 'pre-search';

            const res = block(data);

            assert.equal(res, 'pre-search;post-search;');
        });

        it('should return empty string when data.entry == post-search on enter', () => {
            sandbox.stub(blocks, 'i-global__create-response_normal');
            data.entry = 'post-search';

            const res = block(data);

            assert.equal(res, '');
        });
    });

    describeBlock('i-global__handle-response_html', block => {
        const DELIM = '\u0007<"\'';

        stubBlocks('i-global__set-common-headers');

        it('should split response to pre-search and post-search', () => {
            const response = '<div>pre</div>' + DELIM + '<div>post</div>';

            data.isPreSearch = true;
            const preRes = block(data, response, rrCtx);

            data.isPreSearch = false;
            const postRes = block(data, response, rrCtx);

            assert.equal(preRes, '<div>pre</div>');
            assert.equal(postRes, '<div>post</div>');
        });

        it('should call i-global__set-common-headers in pre-search phase only', () => {
            const RESP = 'html';

            block(Object.assign({}, data, { isPreSearch: true }), RESP, rrCtx);
            block(Object.assign({}, data, { isPreSearch: false }), RESP, rrCtx);

            assert.calledOnce(blocks['i-global__set-common-headers']);
            assert.calledWith(
                blocks['i-global__set-common-headers'],
                Object.assign({}, data, { isPreSearch: true })
            );
        });
    });
});
