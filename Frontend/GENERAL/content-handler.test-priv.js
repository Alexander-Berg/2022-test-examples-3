/* global RequestCtx */

describeBlock('content-handler', function(block) {
    let data;

    beforeEach(() => {
        data = stubData();

        stubBlocks('RequestCtx');
    });

    it('should throw an exception if params.defaultHandler was not specified', () => {
        assert.throws(() => block(data, {}), assert.AssertionError);
    });

    it('should throw and exception if params.defaultHandler is not a function', () => {
        assert.throws(() => block(data, { defaultHandler: 'test' }), assert.AssertionError);
    });

    it('should use default handler if content-handler URL params was not specified', () => {
        const defaultHandler = sinon.stub();

        block(data, { defaultHandler });

        assert.isTrue(defaultHandler.called);
    });

    it('should throw an exception if specified content-handler was not found', () => {
        const type = 'this-one-does-not-exist';
        const blockName = 'content-handler_type_' + type;

        delete blocks[blockName];

        RequestCtx.GlobalContext.contentHandlerName = type;

        assert.throws(() => block(data, { defaultHandler: sinon.stub() }), assert.AssertionError);
    });
});

describeBlock('content-handler__title', function(block) {
    let data;

    beforeEach(() => {
        data = stubData();

        stubBlocks('RequestCtx');
    });

    it('should return undefined if specified content-handler does not exist', () => {
        const type = 'this-one-does-not-exist';

        RequestCtx.GlobalContext.contentHandlerName = type;
        assert.isUndefined(block(data), assert.AssertionError);
    });

    it('should return string if handler exists', () => {
        const type = 'this-one-exists';

        blocks['content-handler_type_this-one-exists-title'] = () => { return 'title' };

        RequestCtx.GlobalContext.contentHandlerName = type;
        assert.equal(block(data), 'title', assert.AssertionError);
    });
});
