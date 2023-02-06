const adsdk = require('../services/adsdk');

it('adsdk pixels', () => {
    const data = adsdk();

    const domains = [
        '*.tns-counter.ru',
        '*.verify.yandex.ru',
        'ads.adfox.ru',
        'bs.serving-sys.com',
        'bs.serving-sys.ru',
        'pixel.adlooxtracking.com',
        'pixel.adlooxtracking.ru',
    ];

    expect(data['connect-src'].length).toEqual(20);
    expect(data['frame-src'].length).toEqual(1);
    expect(data['img-src'].length).toEqual(31);
    expect(data['media-src'].length).toEqual(6);
    expect(data['script-src'].length).toEqual(3);
    expect(data['style-src'].length).toEqual(1);

    domains.forEach(domain => {
        expect(data['img-src'].includes(domain)).toBeTruthy();
    });
});
