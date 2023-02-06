var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    request_text: 'котики видео',
    type: 'wizard',
    data_stub: [{
        type: 'video',
        wizplace: '4',
        count: '16',
        urls: {
            host: 'yandex.ru',
            paths: {
                main: '/video/'
            }
        },
        counter_prefix: '/wiz/video/',
        types: {
            kind: 'wizard'
        },
        clips: [
            {
                clip_href: 'http://www.youtube.com/watch?v=PuhOAAgXtAM',
                dur: '394',
                green_host: 'youtube.com',
                raw_title: 'Смешные \u0007[Котики\u0007]!',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: 'bf390c', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=PuhOAAgXtAM',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=h9RYjl4NyWc',
                dur: '1234',
                green_host: 'youtube.com',
                raw_title: 'Funny Cats, Смешные \u0007[котики\u0007]',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: '30caaa', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=h9RYjl4NyWc',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=LbxTRMKPNH4',
                dur: '171',
                green_host: 'youtube.com',
                raw_title: 'смешные \u0007[котики\u0007]',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: 'bf390c', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=LbxTRMKPNH4',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=N7sGh4KKDg8',
                dur: '80',
                green_host: 'youtube.com',
                raw_title: '\u0007[Котик\u0007] пытается поужинать',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: '30aaca', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                title: 'Котик пытается поужинать',
                views: '955042'
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=PuhOAAgXtAM',
                dur: '394',
                green_host: 'youtube.com',
                raw_title: 'Смешные \u0007[Котики\u0007]!',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: 'bf390c', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=PuhOAAgXtAM',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=h9RYjl4NyWc',
                dur: '1234',
                green_host: 'youtube.com',
                raw_title: 'Funny Cats, Смешные \u0007[котики\u0007]',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: '30caaa', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=h9RYjl4NyWc',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=LbxTRMKPNH4',
                dur: '171',
                green_host: 'youtube.com',
                raw_title: 'смешные \u0007[котики\u0007]',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: 'bf390c', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=LbxTRMKPNH4',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=N7sGh4KKDg8',
                dur: '80',
                green_host: 'youtube.com',
                raw_title: '\u0007[Котик\u0007] пытается поужинать',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: '30aaca', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                title: 'Котик пытается поужинать',
                views: '955042'
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=PuhOAAgXtAM',
                dur: '394',
                green_host: 'youtube.com',
                raw_title: 'Смешные \u0007[Котики\u0007]!',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: 'bf390c', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=PuhOAAgXtAM',
            },
            {
                clip_href: 'http://www.youtube.com/watch?v=h9RYjl4NyWc',
                dur: '1234',
                green_host: 'youtube.com',
                raw_title: 'Funny Cats, Смешные \u0007[котики\u0007]',
                thmb_h: 90,
                thmb_href: stubs.imageUrlStub(140, 90, { color: '30caaa', patternSize: 24, format: 'png' }),
                thmb_w: 120,
                url: 'http://www.youtube.com/watch?v=h9RYjl4NyWc',
            }
        ],
    }]
};
