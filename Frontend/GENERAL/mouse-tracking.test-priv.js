describeBlock('mouse-tracking', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('experiments');
        RequestCtx.GlobalContext.cgi.host = () => {};
        RequestCtx.GlobalContext.expFlags = stubData('experiments');
    });

    describe('with flag = object', function() {
        it('should throw if object could not be parsed', function() {
            RequestCtx.GlobalContext.expFlags['enable_mousetrack'] = '{ a: 123 }';
            assert.throws(_.partial(block, data), SyntaxError, 'Unexpected token a');
        });

        it('should include passed options object', function() {
            RequestCtx.GlobalContext.expFlags['enable_mousetrack'] = '{ "superTestString": 123 }';
            var content = block(data)[0].content;
            assert.include(content, 'superTestString');
        });
    });
});
