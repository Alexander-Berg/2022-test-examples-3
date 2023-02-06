import { getSrcSet, getSizeUrl } from '../../../../lib/helpers/sizes';

describe('sizes', () => {
    const sizes = [
        { url: 'https://downloader/ORIGINAL', name: 'ORIGINAL' },
        { url: 'https://downloader/DEFAULT', name: 'DEFAULT' },
        { url: 'https://downloader/XXXS', name: 'XXXS' },
        { url: 'https://downloader/XXS', name: 'XXS' },
        { url: 'https://downloader/XS', name: 'XS' },
        { url: 'https://downloader/S', name: 'S' },
        { url: 'https://downloader/M', name: 'M' },
        { url: 'https://downloader/L', name: 'L' },
        { url: 'https://downloader/XL', name: 'XL' },
        { url: 'https://downloader/XXL', name: 'XXL' },
        { url: 'https://downloader/XXXL', name: 'XXXL' }
    ];

    describe('getSrcSet', () => {
        it('должен учитывать только размеры с M по XXXL', () => {
            expect(getSrcSet(sizes)).toBe(
                'https://downloader/M 300w,https://downloader/L 500w,https://downloader/XL 800w,https://downloader/XXL 1024w,https://downloader/XXXL 1280w'
            );
        });
    });

    describe('getSizeUrl', () => {
        it('Должен возвращать корректный url', () => {
            expect(getSizeUrl(sizes, 'M')).toBe('https://downloader/M');
        });
    });
});
