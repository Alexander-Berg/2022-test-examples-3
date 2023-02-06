const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        content_type: 'cover',
        content: {
            content_type: 'header',
            type: 'host',
            content: {
                content_type: 'header-title',
                text: 'header_host',
            },
        },
    },
    {
        content_type: 'actions',
        content: {
            content_type: 'button',
            text: 'Посетить сайт',
        },
    },
]);
