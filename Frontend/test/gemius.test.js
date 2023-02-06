const gemius = require('../services/gemius');

it('gemius full domains', () => {
    const data = gemius();

    const domains = [
        'yandexgaby.hit.gemius.pl',
        'yandexgaua.hit.gemius.pl',
    ];

    domains.forEach(domain => {
        expect(data['img-src'].includes(domain)).toBeTruthy();
    });
});

it('gemius custom domains', () => {
    const data = gemius(['it']);

    const domains = ['yandexgait.hit.gemius.pl'];

    domains.forEach(domain => {
        expect(data['img-src'].includes(domain)).toBeTruthy();
    });
});
