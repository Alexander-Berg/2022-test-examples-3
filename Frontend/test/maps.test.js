const maps = require('../services/maps');

it('maps scripts', () => {
    const data = maps();

    expect(data['script-src'].length).toEqual(6);
    expect(data['script-src'].includes('\'unsafe-eval\'')).toBeTruthy();
    expect(data['script-src'].includes('api-maps.yandex.ru')).toBeTruthy();
    expect(data['script-src'].includes('suggest-maps.yandex.ru')).toBeTruthy();
    expect(data['script-src'].includes('*.maps.yandex.net')).toBeTruthy();
    expect(data['script-src'].includes('yandex.ru')).toBeTruthy();
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();
});

it('maps styles', () => {
    const data = maps();

    expect(data['style-src'].length).toEqual(1);
    expect(data['style-src'].includes('blob:')).toBeTruthy();
});

it('maps images', () => {
    const data = maps();

    expect(data['img-src'].length).toEqual(4);
    expect(data['img-src'].includes('data:')).toBeTruthy();
    expect(data['img-src'].includes('*.maps.yandex.net')).toBeTruthy();
    expect(data['img-src'].includes('api-maps.yandex.ru')).toBeTruthy();
    expect(data['img-src'].includes('yandex.ru')).toBeTruthy();
});

it('maps frames', () => {
    const data = maps();

    expect(data['child-src'].length).toEqual(1);
    expect(data['child-src'].includes('api-maps.yandex.ru')).toBeTruthy();

    expect(data['frame-src'].length).toEqual(1);
    expect(data['frame-src'].includes('api-maps.yandex.ru')).toBeTruthy();
});
