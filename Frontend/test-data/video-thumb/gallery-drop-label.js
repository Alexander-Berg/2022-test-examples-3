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
                    dropLabel: true,
                },
            ],
        },
    ],
});
