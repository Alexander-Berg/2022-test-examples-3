describeBlock('advanced-search', function() {
    var data;

    beforeEach(function() {
        data = stubData('cgi');
        data.restrictions = [];
        data.restrictionsByName = {};
    });

    describeBlock('advanced-search__is-opened', function(block) {
        stubBlocks(
            'RequestCtx',
            'advanced-search__is-present'
        );

        beforeEach(function() {
            blocks['advanced-search__is-present'].returns(true);
        });

        it('should return true if filter by lang is active', function() {
            data.restrictions = [{
                type: 'lang'
            }];

            assert.isTrue(block(data));
        });

        it('should return true if filter by date is active', function() {
            data.restrictions = [{
                type: 'date'
            }];

            assert.isTrue(block(data));
        });

        it('should return true if filter by region is active', function() {
            data.restrictions = [{
                type: 'geocat'
            }];

            assert.isTrue(block(data));
        });

        it('should return true if it is `blogs search`', function() {
            RequestCtx.GlobalContext.report = 'blogs2';
            assert.isTrue(block(data));
        });
    });
});

describeBlock('advanced-search__is-present', function(block) {
    var data,
        result;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        data = {};
    });

    it('should return true by default', function() {
        result = block(data);

        assert.isTrue(result);
    });

    it('should return false in Pumpkin mode', function() {
        RequestCtx.GlobalContext.isPumpkin = true;

        result = block(data);

        assert.isFalse(result);
    });

    it('should return false in Mobile Apps mode', function() {
        RequestCtx.GlobalContext.report = 'mobile_apps';

        result = block(data);

        assert.isFalse(result);
    });
});
