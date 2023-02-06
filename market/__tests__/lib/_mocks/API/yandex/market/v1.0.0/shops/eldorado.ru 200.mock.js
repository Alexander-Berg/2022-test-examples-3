/* eslint-disable max-len */

'use strict';

const ApiMock = require('./../../../../../../ApiMock');

const host = 'https://api.content.market.yandex.ru';

const pathname = /v1\/shops\.json/;

const query = {
    host: 'eldorado.ru'
};

const result = {
    comment: 'host = "eldorado.ru"',
    status: 200,
    body: {
        time: 1514384804601,
        metadata: {
            filters: {
                geoId: 0,
                host: 'eldorado.ru'
            }
        },
        shops: [
            {
                id: 258283,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 76,
                createdAt: '2014-10-29'
            },
            {
                id: 145985,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 50,
                createdAt: '2013-03-05'
            },
            {
                id: 174503,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 195,
                createdAt: '2013-08-16'
            },
            {
                id: 397505,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 25,
                createdAt: '2017-01-13'
            },
            {
                id: 310140,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 973,
                createdAt: '2015-09-15'
            },
            {
                id: 156493,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 35,
                createdAt: '2013-04-30'
            },
            {
                id: 17493,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 2,
                createdAt: '2008-11-13'
            },
            {
                id: 397053,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 37,
                createdAt: '2017-01-11'
            },
            {
                id: 296374,
                name: '«Эльдорадо»',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 6,
                createdAt: '2015-06-09'
            },
            {
                id: 111387,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 65,
                createdAt: '2012-07-13'
            },
            {
                id: 397509,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 126436,
                regionId: 239,
                createdAt: '2017-01-13'
            },
            {
                id: 284077,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 49,
                createdAt: '2015-03-26'
            },
            {
                id: 120350,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 56,
                createdAt: '2012-09-26'
            },
            {
                id: 192472,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 75,
                createdAt: '2013-11-19'
            },
            {
                id: 431828,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10740,
                createdAt: '2017-08-14'
            },
            {
                id: 397233,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 30,
                createdAt: '2017-01-12'
            },
            {
                id: 258150,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 46,
                createdAt: '2014-10-28'
            },
            {
                id: 397506,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10849,
                createdAt: '2017-01-13'
            },
            {
                id: 112422,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 39,
                createdAt: '2012-07-24'
            },
            {
                id: 117318,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 51,
                createdAt: '2012-09-07'
            },
            {
                id: 165579,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 126436,
                regionId: 194,
                createdAt: '2013-06-26'
            },
            {
                id: 431834,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 20728,
                createdAt: '2017-08-14'
            },
            {
                id: 154433,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 193,
                createdAt: '2013-04-18'
            },
            {
                id: 149780,
                name: '\'Эльдорадо\'',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 236,
                createdAt: '2013-03-26'
            },
            {
                id: 397525,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 74,
                createdAt: '2017-01-13'
            },
            {
                id: 397523,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 968,
                createdAt: '2017-01-13'
            },
            {
                id: 119236,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 54,
                createdAt: '2012-09-18'
            },
            {
                id: 284125,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 11311,
                createdAt: '2015-03-26'
            },
            {
                id: 296376,
                name: '«Эльдорадо»',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 8,
                createdAt: '2015-06-09'
            },
            {
                id: 216743,
                name: 'ЭЛЬДОРАДО',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 16,
                createdAt: '2014-03-20'
            },
            {
                id: 169247,
                name: 'ЭЛЬДОРАДО',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 15,
                createdAt: '2013-07-17'
            },
            {
                id: 258260,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 53,
                createdAt: '2014-10-29'
            },
            {
                id: 397512,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 13,
                createdAt: '2017-01-13'
            },
            {
                id: 431904,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10752,
                createdAt: '2017-08-15'
            },
            {
                id: 129755,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 240,
                createdAt: '2012-11-22'
            },
            {
                id: 232465,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 9,
                createdAt: '2014-06-03'
            },
            {
                id: 397500,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 970,
                createdAt: '2017-01-13'
            },
            {
                id: 397514,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 14,
                createdAt: '2017-01-13'
            },
            {
                id: 397098,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 126436,
                regionId: 21,
                createdAt: '2017-01-11'
            },
            {
                id: 431831,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10738,
                createdAt: '2017-08-14'
            },
            {
                id: 258140,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 192,
                createdAt: '2014-10-28'
            },
            {
                id: 431896,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 21622,
                createdAt: '2017-08-15'
            },
            {
                id: 397088,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 24,
                createdAt: '2017-01-11'
            },
            {
                id: 143409,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 66,
                createdAt: '2013-02-19'
            },
            {
                id: 123491,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 55,
                createdAt: '2012-10-17'
            },
            {
                id: 114202,
                name: '\'ЭЛЬДОРАДО\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 38,
                createdAt: '2012-08-08'
            },
            {
                id: 115706,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 47,
                createdAt: '2012-08-22'
            },
            {
                id: 296339,
                name: '«Эльдорадо»',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 36,
                createdAt: '2015-06-09'
            },
            {
                id: 431906,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10745,
                createdAt: '2017-08-15'
            },
            {
                id: 431902,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10747,
                createdAt: '2017-08-15'
            },
            {
                id: 397128,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 7,
                createdAt: '2017-01-11'
            },
            {
                id: 296361,
                name: '«Эльдорадо»',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 4,
                createdAt: '2015-06-09'
            },
            {
                id: 431818,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10743,
                createdAt: '2017-08-14'
            },
            {
                id: 161597,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 62,
                createdAt: '2013-06-02'
            },
            {
                id: 225477,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 23,
                createdAt: '2014-04-25'
            },
            {
                id: 431910,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10733,
                createdAt: '2017-08-15'
            },
            {
                id: 174505,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 197,
                createdAt: '2013-08-16'
            },
            {
                id: 139872,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 64,
                createdAt: '2013-01-31'
            },
            {
                id: 397031,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 126436,
                regionId: 20,
                createdAt: '2017-01-11'
            },
            {
                id: 130669,
                name: '\'Эльдорадо\'',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 63,
                createdAt: '2012-11-27'
            },
            {
                id: 139995,
                name: 'Эльдорадо',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 10,
                createdAt: '2013-01-31'
            },
            {
                id: 169243,
                name: 'ЭЛЬДОРАДО',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 11,
                createdAt: '2013-07-17'
            },
            {
                id: 111836,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 43,
                createdAt: '2012-07-18'
            },
            {
                id: 337060,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'oldshop',
                rating: -1,
                gradeTotal: 126436,
                regionId: 11139,
                createdAt: '2016-02-02'
            },
            {
                id: 284123,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 80,
                createdAt: '2015-03-26'
            },
            {
                id: 114871,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 172,
                createdAt: '2012-08-14'
            },
            {
                id: 296363,
                name: '«Эльдорадо»',
                shopName: 'Эльдорадо',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 44,
                createdAt: '2015-06-09'
            },
            {
                id: 397115,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 5,
                createdAt: '2017-01-11'
            },
            {
                id: 256021,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 12,
                createdAt: '2014-10-17'
            },
            {
                id: 397516,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 45,
                createdAt: '2017-01-13'
            },
            {
                id: 284120,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 78,
                createdAt: '2015-03-26'
            },
            {
                id: 397240,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 237,
                createdAt: '2017-01-12'
            },
            {
                id: 235023,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 67,
                createdAt: '2014-06-17'
            },
            {
                id: 237642,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 42,
                createdAt: '2014-07-01'
            },
            {
                id: 256050,
                name: 'Эльдорадо',
                shopName: 'Эльдорадо',
                url: 'www.eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 191,
                createdAt: '2014-10-17'
            },
            {
                id: 256060,
                name: '«Эльдорадо»',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 1106,
                createdAt: '2014-10-17'
            },
            {
                id: 3678,
                name: '\'Эльдорадо\'',
                shopName: 'ЭЛЬДОРАДО',
                url: 'eldorado.ru',
                status: 'actual',
                rating: 5,
                gradeTotal: 126436,
                regionId: 213,
                createdAt: '2006-10-06'
            }
        ]
    }
};

module.exports = new ApiMock(host, pathname, query, result);
