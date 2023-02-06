import { YandexCheckoutCartItemImage as Image } from '@yandex-int/tap-checkout-types';
import { getImageSrc, fallback } from './image';

const images: Array<Image> = [
    { url: 'image-1', width: 10, height: 10 },
    { url: 'image-2', width: 20, height: 20 },
    { url: 'image-3', width: 30, height: 30 },
    { url: 'image-4', width: 30, height: 40 },
    { url: 'image-5', width: 40, height: 30 },
];

describe('getImageSrc()', () => {
    it('should return correct image src', () => {
        expect(getImageSrc({ width: 10, height: 10 }, images)).toEqual({ src: 'image-1', src2x: 'image-2' });
        expect(getImageSrc({ width: 15, height: 15 }, images)).toEqual({ src: 'image-2', src2x: 'image-3' });
        expect(getImageSrc({ width: 16, height: 16 }, images)).toEqual({ src: 'image-2', src2x: 'image-3' });
        expect(getImageSrc({ width: 30, height: 40 }, images)).toEqual({ src: 'image-4', src2x: 'image-4' });
        expect(getImageSrc({ width: 40, height: 30 }, images)).toEqual({ src: 'image-5', src2x: 'image-5' });
    });

    it('should return empty image src', () => {
        expect(getImageSrc({ width: 10, height: 10 })).toEqual({ src: fallback });
        expect(getImageSrc({ width: 10, height: 10 }, [])).toEqual({ src: fallback });
    });
});
