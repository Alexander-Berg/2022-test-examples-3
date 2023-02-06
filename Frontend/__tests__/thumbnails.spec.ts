import { IReportPicture } from '@yandex-turbo/applications/beru.ru/interfaces';
import { convertPictureToThumbnail, getSafePicture } from '../thumbnails';

const stubFormatData = {
    src: '//avatars.mds.yandex.net/get-mpic/1382936/img_id3557252912121609779.png/5hq',
    srcSet: '//avatars.mds.yandex.net/get-mpic/1382936/img_id3557252912121609779.png/9hq 2.5x',
};

export const oldFormatData = {
    url: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg',
    set: [
        { width: 50, height: 50 },
        { width: 55, height: 70 },
        { width: 60, height: 80 },
        { width: 74, height: 100 },
        { width: 75, height: 75 },
        { width: 90, height: 120 },
        { width: 100, height: 100 },
        { width: 120, height: 160 },
        { width: 150, height: 150 },
        { width: 180, height: 240 },
        { width: 190, height: 250 },
        { width: 200, height: 200 },
        { width: 240, height: 320 },
        { width: 300, height: 300 },
        { width: 300, height: 400 },
        { width: 600, height: 600 },
    ],
};

const thumbnails = [
    {
        entity: 'picture',
        original: {
            width: 375,
            height: 701,
            namespace: 'mpic',
            groupId: 1363071,
            key: 'img_id1476273243410111579.jpeg',
        },
    },
    {
        entity: 'picture',
        original: {
            width: 400,
            height: 450,
            namespace: 'mpic',
            groupId: 1363071,
            key: 'img_id147627324341012313.jpeg',
        },
    },
] as IReportPicture[];

export const reportFormatData = {
    knownThumbnails: [
        {
            namespace: 'marketpic',
            thumbnails: [
                { name: '50x50', width: 50, height: 50 },
                { name: '55x70', width: 55, height: 70 },
                { name: '60x80', width: 60, height: 80 },
                { name: '74x100', width: 74, height: 100 },
                { name: '75x75', width: 75, height: 75 },
                { name: '90x120', width: 90, height: 120 },
                { name: '100x100', width: 100, height: 100 },
                { name: '120x160', width: 120, height: 160 },
                { name: '150x150', width: 150, height: 150 },
                { name: '180x240', width: 180, height: 240 },
                { name: '190x250', width: 190, height: 250 },
                { name: '200x200', width: 200, height: 200 },
                { name: '240x320', width: 240, height: 320 },
                { name: '300x300', width: 300, height: 300 },
                { name: '300x400', width: 300, height: 400 },
                { name: '600x600', width: 600, height: 600 },
                { name: '600x800', width: 600, height: 800 },
                { name: '900x1200', width: 900, height: 1200 },
                { name: 'x124_trim', width: 166, height: 124 },
                { name: 'x166_trim', width: 248, height: 166 },
                { name: 'x248_trim', width: 332, height: 248 },
                { name: 'x332_trim', width: 496, height: 332 },
            ],
        },
        {
            namespace: 'mpic',
            thumbnails: [
                { name: '1hq', width: 50, height: 50 },
                { name: '2hq', width: 100, height: 100 },
                { name: '3hq', width: 75, height: 75 },
                { name: '4hq', width: 150, height: 150 },
                { name: '5hq', width: 200, height: 200 },
                { name: '6hq', width: 250, height: 250 },
                { name: '7hq', width: 120, height: 120 },
                { name: '8hq', width: 240, height: 240 },
                { name: '9hq', width: 500, height: 500 },
                { name: 'x124_trim', width: 166, height: 124 },
                { name: 'x166_trim', width: 248, height: 166 },
                { name: 'x248_trim', width: 332, height: 248 },
                { name: 'x332_trim', width: 496, height: 332 },
            ],
        },
    ],
    ...thumbnails[0],
};

// tslint:disable:max-line-length
describe('convertPictureToThumbnail', () => {
    describe('конвертирование данных из формата, принимаемого изображением (из стабов)', () => {
        it('вернёт данные без изменений', () => {
            expect(convertPictureToThumbnail(stubFormatData)).toEqual(stubFormatData);
        });
    });

    // TODO: выпилить поддержку старого формата https://st.yandex-team.ru/BLUEMARKET-5113
    describe('конвертирование данных из старого формата', () => {
        describe('для квадрата со стороной 250px', () => {
            it('вернёт верно подобранные изображения', () => {
                const etalonData = {
                    src: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/300x300',
                    srcSet: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/300x300 1.2x, //avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/600x600 2.4x',
                };
                expect(convertPictureToThumbnail(oldFormatData, 250)).toEqual(etalonData);
            });
        });

        describe('для ширины 250px', () => {
            it('вернёт верно подобранные изображения', () => {
                const etalonData = {
                    src: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/300x300',
                    srcSet: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/300x300 1.2x, //avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/600x600 2.4x',
                };
                expect(convertPictureToThumbnail(oldFormatData, 250, false, 'horizontal')).toEqual(etalonData);
            });
        });

        describe('для высоты 250px', () => {
            it('вернёт верно подобранные изображения', () => {
                const etalonData = {
                    src: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/190x250',
                    srcSet: '//avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/190x250, //avatars.mds.yandex.net/get-mpic/1244413/img_id1649353673440750464.jpeg/600x600 2.4x',
                };
                expect(convertPictureToThumbnail(oldFormatData, 250, false, 'vertical')).toEqual(etalonData);
            });
        });
    });

    describe('конвертирование данных из формата репорта', () => {
        describe('для квадрата со стороной 250px', () => {
            it('вернёт верно подобранные изображения', () => {
                const etalonData = {
                    src: '//avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/6hq',
                    srcSet: '//avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/6hq, //avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/9hq 2x',
                };
                expect(convertPictureToThumbnail(reportFormatData, 250)).toEqual(etalonData);
            });
        });

        describe('для ширины 250px', () => {
            it('вернёт верно подобранные изображения', () => {
                const etalonData = {
                    src: '//avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/6hq',
                    srcSet: '//avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/6hq, //avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/9hq 2x',
                };
                expect(convertPictureToThumbnail(reportFormatData, 250, false, 'horizontal')).toEqual(etalonData);
            });
        });

        describe('для высоты 250px', () => {
            it('вернёт верно подобранные изображения', () => {
                const etalonData = {
                    src: '//avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/6hq',
                    srcSet: '//avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/6hq, //avatars.mds.yandex.net/get-mpic/1363071/img_id1476273243410111579.jpeg/9hq 2x',
                };
                expect(convertPictureToThumbnail(reportFormatData, 250, false, 'vertical')).toEqual(etalonData);
            });
        });
    });

    describe('конвертирование данных из неизвестного формата', () => {
        it('Вернёт ошибку типа', () => {
            expect(() => {
                // @ts-ignore: передаём неправильный формат
                convertPictureToThumbnail({ rakamakafoo: true }, 250);
            }).toThrow(
                // @ts-ignore
                new TypeError('Попытка сконвертировать изображения из неожиданного формата')
            );
        });
    });
});

describe('getSafePicture', () => {
    it('должен из списка картинок, возвращать всегда первую в поддерживаемом формате "convertPictureToThumbnail"', () => {
        expect(getSafePicture(reportFormatData.knownThumbnails, thumbnails)).toEqual({
            knownThumbnails: reportFormatData.knownThumbnails,
            ...thumbnails[0],
        });
    });

    it('должен возвращать объект "заглушку", если картинки не переданы', () => {
        expect(getSafePicture(reportFormatData.knownThumbnails, [])).toEqual({ src: '', srcSet: '' });
        expect(getSafePicture(reportFormatData.knownThumbnails, undefined)).toEqual({ src: '', srcSet: '' });
    });
});
