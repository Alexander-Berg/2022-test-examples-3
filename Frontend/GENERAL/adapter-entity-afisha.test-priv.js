describeBlock('adapter-entity-afisha__full-schedule', function(block) {
    let context;
    let params;
    let result;
    let service;

    beforeEach(function() {
        context = {
            reportData: {},
            expFlags: {}
        };

        params = {
            afishaId: '1234567'
        };

        service = sinon.stub(RequestCtx.Service, 'service').returns({
            root: '//afisha.yandex.ru'
        });
    });

    afterEach(function() {
        service.restore();
    });

    it('should return correct url', function() {
        result = block(context, params);

        assert.strictEqual(result.url, '//afisha.yandex.ru/?eventId=1234567&from=qa');
    });

    it('should use city ID if it exists', function() {
        params.cityId = 'yekaterinburg';

        result = block(context, params);

        assert.strictEqual(
            result.url,
            '//afisha.yandex.ru/yekaterinburg/cinema/?eventId=1234567&from=qa'
        );
    });
});
