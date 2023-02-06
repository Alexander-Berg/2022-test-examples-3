const data = require('../../tools/data');

module.exports = data.createPage({
    content: [{
        block: 'menu',
        col: 24,
        items: [
            {
                text: 'Сегодня',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: 'Завтра',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: 'Выходные',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: '3 дня',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: 'Неделя',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: '10 дней',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: '2 недели',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: '10 дней',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: '2 недели',
                url: 'https://www.gismeteo.ru/',
            },
            {
                text: 'Последний пункт',
                url: 'https://www.gismeteo.ru/',
            },
        ],
    }],
    platform: 'desktop',
});
