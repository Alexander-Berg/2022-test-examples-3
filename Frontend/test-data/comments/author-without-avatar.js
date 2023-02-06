const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        block: 'markup',
        content: {
            block: 'comments',
            content: [
                {
                    author: {
                        title: 'Homer',
                        subtitle: '12 ноя 2017',
                        extra: {
                            type: 'text',
                            value: 'Лучший ответ',
                        },
                    },
                    text: [
                        {
                            block: 'paragraph',
                            content: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.',
                        },
                    ],
                },
            ],
        },
    },
]);
