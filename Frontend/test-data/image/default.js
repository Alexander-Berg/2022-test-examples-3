const data = require('../../tools/data');

module.exports = data.createSnippet([
    {
        content_type: 'image',
        src: 'https://avatars.mds.yandex.net/get-ynews/49544/e49ae9b6e3e7e5e39b6a05f6fbad2cf2/600x450',
        alt: 'Mercedes-Benz',
        width: 400,
        height: 257,
        caption: [
            'В этой подписи ',
            {
                content_type: 'link',
                text: 'есть ссылка',
                url: 'https://ya.ru',
            },
        ],
    },
    {
        content_type: 'image',
        src: 'http://avatars.mdst.yandex.net/get-turbo/5003/80ac814f618aa49409a5e4190305712f/max_g360_c12_r16x9_pd10',
        alt: 'Mercedes-Benz',
        width: 139,
        height: 185,
        caption: [
            'В этой подписи ',
            {
                content_type: 'link',
                text: 'есть ссылка',
                url: 'https://ya.ru',
            },
            ' и она очень-очень длинная',
        ],
    },
    {
        block: 'title',
        content: 'Картинка со стороннего ресурса без размеров',
    },
    {
        content_type: 'image',
        width: 408,
        height: 243,
        src: 'https://yastatic.net/morda-logo/i/blocks/media-stream/18-back.jpg',
        caption: 'Blah: Reddit',
    },
    {
        block: 'title',
        content: 'Инлайновая свг картинка',
    },
    {
        content_type: 'paragraph',
        text: [
            'А вот это SVG картинка:',
            {
                content_type: 'image',
                src: '//avatars.mds.yandex.net/get-turbo/761471/2a0000015fc6c043167485a847dbfc17273e/svg',
                type: 'inline',
            },
        ],
    },
]);
