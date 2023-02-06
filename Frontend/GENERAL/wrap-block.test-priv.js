/* globals RequestCtx */
describeBlock('wrap-blocks-try-catch', function(wrapBlocksTryCatch) {
    var block,
        blocks,
        data,
        skipBlockWrap,
        blockNotFunction;

    beforeEach(function() {
        // Создаём новые объекты для каждого теста
        block = _.constant('block');
        skipBlockWrap = _.constant('skipped block');
        skipBlockWrap._skipWrap = true;
        blockNotFunction = {};
        blocks = {
            block: block,
            'block-alias': block,
            'block-not-function': blockNotFunction,
            skipBlockWrap: skipBlockWrap
        };
        data = stubData('experiments');

        wrapBlocksTryCatch(blocks, data);
    });

    describe('wraps', function() {
        it('functions with try catch', function() {
            assert.notStrictEqual(blocks['block'], block);
        });

        it('should wrap only functions', function() {
            assert.strictEqual(blocks['block-not-function'], blockNotFunction);
        });
    });

    describe('does not wrap', function() {
        it('blocks twice', function() {
            var wrappedAlready = blocks['block'];

            wrapBlocksTryCatch(blocks, data);

            assert.strictEqual(blocks['block'], wrappedAlready);
        });

        it('function with "_skipWrap" property', function() {
            assert.strictEqual(blocks['skipBlockWrap'], skipBlockWrap);
        });
    });
});

describeBlock('wrap-block-try-catch', function(wrapBlockTryCatch) {
    var wrapppedFunc,
        block = _.constant('block'),
        errorMessage = 'Some unexpected function call error',
        blockWithException = function() {
            throw new Error(errorMessage);
        };

    stubBlocks('RequestCtx');

    describe('wrapped functions', function() {
        describe('normal flow', function() {
            var blockName = 'blockName';

            beforeEach(function() {
                wrapppedFunc = wrapBlockTryCatch(block, blockName);
            });

            it('should return function call result', function() {
                assert.equal(wrapppedFunc(), 'block');
            });

            it('should not wrap already wrapped function', function() {
                assert.strictEqual(wrapBlockTryCatch(wrapppedFunc), wrapppedFunc);
            });
        });

        describe('throws error', function() {
            beforeEach(function() {
                wrapppedFunc = wrapBlockTryCatch(blockWithException, '');
            });

            it('should swallow throwen error', function() {
                assert.doesNotThrow(wrapppedFunc, Error);
            });

            it('should return empty string', function() {
                RequestCtx.Logger.reportError.returns('');
                assert.equal(wrapppedFunc(), '');
            });
        });
    });

    describe('From Yandex network', function() {
        beforeEach(function() {
            blocks['wrap-blocks-try-catch__yandex-net'] = true;
            wrapppedFunc = wrapBlockTryCatch(blockWithException, '');
            RequestCtx.Logger.reportError.returns(`<div>${errorMessage}</div>`);
        });

        it('should show error message', function() {
            assert.include(wrapppedFunc(), errorMessage);
            // FIXME: включить проверку вызова browser-error в SERP-62382
            // assert.calledOnce(console['browser-error'])
        });
    });
});
