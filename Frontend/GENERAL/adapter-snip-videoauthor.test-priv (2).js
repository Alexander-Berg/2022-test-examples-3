describeBlock('adapter-snip-videoauthor__path', function(block) {
    var context, snippet, doc;

    beforeEach(function() {
        context = {};
        snippet = {
            Name: 'Colors With Baby Learn',
            MainUrl: 'https://www.youtube.com/channel/UCf7BTEBec56oMjR9nz09Nzw'
        };
        doc = {
            supplementary: {
                domain_link: [
                    {
                        domain_href: 'https://www.youtube.com/',
                        green_domain: 'youtube.com'
                    }
                ]
            }
        };
    });

    it('should return correct path for empty domain_link', function() {
        doc.supplementary.domain_link = [];

        assert.deepEqual(block(context, snippet, doc), [{
            text: 'Colors With Baby Learn',
            url: 'https://www.youtube.com/channel/UCf7BTEBec56oMjR9nz09Nzw'
        }]);
    });

    it('should return correct path', function() {
        assert.deepEqual(block(context, snippet, doc), [
            {
                text: 'youtube.com',
                url: 'https://www.youtube.com/'
            },
            {
                text: 'Colors With Baby Learn',
                url: 'https://www.youtube.com/channel/UCf7BTEBec56oMjR9nz09Nzw'
            }
        ]);
    });
});
