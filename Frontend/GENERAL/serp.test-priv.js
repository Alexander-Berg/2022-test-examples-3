describeBlock('serp', block => {
    let glob;
    let data;
    let res;

    beforeEach(() => {
        glob = stubGlobal('RequestCtx');
        data = stubData('cgi', 'counters');

        RequestCtx.GlobalContext.query = { text: '<script>alert(1)</script>' };

        data.clckHost = 'http://click.ya.ru';
        data.config = { staticVersion: '487' };
        data.navi = { ajax_params: { foo: 'bar' } };

        data.reqdata = {
            reqid: '123',
            action_key: 'u456'
        };

        sinon.stub(blocks, 'b-page__title-found').returns('title found');
    });

    afterEach(() => {
        glob.restore();
        blocks['b-page__title-found'].restore();
    });

    describe('in general case', () => {
        beforeEach(() => {
            res = block(data);
        });

        it('should returns a correct title found', () => {
            assert.nestedPropertyVal(res, 'js.found', 'title found');
        });

        it('should returns a correct reqid', () => {
            assert.nestedPropertyVal(res, 'js.reqid', '123');
        });

        it('should returns a correct counter URL', () => {
            assert.nestedPropertyVal(res, 'js.clck', 'http://click.ya.ru/safeclick/prefix');
        });

        it('should returns a correct static version', () => {
            assert.nestedPropertyVal(res, 'js.staticVersion', '487');
        });

        it('should returns a correct query text', () => {
            assert.nestedPropertyVal(res, 'js.query', '<script>alert(1)</script>');
        });

        it('should returns a correct extra params', () => {
            assert.nestedPropertyVal(res, 'js.extraParams.foo', 'bar');
        });

        it('should returns a correct sk', () => {
            assert.nestedPropertyVal(res, 'js.sk', 'u456');
        });
    });

    describe('with dontUpdateGlobal param', () => {
        beforeEach(() => {
            res = block(data, true);
        });

        it('should returns a correct static version', () => {
            assert.nestedPropertyVal(res, 'js.staticVersion', '487');
        });

        it('should returns a correct title found', () => {
            assert.notNestedProperty(res, 'js.found');
        });

        it('should returns a correct reqid', () => {
            assert.notNestedProperty(res, 'js.reqid');
        });

        it('should returns a correct sk', () => {
            assert.notNestedProperty(res, 'js.sk');
        });

        it('should returns a correct query text', () => {
            assert.notNestedProperty(res, 'js.query');
        });

        it('should returns a correct extra params', () => {
            assert.notNestedProperty(res, 'js.extraParams');
        });
    });
});
