describeBlock('b-page__ograph-generator', function(block) {
    var ogProps = {
            props: {
                type: 'website',
                title: 'title',
                description: 'description',
                url: 'url',
                site_name: 'Yandex',
                image: 'img'
            }
        },
        twiProps;

    beforeEach(function() {
        twiProps = {
            props: {
                site: '@yandex'
            },
            prefix: 'twitter'
        };
    });

    it('should return as many tags as props were passed', function() {
        var result = block([ogProps]);

        assert.lengthOf(result[0], Object.keys(ogProps.props).length);
    });

    it('should generate right property attribute when prefix was passed', function() {
        var result = block([twiProps]);

        assert.equal(result[0][0].attrs.property, 'twitter:site');
    });

    it('should generate property attribute with "og:" prefix when prefix wasn\'t passed', function() {
        twiProps.prefix = '';
        var result = block([twiProps]);

        assert.equal(result[0][0].attrs.property, 'og:site');
    });
});
