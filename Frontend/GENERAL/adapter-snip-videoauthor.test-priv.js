describeBlock('adapter-snip-videoauthor__url', function(block) {
    var context, snippet, service;

    beforeEach(function() {
        context = {
            expFlags: {},
            reportData: {
                reqdata: {
                    reqid: 'channel-reqid'
                }
            }
        };

        snippet = {
            Id: 'UCf7BTEBec56oMjR9nz09Nzw',
            MainUrl: 'https://www.youtube.com/channel/UCf7BTEBec56oMjR9nz09Nzw'
        };

        service = sinon.stub(RequestCtx.Service, 'service').returns({
            id: 'video',
            label: 'Видео',
            params: '',
            pathnames: '',
            root: '//yandex.ru/video',
            search: '/search?text=bmw'
        });
    });

    afterEach(function() {
        service.restore();
    });

    it('should return correct url', function() {
        assert.strictEqual(block(context, snippet), '//yandex.ru/video/search?text=bmw&source=channel_web' +
            '&parent-reqid=channel-reqid&noreask=1&channelId=UCf7BTEBec56oMjR9nz09Nzw');
    });

    it('should contain correct cgi param text if channel name is present', function() {
        snippet.Name = 'Colors With Baby Learn';

        assert.include(block(context, snippet), 'text=Colors%20With%20Baby%20Learn');
    });
});

describeBlock('adapter-snip-videoauthor__info', function(block) {
    var context, snippet;

    beforeEach(function() {
        snippet = {};
        context = {};
    });

    it('should contain correct number of subscribers if there is SubscriberCount field in data', function() {
        snippet.SubscriberCount = 1000;

        assert.deepEqual(block(context, snippet),
            {
                items: [{
                    title: {
                        block: 'i18n',
                        keyset: 'adapter-snip-videoauthor',
                        key: 'Подписчиков',
                        context: 'Подпись в "Подписчиков: 2 млн". https://nda.ya.ru/3Trypr'
                    },
                    text: '1 тыс.'
                }]
            });
    });

    it('should not contain "Subscribers" field if there is no SubscriberCount field in data', function() {
        assert.isUndefined(block(context, snippet));
    });
});
