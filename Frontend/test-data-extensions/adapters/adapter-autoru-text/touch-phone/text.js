module.exports = {
    type: 'snippet',
    request_text: 'Отзывы об автомобилях',
    data_stub: {
        num: 0,
        construct: {
            baobab: {
                path: '/snippet/auto_2/common'
            },
            favicon: {
                domain: 'm.auto.ru'
            },
            path: [
                {
                    text: 'auto.ru',
                    url: 'https://m.auto.ru'
                }
            ],
            site_links: [
                {
                    text: 'С пробегом',
                    url: 'https://m.auto.ru/cars/used'
                },
                {
                    text: 'Новые',
                    url: 'https://m.auto.ru/cars/new'
                },
                {
                    text: 'Коммерческие',
                    url: 'https://m.auto.ru/commercial'
                }
            ],
            subtype: 'thumbs-text',
            text: 'Большая база объявлений о продаже новых и подержанных авто. ' +
                'Полная информация об автомобилях — фотографии, отзывы, характеристики, цены.',
            title: {
                text: '\u0007[Продажа автомобилей\u0007] — 87 465 объявлений в Москве',
                url: 'https://m.auto.ru/cars/all'
            },
            type: 'autoru/thumbs-text'
        }
    }
};
