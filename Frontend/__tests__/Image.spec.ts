const ImageParser = require('../index');

describe('Customs CSS migration', () => {
    describe('Image styles', () => {
        it('Заменяет класс .image на .turbo-image', () => {
            expect(ImageParser.transform('.image{}.image__picture{}.image__img{}'))
                .toEqual('.turbo-image{}.turbo-image{}.turbo-image{}');
        });

        it('Убирает лишние классы', () => {
            expect(ImageParser.transform('.test.header_test{}.image{}.test.unit_test{}')).toEqual('.turbo-image{}');
        });

        it('Заменяет классы с комбинаторами на .turbo-external-resource', () => {
            expect(ImageParser.transform('.image+.cover{}.cover~.image{}'))
                .toEqual('.turbo-external-resource+.cover{}.cover~.turbo-external-resource{}');
        });

        it('Заменяет класс .image_type_edge на .turbo-external-resource', () => {
            expect(ImageParser.transform('.image_type_edge{}'))
                .toEqual('.turbo-external-resource{}');
        });

        it('Убирает класс image_js_inited', () => {
            expect(ImageParser.transform('.image.image_js_inited{}'))
                .toEqual('.turbo-image{}');
        });

        it('Заменяет класс .image_caption_on на .turbo-media', () => {
            expect(ImageParser.transform('.image_caption_on{}'))
                .toEqual('.turbo-media{}');
        });

        it('Заменяет класс .image__capture на .turbo-media__caption', () => {
            expect(ImageParser.transform('.image__capture{}'))
                .toEqual('.turbo-media__caption{}');
        });

        it('Заменяет классы с модификаторами inline и block, преобразует landscape, portrait в block', () => {
            expect(ImageParser.transform('.image_type_inline{}.image_type_landscape{}.image_type_portrait{}'))
                .toEqual('.turbo-image_type_inline{}.turbo-image_type_block{}.turbo-image_type_block{}');
        });
    });
});
