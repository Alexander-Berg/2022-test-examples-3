const butterfly = require('../services/butterfly');
const consts = require('../utils/consts');

it('butterfly frames', () => {
    const data = butterfly();

    expect(data['child-src'].length).toEqual(2);
    expect(data['child-src'].includes('forms.yandex.ru')).toBeTruthy();
    expect(data['child-src'].includes('forms.yandex-team.ru')).toBeTruthy();

    expect(data['frame-src'].length).toEqual(2);
    expect(data['frame-src'].includes('forms.yandex.ru')).toBeTruthy();
    expect(data['child-src'].includes('forms.yandex-team.ru')).toBeTruthy();
});

it('butterfly connect: full domains', () => {
    const data = butterfly();

    const domains = ['ru', consts.TLD].map(tld => `yandex.${tld}`);

    expect(data['connect-src'].length).toEqual(4);
    domains.forEach(domain => {
        expect(data['connect-src'].includes(domain)).toBeTruthy();
    });
    expect(data['connect-src'].includes('ya.ru')).toBeTruthy();
    expect(data['connect-src'].includes('ecoo.n.yandex-team.ru')).toBeTruthy();
});

it('butterfly connect: custom domains', () => {
    const data = butterfly(['exp.it']);

    expect(data['connect-src'].length).toEqual(3);
    expect(data['connect-src'].includes('exp.it')).toBeTruthy();
});
