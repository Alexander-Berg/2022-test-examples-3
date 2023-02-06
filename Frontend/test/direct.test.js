const direct = require('../services/direct');

it('direct scripts', () => {
    const data = direct();

    expect(data['script-src'].length).toEqual(7);
    expect(data['script-src'].includes('*.adfox.ru')).toBeTruthy();
    expect(data['script-src'].includes('\'unsafe-eval\'')).toBeTruthy();
    expect(data['script-src'].includes('*.yandex.ru')).toBeTruthy();
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();
    expect(data['script-src'].includes('yandex.ru')).toBeTruthy();
    expect(data['script-src'].includes('yandex.com')).toBeTruthy();
});

it('direct styles', () => {
    const data = direct();

    expect(data['style-src'].length).toEqual(4);
    expect(data['style-src'].includes('\'unsafe-inline\'')).toBeTruthy();
    expect(data['style-src'].includes('yastatic.net')).toBeTruthy();
    expect(data['style-src'].includes('*.adfox.ru')).toBeTruthy();
});

it('direct images', () => {
    const data = direct();

    expect(data['img-src'].length).toEqual(7);
    expect(data['img-src'].includes('\'self\'')).toBeTruthy();
    expect(data['img-src'].includes('data:')).toBeTruthy();
    expect(data['img-src'].includes('*.yandex.ru')).toBeTruthy();
    expect(data['img-src'].includes('*.yandex.net')).toBeTruthy();
    expect(data['img-src'].includes('yandex.ru')).toBeTruthy();
    expect(data['img-src'].includes('yandex.com')).toBeTruthy();
});

it('direct media', () => {
    const data = direct();

    expect(data['media-src'].length).toEqual(8);
    expect(data['media-src'].includes('*.yandex.net')).toBeTruthy();
    expect(data['media-src'].includes('*.yandex.ru')).toBeTruthy();
    expect(data['media-src'].includes('yastatic.net')).toBeTruthy();
});

it('direct frame', () => {
    const data = direct();

    expect(data['frame-src'].length).toEqual(5);
    expect(data['frame-src'].includes('*.yandex.ru')).toBeTruthy();
    expect(data['frame-src'].includes('yastatic.net')).toBeTruthy();
    expect(data['frame-src'].includes('yandexadexchange.net')).toBeTruthy();
    expect(data['frame-src'].includes('*.yandexadexchange.net')).toBeTruthy();
});

it('direct connect', () => {
    const data = direct();

    expect(data['connect-src'].length).toEqual(6);
    expect(data['connect-src'].includes('\'self\'')).toBeTruthy();
    expect(data['connect-src'].includes('yastatic.net')).toBeTruthy();
    expect(data['connect-src'].includes('*.yandex.ru')).toBeTruthy();
    expect(data['connect-src'].includes('*.adfox.ru')).toBeTruthy();
    expect(data['connect-src'].includes('yandex.ru')).toBeTruthy();
    expect(data['connect-src'].includes('yandex.com')).toBeTruthy();
});

it('direct font', () => {
    const data = direct();

    expect(data['font-src'].length).toEqual(3);
    expect(data['font-src'].includes('yastatic.net')).toBeTruthy();
});
