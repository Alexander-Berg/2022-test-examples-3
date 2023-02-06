describeBlock('def-block', function(defBlock) {
    afterEach(function() {
        delete blocks['defBlockTest'];
    });

    it('should throw an error when an empty name is passed', function() {
        var func = _.partial(defBlock, null);

        assert.throws(func, 'Empty block name');
    });

    it('should throw an error when an empty object is passed', function() {
        var func = _.partial(defBlock, 'name', null);

        assert.throws(func, 'Empty object');
    });

    describe('when a new block is defining', function() {
        it('should be defined in blocks object', function() {
            defBlock('defBlockTest', _.constant('test'));

            assert.isFunction(blocks.defBlockTest);
            assert.equal(blocks.defBlockTest(), 'test');
        });

        it('should pass the empty base function', function() {
            defBlock('defBlockTest', {});

            assert.nestedProperty(blocks, 'defBlockTest.__base');
            assert.isFunction(blocks.defBlockTest.__base);
        });
    });

    describe('when an existing block is wrapped', function() {
        beforeEach(function() {
            blocks['defBlockTest'] = _.constant('base');
        });

        it('should redefine block object', function() {
            defBlock('defBlockTest', _.constant('new'));

            assert.isFunction(blocks.defBlockTest);
            assert.equal(blocks.defBlockTest(), 'new');
        });

        it('should set a base object', function() {
            defBlock('defBlockTest', {});

            assert.equal(blocks.defBlockTest.__base(), 'base');
        });
    });
});
