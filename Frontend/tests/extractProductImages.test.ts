import { extractProductImages } from '../extractProductImages';

describe('extractProductImages', () => {
    it('возвращает корректный результат, если передан массив с элементом undefined', () => {
        expect(extractProductImages([
            '//avatars.mds.yandex.net/get-mpic/4501142/img_id3567689757960365754.jpeg/orig',
            '',
            '//avatars.mds.yandex.net/get-mpic/4345877/img_id2284281129580616507.jpeg/orig',
        ])).toStrictEqual([
            {
                src: '//avatars.mds.yandex.net/get-mpic/4501142/img_id3567689757960365754.jpeg/600x600',
                srcHd: '//avatars.mds.yandex.net/get-mpic/4501142/img_id3567689757960365754.jpeg/600x600',
            },
            {
                src: '//avatars.mds.yandex.net/get-mpic/4345877/img_id2284281129580616507.jpeg/600x600',
                srcHd: '//avatars.mds.yandex.net/get-mpic/4345877/img_id2284281129580616507.jpeg/600x600',
            },
        ]);
    });
});
