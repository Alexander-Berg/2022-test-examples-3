const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        content_type: 'form',
        content: [
            {
                content_type: 'cover',
                content: [
                    {
                        content_type: 'header',
                        type: 'host',
                        content: {
                            content_type: 'header-title',
                            text: 'header_host',
                        },
                    },
                    {
                        content_type: 'divider',
                    },
                ],
            },
            {
                content_type: 'fieldset',
                content: {
                    content_type: 'form-line',
                    content: {
                        content_type: 'input',
                        label: 'Как вас зовут',
                    },
                },
            },
        ],
    },
]);
