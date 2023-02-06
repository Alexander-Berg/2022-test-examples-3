describeBlock('adapter-companies__photo-stream-ajax-data', function(block) {
    let state;

    beforeEach(function() {
        state = {
            oid: '1234567'
        };
    });

    it('should return data with experiments', function() {
        const context = {
            expFlags: {
                'GEO_photo-stream_exp': '{"exp1":"val1","exp2":"val2"}'
            }
        };
        const result = block(context, state);

        assert.equal(result.exp1, 'val1');
        assert.equal(result.exp2, 'val2');
    });

    it('should return data with experiments as object', function() {
        const context = {
            expFlags: {
                'GEO_photo-stream_exp': { exp3: 'val3' }
            }
        };
        const result = block(context, state);

        assert.equal(result.exp3, 'val3');
    });

    it('should return correct data with bad experiments', function() {
        const context = {
            expFlags: {
                'GEO_photo-stream_exp': 1
            }
        };
        const result = block(context, state);

        assert.isDefined(result);
        assert.equal(result.id, '1234567');
    });
});
