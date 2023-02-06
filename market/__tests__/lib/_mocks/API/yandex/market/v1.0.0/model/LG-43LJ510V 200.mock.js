/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/model\/1724547969\.json/;

const query = {};

const result = {
    comment: 'model/1724547969',
    status: 200,
    body: {
        model: {
            id: 1724547969,
            name: 'LG 43LJ510V',
            kind: 'телевизор',
            description: 'ЖК-телевизор, LED, 43\', 1920x1080, 1080p Full HD, мощность звука 10 ВтHDMI x2',
            categoryId: 90639,
            category: {
                id: 90639,
                type: 'GURU',
                advertisingModel: 'CPA',
                name: 'Телевизоры'
            },
            prices: {
                max: '26014',
                min: '20450',
                avg: '23849',
                curCode: 'RUR',
                curName: 'руб.'
            },
            mainPhoto: {
                url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id1791511517718709854/orig',
                width: 701,
                height: 466
            },
            previewPhoto: {
                url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id1791511517718709854/4hq',
                width: 150,
                height: 150
            },
            link: 'https://market.yandex.ru/product/1724547969?hid=90639&pp=1002&clid=2270459&distr_type=4',
            vendorId: 153074,
            vendor: 'LG',
            isGroup: false,
            reviewsCount: 5,
            rating: 3.5,
            offersCount: 67,
            gradeCount: 6,
            articlesCount: 0,
            isNew: 0,
            photos: {
                photo: [
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id1791511517718709854/orig',
                        width: 701,
                        height: 466
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id7214123425790398175/orig',
                        width: 579,
                        height: 572
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id7155449216677351796/orig',
                        width: 701,
                        height: 476
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6555740134591202648/orig',
                        width: 701,
                        height: 478
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id3346403059611136138/orig',
                        width: 568,
                        height: 562
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id5645974959396324972/orig',
                        width: 689,
                        height: 562
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id478499130270591809/orig',
                        width: 192,
                        height: 542
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id4247098865274409299/orig',
                        width: 701,
                        height: 143
                    },
                    {
                        url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id3466628210327205981/orig',
                        width: 613,
                        height: 558
                    }
                ]
            },
            previewPhotos: [
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id1791511517718709854/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id7214123425790398175/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id7155449216677351796/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6555740134591202648/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id3346403059611136138/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id5645974959396324972/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/364668/img_id478499130270591809/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/199079/img_id4247098865274409299/4hq',
                    width: 150,
                    height: 150
                },
                {
                    url: 'https://avatars.mds.yandex.net/get-mpic/200316/img_id3466628210327205981/4hq',
                    width: 150,
                    height: 150
                }
            ]
        }
    }
};

module.exports = new ApiMock(host, pathname, query, result);
