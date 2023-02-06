describe('i-device-info', function() {
    var block;

    before(function() {
        block = buildDomBlock('i-device-info', {
            block: 'i-device-info',
            js: true
        });
    });

    describe('#validateScreenSize()', function() {
        describe('screenSize.h more than screenSize.w', function() {
            // По условию на тачевых устройствах(планшеты + телефоны)
            // значение ширины должно быть больше значения высоты,
            // поэтому если это не так - меняем их местами
            it('should swap screen height with screen width', function() {
                var notValidSize = {
                        w: 480,
                        h: 640
                    },
                    validatedSize = block.validateScreenSize(notValidSize);

                assert.equal(notValidSize.w, validatedSize.h);
                assert.equal(notValidSize.h, validatedSize.w);
            });
        });
    });
});
