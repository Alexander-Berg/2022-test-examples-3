const player = require('../services/player');

it('player csp rules', () => {
    const preset = {
        'worker-src': [
            'blob:',
        ],
        'connect-src': [
            '*.strm.yandex.net',
            'widevine-proxy.ott.yandex.ru',
            'fairplay-proxy.ott.yandex.ru',
            'playready-proxy.ott.yandex.ru',
            'drm.yandex-team.ru',
        ],
        'media-src': [
            'blob:',
            'strm.yandex.ru',
            '*.strm.yandex.net',
        ],
        'img-src': [
            'strm.yandex.net',
        ],
    };
    const data = player();

    Object.keys(preset).forEach(key => {
        preset[key].forEach(domain => {
            expect(data[key].includes(domain)).toBeTruthy();
        });
    });

    expect(data['worker-src'].length).toEqual(1);
    expect(data['child-src'].length).toEqual(1);
    expect(data['script-src'].length).toEqual(5);
    expect(data['connect-src'].length).toEqual(29);
    expect(data['style-src'].length).toEqual(2);
    expect(data['media-src'].length).toEqual(12);
    expect(data['font-src'].length).toEqual(1);
    expect(data['img-src'].length).toEqual(36);
});
