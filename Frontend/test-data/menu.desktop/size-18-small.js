const data = require('../../tools/data');

module.exports = data.createPage({
    content: [{
        block: 'menu',
        col: 18,
        items: [
            {
                text: 'Сегодня',
                url: 'https://rg.ru/',
            },
            {
                text: 'Вчера',
                url: 'https://rg.ru/',
            },
        ],
    }],
    platform: 'desktop',
});
