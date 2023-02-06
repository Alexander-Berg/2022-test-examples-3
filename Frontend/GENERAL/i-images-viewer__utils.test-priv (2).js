describe('i-images-viewer__utils', function() {
    var data,
        block = blocks['i-images-viewer__utils'],
        text = 'мадонна фото',
        encodedText = encodeURIComponent(text),
        result;

    stubBlocks(
        'RequestCtx'
    );

    beforeEach(function() {
        data = stubData('prefs', 'region', 'experiments', 'cgi', 'user-time', 'device');
        RequestCtx.GlobalContext.templatePlatform = 'touch-phone';

        data.reqdata.reqid = '123';
        data.reqdata.ycookie = { yp: { szm: '2:320x568:1280x2272' } };

        RequestCtx.GlobalContext.query = {
            uriEscaped: text
        };
        RequestCtx.GlobalContext.reqid = data.reqdata.reqid;
    });

    describe('with getServiceHost', function() {
        it('should return serviceHost url starting with //', function() {
            RequestCtx.GlobalContext.tld = 'ru';
            assert.equal(block.getServiceHost(data), '//yandex.ru');
        });
    });

    describe('with getSearchUrl', function() {
        it('should return correct search url', function() {
            RequestCtx.GlobalContext.tld = 'ru';
            result = block.getSearchUrl(data);
            assert.equal(result, '//yandex.ru/images/touch/search?text=' + encodedText + '&parent-reqid=123');
        });

        it('should return correct search url for comTr', function() {
            RequestCtx.GlobalContext.tld = 'com.tr';
            RequestCtx.GlobalContext.isComTr = true;
            result = block.getSearchUrl(data);
            assert.equal(result, '//yandex.com.tr/gorsel/touch/search?text=' + encodedText + '&parent-reqid=123');
        });

        it('should return correct search url in mobileApp', function() {
            data.reqdata.uuid = 'test-uuid';
            RequestCtx.GlobalContext.uuid = data.reqdata.uuid;
            RequestCtx.GlobalContext.cgi.p.withArgs('app_version').returns('7020600');
            result = block.getSearchUrl(data);

            assert.equal(
                result,
                '//yandex.ru/images/touch/search?text=' + encodedText +
                '&parent-reqid=123&app_version=7020600&uuid=test-uuid'
            );
        });
    });

    describe('with canHavePreview and andriod platform', function() {
        let method = block.canHavePreview;
        let isSupportedPhoneApp;

        beforeEach(function() {
            RequestCtx.GlobalContext.platform = 'android';
            isSupportedPhoneApp = sinon.stub(RequestCtx.YandexApi, 'isSupportedPhoneApp').returns(false);
        });

        afterEach(function() {
            isSupportedPhoneApp.restore();
        });

        it('should not have preview for Android < 4.4', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '4.3';
            assert.isFalse(method(data), 'Android < 4.4 has not preview');
        });

        it('should not have preview for any opera UA', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '4.0';
            RequestCtx.GlobalContext.device.BrowserName = 'Opera';
            assert.isFalse(method(data), 'Android device with opera has not preview');
        });

        it('should have preview for android platform and OS version 4.4', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '4.4';
            assert.isTrue(method(data), 'Android 4.4 device can have preview');
        });

        it('should have preview for android platform and OS version > 4.4', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '5.0.4';
            assert.isTrue(method(data), 'Android > 4.4 can have preview');
        });

        it('should have preview for android platform and OS version > 10.0', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '10.2';
            assert.isTrue(method(data), 'Android > 10.0 can have preview');
        });
    });

    describe('with canHavePreview and ios platform', function() {
        var method = block.canHavePreview;

        beforeEach(function() {
            RequestCtx.GlobalContext.platform = 'ios';
        });

        it('should not have preview for iOS < 4.0', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '3.9';
            assert.isFalse(method(data), 'iOS < 4.0 has not preview');
        });

        it('should not have preview for iOS 4.0', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '4.0';
            assert.isFalse(method(data), 'iOS 4.0 has not preview');
        });

        it('should have preview for iOS > 4.0', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '5.0.4';
            assert.isTrue(method(data), 'iOS > 4.0 can have preview');
        });

        it('should have preview for iOS > 10.0', function() {
            RequestCtx.GlobalContext.device.OSVersionRaw = '10.2';
            assert.isTrue(method(data), 'iOS > 10.0 can have preview');
        });
    });

    describe('with canHavePreview and apps api', function() {
        let isSupportedPhoneApp;
        beforeEach(function() {
            isSupportedPhoneApp = sinon.stub(RequestCtx.YandexApi, 'isSupportedPhoneApp').returns(true);
        });

        afterEach(function() {
            isSupportedPhoneApp.restore();
        });

        it('should return true for supported apps', function() {
            assert.isTrue(block.canHavePreview(data), 'App is supported with preview');
        });
    });
});
