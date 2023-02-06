const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        content_type: 'title',
        size: 'l',
        text: 'Спасибо за заявку!',
    },
    {
        content_type: 'markup',
        content: [
            {
                content_type: 'paragraph',
                text: 'Мы скоро свяжемся с вами, чтобы выбрать подходящее время для мытья окон.',
            },
        ],
    },
    {
        content_type: 'actions',
        content: {
            content_type: 'button',
            text: 'Посетить сайт',
        },
    },
]);
