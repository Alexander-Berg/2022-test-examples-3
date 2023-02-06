describeBlock('advanced-search__within', function() {
    var data;

    beforeEach(function() {
        data = stubData('experiments', 'i18n');
    });

    describeBlock('advanced-search__within-menu', function(block) {
        it('should return 3 items, with 77, 1, 2 values', function() {
            var result = block(data);

            assert.equal(result.length, 3);
            assert.propertyVal(result[0], 'value', '77');
            assert.propertyVal(result[1], 'value', '1');
            assert.propertyVal(result[2], 'value', '2');
        });
    });

    describeBlock('advanced-search__within-is-radio', function(block) {
        it('should return true if radio list has value', function() {
            var radio = [
                { value: '77', text: '1' },
                { value: '1', text: '3' },
                { value: '2', text: '4' }
            ];
            assert.isTrue(block(radio, '77'));
        });

        it('should return false if radio list has not value', function() {
            var radio = [
                { value: '77', text: '1' },
                { value: '1', text: '3' },
                { value: '2', text: '4' }
            ];
            assert.isFalse(block(radio, '100'));
        });
    });
});
