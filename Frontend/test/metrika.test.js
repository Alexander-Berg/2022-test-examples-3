const metrika = require('../services/metrika');

it('Metrika full domains', () => {
    const data = metrika();

    const domains = [
        'mc.yandex.az',
        'mc.yandex.by',
        'mc.yandex.co.il',
        'mc.yandex.com',
        'mc.yandex.com.am',
        'mc.yandex.com.ge',
        'mc.yandex.com.tr',
        'mc.yandex.ee',
        'mc.yandex.fr',
        'mc.yandex.kg',
        'mc.yandex.kz',
        'mc.yandex.lt',
        'mc.yandex.lv',
        'mc.yandex.md',
        'mc.yandex.ru',
        'mc.yandex.tj',
        'mc.yandex.tm',
        'mc.yandex.ua',
        'mc.yandex.uz',
    ];

    domains.forEach(domain => {
        expect(data['script-src'].includes(domain)).toBeTruthy();
        expect(data['img-src'].includes(domain)).toBeTruthy();
        expect(data['connect-src'].includes(domain)).toBeTruthy();
    });
});

it('Metrika custom domains', () => {
    const data = metrika(['it']);

    const domains = [
        'mc.yandex.it',
    ];

    domains.forEach(domain => {
        expect(data['script-src'].includes(domain)).toBeTruthy();
        expect(data['img-src'].includes(domain)).toBeTruthy();
        expect(data['connect-src'].includes(domain)).toBeTruthy();
    });
});

it('admetrika', () => {
    const data = metrika();
    const admetrika = 'mc.admetrica.ru';

    expect(data['img-src'].includes(admetrika)).toBeTruthy();
    expect(data['connect-src'].includes(admetrika)).toBeTruthy();
});

it('Metrika iframe rules', () => {
    const data = metrika();

    expect(data['frame-ancestors']).toStrictEqual([
        'webvisor.com',
        '*.webvisor.com',
        // Карты кликов и другие карты работают из http почему то :(
        'http://webvisor.com',
        'http://*.webvisor.com',
    ]);
});
