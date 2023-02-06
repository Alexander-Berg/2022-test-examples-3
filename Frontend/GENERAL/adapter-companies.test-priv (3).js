describeBlock('adapter-companies__type', function(block) {
    let context;
    let snippet;

    beforeEach(function() {
        context = {
            expFlags: {
            }
        };
        snippet = {
            serp_info: { subtype: '' }
        };
    });

    it('should return `list` type if snippet subtype equals `map`', function() {
        snippet.serp_info.subtype = 'map';

        assert.equal(block(context, snippet), 'list');
    });

    it('should return `1org` type if snippet subtype equals `company`', function() {
        snippet.serp_info.subtype = 'company';

        assert.equal(block(context, snippet), '1org');
    });
});

describeBlock('adapter-companies__sitelinks', function(block) {
    it('should return sitelinks', function() {
        const result = block({}, {
            orgProperties: {
                'sitelinks/1.x': [
                    { title: 'title #1', url: 'url #1' },
                    { title: 'title #2', url: 'url #2' }
                ]
            }
        });

        assert.deepEqual(result, [
            { title: 'title #1', url: 'url #1' },
            { title: 'title #2', url: 'url #2' }
        ]);
    });

    it('should return `undefined` if sitelinks/1.x is not array', function() {
        assert.equal(block({}, {}), undefined);
        assert.equal(block({}, {
            orgProperties: {}
        }), undefined);
        assert.equal(block({}, {
            orgProperties: { 'sitelinks/1.x': null }
        }), undefined);
        assert.equal(block({}, {
            orgProperties: { 'sitelinks/1.x': '' }
        }), undefined);
        assert.equal(block({}, {
            orgProperties: { 'sitelinks/1.x': 1 }
        }), undefined);
    });
});
