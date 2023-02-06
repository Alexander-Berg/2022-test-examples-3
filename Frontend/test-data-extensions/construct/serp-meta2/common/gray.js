module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'test',
            preventLegacy: true,
            godMode: true,
            blocks: [{
                block: 'organic',
                url: 'http://ya.ru',
                title: 'Сниппет с серой метой',
                meta: {
                    type: 'gray',
                    items: [
                        {
                            link: {
                                block: 'link',
                                text: 'Положительные отзывы',
                                url: 'http://ya.ru'
                            },
                            items: [
                                // jscs:disable maximumLineLength
                                // тут по задумке строка должна быть длинная
                                'просп. Космонавтов, 21/1, и еще какой-то текст, просп. Космонавтов, 21/1, sdfsdf. sdfsdfsd и еще какой-то текст просп. Космонавтов, 21/1',
                                // jscs:enable maximumLineLength
                                'размер большой',
                                'размер большой',
                                'размер большой'
                            ]
                        },
                        {
                            wrap: true,
                            items: [
                                // jscs:disable maximumLineLength
                                // тут по задумке строка должна быть длинная
                                'просп. Космонавтов, 21/1, и еще какой-то текст, просп. Космонавтов, 21/1, sdfsdf. sdfsdfsd и еще какой-то текст просп. Космонавтов, 21/1',
                                // jscs:enable maximumLineLength
                                'и еще какой-то текст'
                            ]
                        },
                        {
                            type: 'map',
                            wrap: true,
                            link: {
                                block: 'link',
                                // jscs:disable maximumLineLength
                                // тут по задумке строка должна быть длинная
                                text: 'просп. Космонавтов, 21/1, и еще какой-то текст, просп. Космонавтов, 21/1, sdfsdf. sdfsdfsd и еще какой-то текст просп. Космонавтов, 21/1',
                                // jscs:enable maximumLineLength
                                url: 'http://ya.ru'
                            },
                            items: [
                                'цвет розовый',
                                'размер большой'
                            ]
                        },
                        {
                            type: 'map',
                            items: [
                                'цвет розовый',
                                'размер большой'
                            ]
                        }
                    ]
                }
            }]
        }
    }
};
