import {
    getMdsUrl,
    getMdsNamespace,
    config,
} from '../getMdsUrl';

describe('getMdsUrl', () => {
    it('должен определять неймспейс из ссылки', () => {
        const urlToNamespace = {
            '//avatars.mds.yandex.net/get-marketpic/331398/img_id316797945473503285.jpeg': config.Marketpic,
            '//avatars.mds.yandex.net/get-yabs_performance/331398/img_id316797945473503285.jpeg': config.YabsPerformance,
            '//avatars.mds.yandex.net/get-mrkt_idx_direct/331398/img_id316797945473503285.jpeg': config.MrktIdxDirect,
            '//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg': config.GoodsPic,
            '//avatars.mds.yandex.net/i?id=ea0fa289d7b536e64713a5e77f02dd11-5282186-images-thumbs': config.Images,
            '//im0-tub-ru.yandex.net/i?id=0d3e749bf667aa38f433cb56900e8d9f-l': config.ImagesBigThumbs,
            '//avatars.mds.yandex.net/get-mpic/331398/img_id316797945473503285.jpeg': config.Mpic,
            '//avatars.mds.yandex.net/get-marketpictesting/331398/img_id316797945473503285.jpeg': config.Marketpictesting,
            '//avatars.mds.yandex.net/get-mrkt_idx_direct_test/331398/img_id316797945473503285.jpeg': config.MrktIdxDirectTest,
            '//avatars.mds.yandex.net/get-foo/331398/img_id316797945473503285.jpeg': undefined,
        };
        for (let [url, namespace] of Object.entries(urlToNamespace)) {
            expect(getMdsNamespace(url)).toEqual(namespace);
        }
    });

    it('должен вернуть исходный адрес для несуществующего неймспейса', () => {
        expect(getMdsUrl('//avatars.mds.yandex.net/get-foo/331398/img_id316797945473503285.jpeg', 'm'))
            .toEqual({
                x1: '//avatars.mds.yandex.net/get-foo/331398/img_id316797945473503285.jpeg',
                x2: '//avatars.mds.yandex.net/get-foo/331398/img_id316797945473503285.jpeg',
            });
    });

    it('должен вернуть исходный адрес при отсутствии в нём алиаса', () => {
        expect(getMdsUrl('//avatars.mds.yandex.net/get-yabs_performance/331398/img_id316797945473503285.jpeg', 'm'))
            .toEqual({
                x1: '//avatars.mds.yandex.net/get-yabs_performance/331398/img_id316797945473503285.jpeg',
                x2: '//avatars.mds.yandex.net/get-yabs_performance/331398/img_id316797945473503285.jpeg',
            });
    });

    it('должен заменять orig на алиас', () => {
        expect(getMdsUrl('//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg/orig', 'm')).toEqual({
            x1: '//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg/350x350',
            x2: '//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg/750x750',
        });
    });

    it('должен заменять 200x200 на алиас', () => {
        expect(getMdsUrl('//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg/200x200', 'm')).toEqual({
            x1: '//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg/350x350',
            x2: '//avatars.mds.yandex.net/get-goods_pic/331398/img_id316797945473503285.jpeg/750x750',
        });
    });

    it('должен добавлять параметры в адрес', () => {
        expect(getMdsUrl('//im0-tub-ru.yandex.net/i?id=0d3e749bf667aa38f433cb56900e8d9f-l', 'm')).toEqual({
            x1: '//im0-tub-ru.yandex.net/i?id=0d3e749bf667aa38f433cb56900e8d9f-l&n=33&w=600&h=600',
            x2: '//im0-tub-ru.yandex.net/i?id=0d3e749bf667aa38f433cb56900e8d9f-l&n=33&w=600&h=600',
        });
    });
});
