import { getMimeTypeForPreview } from '../image';

describe('Image', () => {
    describe('getMimeType', () => {
        it('Should returns png mime type', () => {
            expect(getMimeTypeForPreview('image.png')).toEqual('image/png');
            expect(getMimeTypeForPreview('image.PnG')).toEqual('image/png');
            expect(getMimeTypeForPreview('image.gif')).toEqual('image/png');
            expect(getMimeTypeForPreview('image.bmp')).toEqual('image/png');
            expect(getMimeTypeForPreview('image.webp')).toEqual('image/png');
        });

        it('Should returns mime type for jpeg', () => {
            expect(getMimeTypeForPreview('image.jpg')).toEqual('image/jpeg');
            expect(getMimeTypeForPreview('image.jpeg')).toEqual('image/jpeg');
        });

        it('Should returns mime type for other file extensions', () => {
            expect(getMimeTypeForPreview('image.unknown')).toEqual('image/jpeg');
        });
    });
});
