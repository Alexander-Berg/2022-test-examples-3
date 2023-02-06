const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        block: 'markup',
        content: {
            block: 'comments',
            content: [
                {
                    author: {
                        avatar_url: 'https://avatars.mds.yandex.net/get-turbo/372683/2279943d-2482-4cd1-9c2b-8874e607a6cf',
                        title: 'Homer',
                        subtitle: '12 ноя 2017',
                        extra: {
                            type: 'image',
                            value: 'https://emojipedia-us.s3.amazonaws.com/thumbs/240/apple/114/fox-face_1f98a.png',
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
