describeBlock('favicons-push-css__build-url', function(block) {
    var data,
        params;

    stubBlocks('favicons-push-css');

    beforeEach(function() {
        data = stubData();
        data.favicons = [];
        params = {
            cssParams: _.constant({
                size: 32
            })
        };
    });

    it('should contain stub params for favicon url', function() {
        var url = block(data, params);

        assert.equal(url, '//favicon.yandex.net/favicon/?size=32&color=255%2C255%2C255%2C0&stub=1');
    });

    it('should return the url with the hosts in lowercase', function() {
        data.favicons = ['www.WildBerries.ru'];

        assert.equal(
            block(data, params),
            '//favicon.yandex.net/favicon/www.wildberries.ru?size=32&color=255%2C255%2C255%2C0&stub=1'
        );
    });

    describe('v2 API', function() {
        beforeEach(function() {
            data = stubData();
            data.favicons = [];
            params = {
                cssParams: _.constant({
                    size: 32
                }),
                faviconVersion: 2,
                separator: ';',
                color: ''
            };
        });

        it('should contain stub params for favicon url', function() {
            var url = block(data, params);

            assert.equal(url, '//favicon.yandex.net/favicon/v2/?size=32&stub=1');
        });

        it('should report error if url contains special characters', function() {
            for (let specialChar of ['?', '#', '(', ')']) {
                const favicon = 'https://www.host.ru/path';
                data.favicons = [`${favicon}${specialChar}`];

                RequestCtx.Logger.reportError.reset();

                const url = block(data, params);
                // encodeURIComponent не работает для скобок, поэтому кодируем вручную
                const charCode = specialChar.charCodeAt(0).toString(16);
                const expectedUrl = `//favicon.yandex.net/favicon/v2/${favicon}%${charCode}?size=32&stub=1`;
                assert.equal(url, expectedUrl);

                assert.calledOnce(RequestCtx.Logger.reportError);

                const errorMessage = 'Путь для спрайта с favicon содержит запрещенные символы';

                const firstArg = RequestCtx.Logger.reportError.firstCall.args[0];
                const thirdArg = RequestCtx.Logger.reportError.firstCall.args[2];
                expect(firstArg).to.be.instanceof(Error);
                expect(firstArg.message).to.equal(errorMessage);
                expect(thirdArg.additional).to.equal(`{"symbol":"${specialChar}","path":"${data.favicons[0]}"}`);
            }
        });
    });
});
