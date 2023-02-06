describe('i-images-viewer__utils', function() {
    var utils = blocks['i-images-viewer__utils'],
        sandbox,
        context;

    beforeEach(function() {
        context = {
            expFlags: {},
            reportData: stubData('experiments', 'cgi', 'user-time'),
            reqdata: {}
        };

        sandbox = sinon.createSandbox();
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('getImagesUrl()', function() {
        it('should return correct Yandex.Images service urls', function() {
            sandbox.stub(utils, 'getService').returns({
                root: 'http://ya.ru/',
                search: 'search?text=bmw',
                params: '&parent-reqid=12345&source=wiz'
            });

            assert.deepEqual(utils.getImagesUrl(context), {
                index: 'http://ya.ru/?parent-reqid=12345&source=wiz',
                search: 'http://ya.ru/search?text=bmw&parent-reqid=12345&source=wiz'
            });
        });
    });

    describe('addReqidParam()', function() {
        var reqid = '12345';

        beforeEach(function() {
            context.reportData.reqdata.reqid = reqid;
        });

        it('should return undefined when url not passed as argument', function() {
            assert.isUndefined(utils.addReqidParam(context.reportData));
        });

        it('should return absolute url with reqid param when url with host passed', function() {
            var url = 'https://ya.ru/';

            assert.strictEqual(utils.addReqidParam(context.reportData, url), `${url}?parent-reqid=${reqid}`);
        });

        it('should return relative url with reqidparam when url without host passed', function() {
            var url = '/search';

            assert.strictEqual(utils.addReqidParam(context.reportData, url), `${url}?parent-reqid=${reqid}`);
        });
    });

    describe('thumbSize()', function() {
        it('should returns initial width and height when n!=33 and height<100', function() {
            assert.deepEqual(utils.thumbSize(100, 90, 22, 120), { width: 100, height: 90 });
        });

        it('should returns initial width and height if parameter n not passed', function() {
            assert.deepEqual(utils.thumbSize(130, 130, undefined, 120), { width: 130, height: 130 });
        });

        it('should return a correct value of square image with n=33 and height<100', function() {
            assert.deepEqual(utils.thumbSize(90, 90, 33, 100), { width: 100, height: 100 });
        });

        it('should return a correct value of square image with n=33', function() {
            assert.deepEqual(utils.thumbSize(100, 100, 33, 170), { width: 170, height: 170 });
        });

        it('should returns correct sizes of panorama with n=33', function() {
            assert.deepEqual(utils.thumbSize(1000, 100, 33, 170), { width: 480, height: 48 });
        });

        it('should return a correct value of rectangle image with n=33', function() {
            assert.deepEqual(utils.thumbSize(300, 200, 33, 170), { width: 255, height: 170 });
        });

        it('should returns correct sizes of image with n=33', function() {
            assert.deepEqual(utils.thumbSize(200, 170, 33, 170), { width: 200, height: 170 });
        });

        it('should return a correct value of rectangle image with n=21', function() {
            assert.deepEqual(utils.thumbSize(300, 200, 21), { width: 225, height: 150 });
        });

        it('should return a correct value of rectangle image with n=24', function() {
            assert.deepEqual(utils.thumbSize(300, 200, 24), { width: 195, height: 130 });
        });

        it('should return a correct value of rectangle image with n=22', function() {
            assert.deepEqual(utils.thumbSize(300, 200, 22, 120), { width: 180, height: 120 });
        });

        it('should return a correct value of rectangle image with n=22 and too long image', function() {
            assert.deepEqual(utils.thumbSize(480, 124, 22, 120), { width: 360, height: 93 });
        });
    });

    describe('getCropParams()', function() {
        var image, thumbParams;

        beforeEach(function() {
            image = {
                bdr: '2.5x2.5+95.1x95.0'
            };

            thumbParams = {
                width: 222,
                height: 320,
                n: 13
            };
        });

        it('should return an empty value without flag imgwiz_thumb_crop', function() {
            context.expFlags.imgwiz_thumb_crop = 0;

            assert.isUndefined(utils.getCropParams(context, image, thumbParams));
        });

        it('should return an empty value without image data', function() {
            delete image.bdr;

            assert.isUndefined(utils.getCropParams(context, image, thumbParams));
        });

        it('should return an empty value without crop parsed data', function() {
            sandbox.stub(utils, 'parseCrop').returns(undefined);

            assert.isUndefined(utils.getCropParams(context, image, thumbParams));
        });

        it('should return a correct value', function() {
            var result = {
                crop: {
                    x: 2.5,
                    y: 2.5,
                    w: 95.1,
                    h: 95
                },
                image: {
                    height: 320,
                    width: 223
                },
                thumb: {
                    height: 337,
                    width: 234
                }
            };

            context.expFlags.imgwiz_thumb_crop = 1;

            assert.deepEqual(utils.getCropParams(context, image, thumbParams), result);
        });
    });

    describe('parseCrop()', function() {
        it('should return a correct value', function() {
            assert.deepEqual(utils.parseCrop('3.4x34+76x32'), { x: 3.4, y: 34, w: 76, h: 32 });
        });

        it('should return an empty value for panoramas', function() {
            assert.isUndefined(utils.parseCrop('0x0+0x0'));
        });

        it('should return an empty value if crop is redundant', function() {
            assert.isUndefined(utils.parseCrop('0x0+100x100'));
        });
    });

    describe('calcCropOffset()', function() {
        it('should return a correct value with zeros params', function() {
            assert.deepEqual(
                utils.calcCropOffset({ x: 0, y: 0, w: 0, h: 0 }, { width: 0, height: 0 }),
                { x: -0, y: -0 }
            );
        });

        it('should return a correct value with zeros params', function() {
            assert.deepEqual(
                utils.calcCropOffset({ x: 0, y: 0, w: 20, h: 320 }, { width: 0, height: 0 }),
                { x: -0, y: -0 }
            );
        });

        it('should return a correct value with zeros params', function() {
            assert.deepEqual(
                utils.calcCropOffset({ x: 0, y: 0, w: 0, h: 0 }, { width: 100, height: 90 }),
                { x: -0, y: -0 }
            );
        });

        it('should return a correct value with zeros params', function() {
            assert.deepEqual(
                utils.calcCropOffset({ x: 0, y: 0, w: 30, h: 50 }, { width: 100, height: 90 }),
                { x: -0, y: -0 }
            );
        });

        it('should return a correct value', function() {
            assert.deepEqual(
                utils.calcCropOffset({ x: 10, y: 20, w: 20, h: 20 }, { width: 100, height: 90 }),
                { x: -50, y: -90 }
            );
        });
    });

    describe('with getThumbSize', function() {
        var image,
            szm,
            height,
            params;

        stubBlocks('RequestCtx');

        beforeEach(function() {
            image = {
                thmb_h: 150,
                thmb_w: 200,
                thmb_href: '//im0-tub-ru.yandex.net/i?id=7e8d1b5f1b65facf15e46c99235231cf'
            };
            height = 110;
            szm = {
                viewportSize: {
                    w: 350,
                    h: 550
                },
                devicePixelRatio: 1
            };

            RequestCtx.RetinaScale.getScale.returns(2);
        });

        it('should return computed size for height 110px (retina 2 DPR)', function() {
            params = { cookieSzm: szm, targetHeight: height };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'height', 110);
            assert.propertyVal(utils.getThumbSize(context, image, params), 'width', 146);
            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=13');
        });

        it('should return computed size for height 110px (1.3 DPR)', function() {
            RequestCtx.RetinaScale.getScale.returns(1.3);
            params = { cookieSzm: szm, targetHeight: height };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'height', 110);
            assert.propertyVal(utils.getThumbSize(context, image, params), 'width', 146);
            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=13');
        });

        it('should return computed size for height 110px (1.5 DPR)', function() {
            RequestCtx.RetinaScale.getScale.returns(1.5);
            params = { cookieSzm: szm, targetHeight: height };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'height', 110);
            assert.propertyVal(utils.getThumbSize(context, image, params), 'width', 146);
            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=13');
        });

        it('should return computed n = 13 given in adaptive config flag', function() {
            context.expFlags.imgwiz_adaptive_config = '974x618:numdoc=20:h=190:n=33,300x500:numdoc=14:h=180:n=13';
            params = { cookieSzm: szm, targetHeight: height };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=13');
        });

        it('should return computed n = 33 given in adaptive config flag', function() {
            context.expFlags.imgwiz_adaptive_config = '974x618:numdoc=20:h=190:n=33,300x500:numdoc=14:h=180:n=33';
            params = { cookieSzm: szm, targetHeight: height };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=33&h=180&w=240');
        });

        it('should return extra params in url given in adaptive config flag', function() {
            var expected = '&n=33&h=180&w=240&q=15&enc=pjpeg&blur=5&shower=0.3';
            context.expFlags.imgwiz_adaptive_config = '300x500:numdoc=14:h=180:n=33:' +
                'enc=pjpeg:shower=0.3:blur=5:q=15:z=0';
            params = { cookieSzm: szm, targetHeight: height };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', expected);
        });

        it('should return computed n = 13 given in custom adaptive config flag and do not use exp flag', function() {
            context.expFlags.imgwiz_adaptive_config = '974x618:numdoc=20:h=190:n=33,300x500:numdoc=14:h=180:n=33';
            params = {
                cookieSzm: szm,
                targetHeight: height,
                customAdaptiveConfig: '974x618:numdoc=20:h=190:n=33,300x500:numdoc=14:h=180:n=13'
            };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=13');
        });

        it('should return computed n = 13 given in custom adaptive config flag', function() {
            params = {
                cookieSzm: szm,
                targetHeight: height,
                customAdaptiveConfig: '974x618:numdoc=20:h=190:n=33,300x500:numdoc=14:h=180:n=13'
            };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=13');
        });

        it('should return computed n = 33 given in custom adaptive config flag', function() {
            params = {
                cookieSzm: szm,
                targetHeight: height,
                customAdaptiveConfig: '974x618:numdoc=20:h=190:n=33,300x500:numdoc=14:h=180:n=33'
            };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', '&n=33&h=180&w=240');
        });

        it('should return extra params in url given in custom adaptive config flag', function() {
            var expected = '&n=33&h=180&w=240&q=15&enc=pjpeg&blur=5&shower=0.3';
            params = {
                cookieSzm: szm,
                targetHeight: height,
                customAdaptiveConfig: '300x500:numdoc=14:h=180:n=33:enc=pjpeg:shower=0.3:blur=5:q=15:z=0'
            };

            assert.propertyVal(utils.getThumbSize(context, image, params), 'n', expected);
        });
    });

    describe('getImageUrl', function() {
        var image,
            service;

        beforeEach(function() {
            image = {
                detail_url: '/images/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz'
            };

            service = {
                root: '//yandex.ru/images',
                params: '&my-param=beautiful'
            };
        });

        it('should return empty string if detail_url is undefined', function() {
            delete image.detail_url;

            assert.strictEqual(utils.getImageUrl(image, service), '');
        });

        it('should return detail_url as is if it absolute', function() {
            image.detail_url = '//yandex.ru/images/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz';

            assert.strictEqual(utils.getImageUrl(image, service), '//yandex.ru/images/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz');
        });

        it('should return correct url with service params', function() {
            assert.strictEqual(utils.getImageUrl(image, service), '//yandex.ru/images/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz&my-param=beautiful');
        });

        it('should return correct url with service params for Turkey', function() {
            service.root = '//yandex.com.tr/gorsel';

            assert.strictEqual(utils.getImageUrl(image, service), '//yandex.com.tr/gorsel/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz&my-param=beautiful');
        });

        it('should return correct url with service params and foreign detail_url for Turkey', function() {
            service.root = '//yandex.com.tr/gorsel';
            image.detail_url = '/gorsel/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz';

            assert.strictEqual(utils.getImageUrl(image, service), '//yandex.com.tr/gorsel/search?text=istanbul&img_url=https://some.site.url/images/gorsel/test&source=wiz&my-param=beautiful');
        });
    });

    describe('getDomain', function() {
        it('should return correct domain', function() {
            assert.equal(utils.getDomain('www.yandex.ru'), 'www.yandex.ru');
        });

        it('should return correct domain without "www."', function() {
            assert.equal(utils.getDomain('www.yandex.ru', true), 'yandex.ru');
        });
    });
});
