const video = require('../services/videoPlatform');

it('Share to social', () => {
    const data = video();

    expect(data['media-src'].length).toEqual(2);
    expect(data['media-src'].includes('streaming.video.yandex.ru')).toBeTruthy();
    expect(data['media-src'].includes('*.storage.yandex.net')).toBeTruthy();
});
