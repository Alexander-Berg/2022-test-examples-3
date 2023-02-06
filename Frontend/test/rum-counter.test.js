const rumCounter = require('../services/rum-counter');

it('Rum-counter domain', () => {
    const data = rumCounter();

    const domains = [
        'https://yandex.ru',
    ];

    domains.forEach(domain => {
        expect(data['connect-src'].length).toEqual(1);
        expect(data['connect-src'].includes(domain)).toBeTruthy();
    });
});
