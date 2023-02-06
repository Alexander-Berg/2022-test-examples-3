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
        content_type: 'markup',
        content: {
            content_type: 'paragraph',
            content: 'Помимо жилых домов проектом предусмотрено строительство всей необходимой инфраструктуры.',
        },
    },
    {
        content_type: 'sticky',
        position: 'bottom',
        content: {
            content_type: 'button',
            text: 'Заказать звонок',
        },
    },
]);
