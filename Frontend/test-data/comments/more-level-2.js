const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        block: 'markup',
        content: {
            block: 'comments',
            content: [
                {
                    more: 13,
                    author: {
                        avatar_url: 'https://avatars.mds.yandex.net/get-turbo/372683/2279943d-2482-4cd1-9c2b-8874e607a6cf',
                        title: 'Homer',
                        subtitle: '12 ноя 2017',
                    },
                    text: [
                        {
                            block: 'paragraph',
                            content: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.',
                        },
                    ],
                },
                {
                    author: {
                        avatar_url: 'https://avatars.mds.yandex.net/get-turbo/372683/2279943d-2482-4cd1-9c2b-8874e607a6cf',
                        title: 'Moe',
                        subtitle: '12 ноя 2017',
                    },
                    indent: 1,
                    reply_to: {
                        author: {
                            avatar_url: 'https://avatars.mds.yandex.net/get-turbo/372683/2279943d-2482-4cd1-9c2b-8874e607a6cf',
                            title: 'Homer',
                            subtitle: '12 ноя 2017',
                        },
                        text: [
                            {
                                block: 'paragraph',
                                content: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.',
                            },
                        ],
                    },
                    text: [
                        {
                            block: 'paragraph',
                            content: 'Phasellus interdum feugiat dui id facilisis.',
                        },
                    ],
                },
            ],
        },
    },
]);
