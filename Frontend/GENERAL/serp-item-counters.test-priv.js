describeBlock('serp-item-counters', function(block) {
    var data, item, path, params;

    stubBlocks([
        'serp-item-counters__heights'
    ]);

    beforeEach(function() {
        data = stubData(['counters', 'experiments']);
        item = {};
        path = '/foo/bar';

        blocks['serp-item-counters__heights'].returns({ '-pos': 'p1' });
    });

    every([undefined, 1, '1', true, 0], 'should not call counter if item has wrong type', function(item) {
        block(data, item, path, params);

        assert.notCalled(data.counter);
    });

    it('should call counter with all params', function() {
        block(data, item, path, params);

        assert.calledWith(data.counter, '/foo/bar', { '-pos': 'p1' });
    });
});

describeBlock('serp-item-counters__heights', function(block) {
    var data, item, params, result;

    beforeEach(function() {
        data = {};
    });

    it('should return "pos" param if it exists', function() {
        params = { pos: 1 };

        result = block(data, item, params);

        assert.propertyVal(result, '-pos', 'p1');
    });

    it('should not duplicate "p" in pos param', function() {
        params = { pos: 'p9' };

        result = block(data, item, params);

        assert.propertyVal(result, '-pos', 'p9');
    });

    it('should not cut "p" in pos param', function() {
        params = { pos: 'important' };

        result = block(data, item, params);

        assert.propertyVal(result, '-pos', 'pimportant');
    });

    it('should return object with id=0 first time', function() {
        params = { pos: 1 };

        result = block(data, item, params);

        assert.propertyVal(result, '-id', '0');
    });

    every([0, 1, 35], 'should return counter with correct id', function(counterBlockId) {
        params = { pos: 1 };
        data._counterBlockId = counterBlockId;

        result = block(data, item, params);

        assert.propertyVal(result, '-id', String(counterBlockId + 1));
    });

    it('should add to item "data-cid" attribute if item is object', function() {
        data._counterBlockId = 41;
        item = { block: 'serp-item' };

        block(data, item, params);

        assert.nestedPropertyVal(item, 'attrs.data-cid', 42);
    });

    it('should add to item "data-cid" attribute if item is array', function() {
        data._counterBlockId = 41;
        item = [
            { block: 'serp-item' },
            { block: 'serp-item', attrs: {} },
            [{ block: 'serp-item' }]
        ];

        block(data, item, params);

        assert.nestedPropertyVal(item, '0.attrs.data-cid', 42);
        assert.nestedPropertyVal(item, '1.attrs.data-cid', 42);
        assert.nestedPropertyVal(item, '2.0.attrs.data-cid', 42);
    });

    it('should return "bid" param if it exists', function() {
        params = { bid: '123123123' };

        result = block(data, item, params);

        assert.propertyVal(result, '-bid', '123123123');
    });
});
