const browserParanja = require('../services/browserParanja');

it('paranja prod', () => {
    const data = browserParanja();

    expect(data['script-src'].length).toEqual(1);
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();

    expect(data['frame-src'].length).toEqual(1);
    expect(data['frame-src'].includes('download-paranja.yandex.net')).toBeTruthy();

    expect(data['child-src'].length).toEqual(1);
    expect(data['child-src'].includes('download-paranja.yandex.net')).toBeTruthy();
});

it('paranja testing', () => {
    const data = browserParanja('test');

    expect(data['script-src'].length).toEqual(1);
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();

    expect(data['frame-src'].length).toEqual(1);
    expect(data['frame-src'].includes('download-paranja.brpages-test.yandex.net')).toBeTruthy();

    expect(data['child-src'].length).toEqual(1);
    expect(data['child-src'].includes('download-paranja.brpages-test.yandex.net')).toBeTruthy();
});
