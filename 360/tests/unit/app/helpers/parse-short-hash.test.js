import parseShortHash from '../../../../app/helpers/parse-short-hash';

describe('app/helpers/parse-short-hash', () => {
    it('просто хэш', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg' });
    });

    it('ссылка на картинку размера xxxxs (такого размера нет)', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_xxxxs.jpg'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg_xxxxs.jpg' });
    });

    it('ссылка на картинку размера xxxs', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_xxxs.jpg'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'xxxs' });
    });

    it('ссылка на картинку размера xXs', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_xXs.jpg'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'xxs' });
    });

    it('ссылка на картинку размера s', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_S.JPEG'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 's' });
    });

    it('ссылка на картинку размера m', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_m.png'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'm' });
    });

    it('ссылка на картинку размера l', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_l.png'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'l' });
    });

    it('ссылка на картинку размера XL', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_XL.PNG'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'xl' });
    });

    it('ссылка на картинку размера XXXL', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_xxxL.jpeg'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'xxxl' });
    });

    it('ссылка на картинку размера xxxxl (такого нет)', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_xxxxl.jpeg'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg_xxxxl.jpeg' });
    });

    it('не-яндексовым origin не разрешаем ссылки на картинки и обрезаем расширение', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_l.png', 'https://livejournal.com/asdalsd?a=b'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg' });
        expect(parseShortHash('u1sGbfqK3DXhxg_XXXS.JPEG', 'https://vk.com'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg' });
    });

    it('для tech.yandex.ru разрешаем ссылки на картинки', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_l.png', 'https://tech.yandex.ru/some/page'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'l' });
    });

    it('для yandex-team.ru разрешаем ссылки на картинки', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_l.png', 'https://yandex-team.ru'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'l' });
    });

    it('для *.yandex-team.ru разрешаем ссылки на картинки', () => {
        expect(parseShortHash('u1sGbfqK3DXhxg_l.png', 'https://mail.yandex-team.ru/lite'))
            .toEqual({ shortHash: 'u1sGbfqK3DXhxg', imageSize: 'l' });
    });

    it('не передали хэш (WAT?)', () => {
        expect(parseShortHash())
            .toEqual({ shortHash: '' });
    });
});
