var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            template: 'composite',
            type: 'test',
            preventLegacy: true,
            dividers: 'both',
            items: [
                {
                    block: 'object-badge',
                    title: 'Аквапарк Ква-Ква парк',
                    type: 'organization',
                    subtitle: 'Аквапарк'
                },
                {
                    block: 'object-badge',
                    type: 'organization',
                    map: {
                        block: 'map2',
                        type: 'static',
                        zoom: 14,
                        url: stubs.imageUrlStub(320, 130),
                        center: ['30.404445', '59.959933'],
                        width: 320,
                        height: 130
                    },
                    thumb: {
                        image: stubs.imageStub(195, 130),
                        width: 195,
                        height: 130
                    }
                },
                {
                    block: 'key-value',
                    items: [{
                        title: 'Адрес',
                        text: 'Санкт-Петербург, Невский просп., 49/2'
                    }, {
                        title: 'Телефон',
                        text: '+7 812 322‑50-02'
                    }, {
                        title: 'Сайт',
                        text: 'radissonblu.ru'
                    }, {
                        title: 'Открыто',
                        text: 'ежедневно, круглосуточно'
                    }]
                },
                {
                    block: 'button-api',
                    text: 'Показать больше',
                    size: 'l',
                    width: 'max'
                }
            ]
        }
    }
};
