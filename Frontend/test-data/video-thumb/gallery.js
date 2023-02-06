const data = require('../../tools/data');

module.exports = data.createPage({
    content: [
        {
            content_type: 'gallery',
            items: [
                {
                    content_type: 'video-thumb',
                    title: 'Трейлер',
                    preview: {
                        width: 1130,
                        height: 480,
                        url: '//avatars.mds.yandex.net/get-kino-vod-films-gallery/175804/2a0000015f347aa01d4beb4600b298ddd2a6/orig',
                    },
                    sourceUrl: '//kp.cdn.yandex.net/662839/kinopoisk.ru-Lost-for-Words-202896.mp4',
                    duration: 132000,
                    videoResolution: {
                        width: 1280,
                        height: 544,
                    },
                },
                {
                    content_type: 'video-thumb',
                    title: 'ТВ-ролик',
                    preview: {
                        width: 3600,
                        height: 1525,
                        url: '//avatars.mds.yandex.net/get-kino-vod-films-gallery/27707/2a00000151e8043bacc5e1167d18a80b145e/orig',
                    },
                    sourceUrl: '//kp.cdn.yandex.net/692217/kinopoisk.ru-Bad-Words-208863.mp4',
                    duration: 31000,
                    videoResolution: {
                        width: 1280,
                        height: 720,
                    },
                },
                {
                    content_type: 'video-thumb',
                    title: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
                    preview: {
                        width: 1280,
                        height: 720,
                        url: '//avatars.mds.yandex.net/get-kino-vod-films-gallery/27707/2a00000151e80440178428c00af35b47ddd7/orig',
                    },
                    sourceUrl: '//kp.cdn.yandex.net/88078/kinopoisk.ru-Giver_-The-222540.mp4',
                    duration: 37000,
                    videoResolution: {
                        width: 1920,
                        height: 800,
                    },
                },
            ],
        },
    ],
});
