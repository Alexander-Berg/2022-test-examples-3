const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'promo',
    title: 'Эфир на Яндексе',
    description: 'Смотрите любимые программы онлайн',
    action: {
        url: '#',
        text: 'Смотреть',
    },
});
