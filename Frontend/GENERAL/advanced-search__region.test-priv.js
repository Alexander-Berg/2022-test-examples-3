describeBlock('advanced-search__region', function() {
    var data, rstr;

    beforeEach(function() {
        data = stubData('region');
        data.restrictionsByName = {};
        data.newRestrictions = [{ type: 'geosuggest', region: { id: 2 } }];

        rstr = {
            type: 'geocat',
            geo: { id: 1, region: { id: 1 }, rstr: '-1' }
        };
    });

    describeBlock('advanced-search__current-region', function(block) {
        it('should return region from active filter', function() {
            var region = block(data, rstr);
            assert.equal(region.id, 1);
        });

        it('should return region from cookie if filter is not active', function() {
            var region = block(data, null);
            assert.equal(region.id, 2);
        });

        it('should return user region if filter is not active and cookie does not set', function() {
            data.newRestrictions = [];
            var region = block(data, null);
            assert.equal(region.id, 213);
        });
    });
});
