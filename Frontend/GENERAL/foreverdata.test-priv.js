describeBlock('foreverdata__hash', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('cgi');
    });

    it('should keep hash undefined without parameters in url', function() {
        data.reqdata.url = 'https://yandex.ru/search/?text=test';

        var hash = block(data);

        assert.isUndefined(hash);
    });

    it('should extract hash value from CGI param `foreverdata`', function() {
        data.reqdata.url = 'https://yandex.ru/search/?text=test&foreverdata=42';

        var hash = block(data);

        assert.equal(hash, 42);
    });

    it('should extract hash value from exp_flag `foreverdata`', function() {
        data.reqdata.url = 'https://yandex.ru/search/?text=test&exp_flags=foreverdata=146';

        var hash = block(data);

        assert.equal(hash, 146);
    });

    it('should prioritize CGI param `foreverdata` over exp_flag `foreverdata`', function() {
        data.reqdata.url = 'https://yandex.ru/search/?text=test&foreverdata=42&exp_flags=foreverdata=146';

        var hash = block(data);

        assert.equal(hash, 42);
    });
});
