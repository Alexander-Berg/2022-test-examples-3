describe('i-device-info', function() {
    var block,
        assert = chai.assert,

        SCREEN_WIDTH = 640,
        SCREEN_HEIGHT = 480,

        VIEWPORT_WIDTH = 620,
        VIEWPORT_HEIGHT = 475,

        DEVICE_PIXEL_RATIO = '1_5';

    before(function() {
        block = buildDomBlock('i-device-info', {
            block: 'i-device-info',
            js: true
        });

        window.screen = {
            height: 640,
            width: 480
        };

        window.innerWidth = 470;
        window.innerHeight = 620;
    });

    describe('#getScreenSize()', function() {
        var stubs,
            VALIDATED_SCREEN_SIZE = 'validated screen size value';

        before(function() {
            stubs = stubBlockPrototype('i-device-info', {
                validateScreenSize: function() {
                    return VALIDATED_SCREEN_SIZE;
                }
            });
        });

        beforeEach(function() {
            stubs.init();
        });

        afterEach(function() {
            stubs.restore();
        });

        // Метод должен возвращать результат валидирующей функции
        it('should return result of function validateScreenSize', function() {
            var screenSize = block.getScreenSize();

            assert.equal(screenSize, VALIDATED_SCREEN_SIZE);
        });
    });

    it('#getViewportSize() should return info about viewport size', function() {
        var viewportSize = block.getViewportSize();

        assert.equal(viewportSize.w, window.innerWidth);
        assert.equal(viewportSize.h, window.innerHeight);
    });

    describe('#devicePixelRatio()', function() {
        before(function() {
            window.devicePixelRatio = 1.5;
        });

        after(function() {
            delete window.devicePixelRatio;
        });

        it('should return window.devicePixelRatio in right format', function() {
            assert.equal(block.getDevicePixelRatio(), String(window.devicePixelRatio).replace('.', '_'));
        });

        it('should return 1 if window.devicePixelRatio is absent', function() {
            delete window.devicePixelRatio;

            assert.equal(block.getDevicePixelRatio(), 1);
        });
    });

    describe('#getScreenParams()', function() {
        var stubs,
            SCREEN_SIZE_RESULT = 'SCREEN_SIZE_RESULT',
            VIEWPORT_SIZE_RESULT = 'VIEWPORT_SIZE_RESULT';

        before(function() {
            stubs = stubBlockPrototype('i-device-info', {
                getScreenSize: sinon.stub().returns(SCREEN_SIZE_RESULT),
                getViewportSize: sinon.stub().returns(VIEWPORT_SIZE_RESULT)
            });

            stubs.init();
        });

        after(function() {
            stubs.restore();
        });

        it('should have result of #getScreenSize() method in result', function() {
            assert.equal(block.getScreenParams().screenSize, SCREEN_SIZE_RESULT);
        });

        it('should have result of #getViewportSize() method in result', function() {
            assert.equal(block.getScreenParams().viewportSize, VIEWPORT_SIZE_RESULT);
        });

        it('should have devicePixelRatio in result object', function() {
            assert.equal(block.getScreenParams().devicePixelRatio, block.getDevicePixelRatio());
        });
    });

    describe('#updateSzmCookie()', function() {
        var stubs,

            params = 'FAKE_PARAMS',

            SCREEN_PARAMS = 'SCREEN_PARAMS',
            SZM_COOKIE = 'SZM_COOKIE',
            PARSED_SZM_COOKIE = 'PARSED_SZM_COOKIE',

            STRINGIFIED_SZM_COOKIE = 'STRINGIFIED_SZM_COOKIE';

        before(function() {
            stubs = stubBlockPrototype('i-device-info', {
                getScreenParams: sinon.stub().returns(SCREEN_PARAMS),
                getSzmCookie: sinon.stub().returns(SZM_COOKIE),
                parseSzmCookie: sinon.stub().returns(PARSED_SZM_COOKIE),
                stringifySzmCookie: sinon.stub().returns(STRINGIFIED_SZM_COOKIE),
                setSzmCookie: sinon.stub(),
                // Сначала считаем, что значение куки актуально
                isSzmEqual: sinon.stub().returns(true)
            });
        });

        beforeEach(function() {
            stubs.init();

            block.updateSzmCookie(params);
        });

        afterEach(function() {
            stubs.restore();
        });

        it('should call #parseSzmCookie() with result of #getSzmCookie', function() {
            assert.calledWith(block.parseSzmCookie, SZM_COOKIE);
        });

        it('should check relevance of cookie', function() {
            assert.calledWith(block.isSzmEqual, PARSED_SZM_COOKIE, SCREEN_PARAMS);
        });

        // Если значение куки актуально
        it('shouldn`t call #setSzmCookie if cookie has relevant data', function() {
            assert.notCalled(block.setSzmCookie);
        });

        // Если значения параметров экрана отличаются от тех, что в куке
        describe('irrelevant cookie', function() {
            beforeEach(function() {
                // Стабим метод, который говорит о идентичности данных из куки и текущих параметров
                stubs.get('isSzmEqual').returns(false);

                block.updateSzmCookie(params);
            });

            it('should call #stringifySzmCookie() with result of #getScreenParams()', function() {
                assert.calledWith(block.stringifySzmCookie, SCREEN_PARAMS);
            });

            it('should call #setSzmCookie() with result of #stringifySzmCookie()', function() {
                assert.calledWith(block.setSzmCookie, STRINGIFIED_SZM_COOKIE);
            });
        });
    });

    // Метод сравнивает, эквивалентны ли значения в двух куках
    describe('#isSzmEqual()', function() {
        var szm1, szm2;

        beforeEach(function() {
            szm1 = {
                screenSize: {
                    w: SCREEN_WIDTH,
                    h: SCREEN_HEIGHT
                },
                viewportSize: {
                    w: VIEWPORT_WIDTH,
                    h: VIEWPORT_HEIGHT
                },
                devicePixelRatio: DEVICE_PIXEL_RATIO
            };

            szm2 = {
                screenSize: {
                    w: SCREEN_WIDTH,
                    h: SCREEN_HEIGHT
                },
                viewportSize: {
                    w: VIEWPORT_WIDTH,
                    h: VIEWPORT_HEIGHT
                },
                devicePixelRatio: DEVICE_PIXEL_RATIO
            };
        });

        it('should return true if formatted cookies are equal', function() {
            assert.isTrue(block.isSzmEqual(szm1, szm2));
        });

        it('should return false if cookies has different value(s) of screenSize', function() {
            szm1.screenSize.h = 160;

            assert.isFalse(block.isSzmEqual(szm1, szm2));
        });

        it('should return false if cookies has different value(s) of viewportSize', function() {
            szm2.viewportSize.w = 140;

            assert.isFalse(block.isSzmEqual(szm1, szm2));
        });

        it('should return false if cookies has different value(s) of devicePixelRatio', function() {
            szm1.devicePixelRatio = '2';

            assert.isFalse(block.isSzmEqual(szm1, szm2));
        });
    });

    describe('#formatSzmSizeToString()', function() {
        // Параметр обязательный, но блок не должен падать с ошибкой,
        // чтобы не делать дополнительные проверки уровнем выше
        it('should return undefined without params', function() {
            assert.isUndefined(block.formatSzmSizeToString());
        });

        it('should convert object to right string format', function() {
            var size = { w: 200, h: 100 };

            assert.equal(block.formatSzmSizeToString(size), '200x100');
        });
    });

    describe('#formatSzmSizeToObject()', function() {
        // не должен падать без параметров, см. коммент выше
        it('should return undefined without params', function() {
            assert.isUndefined(block.formatSzmSizeToObject());
        });

        it('should convert string to right object format', function() {
            var str = '200x100'; // Ширина x Высота

            assert.deepEqual(block.formatSzmSizeToObject(str), {
                w: 200,
                h: 100
            });
        });
    });

    describe('#stringifySzmCookie()', function() {
        // Конвертируем куку в четко определенный формат (SERP-38574)
        it('should convert cookie to right format', function() {
            assert.equal(
                block.stringifySzmCookie({
                    screenSize: {
                        w: SCREEN_WIDTH,
                        h: SCREEN_HEIGHT
                    },
                    viewportSize: {
                        w: VIEWPORT_WIDTH,
                        h: VIEWPORT_HEIGHT
                    },
                    devicePixelRatio: DEVICE_PIXEL_RATIO
                }),
                [
                    DEVICE_PIXEL_RATIO,
                    SCREEN_WIDTH + 'x' + SCREEN_HEIGHT,
                    VIEWPORT_WIDTH + 'x' + VIEWPORT_HEIGHT
                ].join(':')
            );
        });
    });

    describe('#parseSzmCookie()', function() {
        // D_D : ScreenW x ScreenH : ViewportW x ViewportH
        // D_D - devicePixelRatio
        var SZM_COOKIE = '2_5:640x480:630x475',
            parsedCookie;

        before(function() {
            parsedCookie = block.parseSzmCookie(SZM_COOKIE);
        });

        it('should have devicePixelRatio in result object', function() {
            assert.equal(parsedCookie.devicePixelRatio, 2.5);
        });

        // Здесь и ниже – цифры взяты из SZM_COOKIE, описанной выше
        it('should have screenSize in result object', function() {
            assert.equal(parsedCookie.screenSize.w, 640);
            assert.equal(parsedCookie.screenSize.h, 480);
        });

        it('should have viewportSize in result object', function() {
            assert.equal(parsedCookie.viewportSize.w, 630);
            assert.equal(parsedCookie.viewportSize.h, 475);
        });
    });
});
