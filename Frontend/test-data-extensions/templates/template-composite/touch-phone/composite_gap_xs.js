var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            template: 'composite',
            type: 'test',
            preventLegacy: true,
            gap: 'xs',
            items: [
                {
                    block: 'object-badge',
                    title: 'Аквапарк Ква-Ква парк',
                    type: 'organization',
                    subtitle: 'Аквапарк'
                },
                {
                    block: 'map2',
                    type: 'static',
                    controls: false,
                    url: stubs.imageUrlStub(320, 120),
                    height: 120,
                    width: 320,
                    center: ['30.404445', '59.959933'],
                    zoom: 14
                },
                {
                    content: {
                        template: 'fact',
                        fact: {
                            description: 'Я serp-item как дочерний элемент template-composite__item, и у меня ' +
                            'снизу остаётся стандартный отступ serp-item\'a. Пока что меня считают нормальным'
                        }
                    }
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
